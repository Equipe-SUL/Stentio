import axios from 'axios';
import { Platform } from 'react-native';
import { getToken } from './auth';

// --- Tipos -------------------------------------------------------------------

/**
 * Origem remota da logo para o <Image> do expo-image: ele aceita headers, o
 * que resolve a autenticação sem passar por blob/object URL.
 */
export interface LogoSource {
  uri: string;
  /**
   * Opcional de propósito: quando a sessão está no cookie jar nativo (login sem
   * "lembrar sessão", ou app reaberto) não existe token em storage, e a
   * requisição é autenticada pelo cookie que o client já tem. Exigir o header
   * aqui fazia a prévia falhar mesmo com a empresa e a logo existindo.
   */
  headers?: Record<string, string>;
}

export interface EmpresaResponse {
  nome: string;
  cnpj: string;
  endereco: string;
  telefone: string | null;
  logoDisponivel: boolean;
}

export interface EmpresaRequest {
  nome: string;
  cnpj: string;
  endereco: string;
  telefone: string;
}

// --- Cliente -----------------------------------------------------------------

const DEFAULT_EMPRESA_API_URL =
  Platform.OS === 'android' ? 'http://10.0.2.2:8083' : 'http://localhost:8083';

function resolveApiUrl(): string {
  const configured = process.env.EXPO_PUBLIC_EMAIL_API_URL ?? DEFAULT_EMPRESA_API_URL;

  // No web o cookie de sessão é SameSite=Lax: página e API precisam estar no
  // mesmo host. Alinha a URL da API ao host atual da página, mantendo a porta.
  if (Platform.OS === 'web' && typeof window !== 'undefined') {
    try {
      const url = new URL(configured);
      if (url.hostname !== window.location.hostname) {
        url.hostname = window.location.hostname;
        return url.toString().replace(/\/$/, '');
      }
    } catch {
      // URL inválida: usa como configurada.
    }
  }

  return configured;
}

export const EMPRESA_API_URL = resolveApiUrl();

const api = axios.create({
  baseURL: EMPRESA_API_URL,
  timeout: 15000,
  withCredentials: true,
});

api.interceptors.request.use(async (config) => {
  const token = await getToken();
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

// --- Funções -----------------------------------------------------------------

/**
 * Busca os dados atuais da empresa.
 * Retorna null se a empresa ainda não foi cadastrada (404).
 */
export async function buscarEmpresa(): Promise<EmpresaResponse | null> {
  try {
    const { data } = await api.get<EmpresaResponse>('/api/v1/empresa');
    return data;
  } catch (error: any) {
    const status = error?.response?.status;
    if (status === 404) {
      return null;
    }
    throw error;
  }
}

/**
 * Cria ou atualiza os dados da empresa (PUT idempotente no backend).
 */
export async function salvarEmpresa(request: EmpresaRequest): Promise<EmpresaResponse> {
  const { data } = await api.put<EmpresaResponse>('/api/v1/empresa', request);
  return data;
}

/**
 * Atualiza apenas os campos enviados (PATCH parcial).
 */
export async function atualizarEmpresa(
  request: Partial<EmpresaRequest>
): Promise<EmpresaResponse> {
  const { data } = await api.patch<EmpresaResponse>('/api/v1/empresa', request);
  return data;
}

/**
 * Faz upload da logo da empresa.
 * @param uri  URI local do arquivo (resultado do document picker).
 * @param name Nome original do arquivo.
 * @param type MIME type do arquivo (image/png, image/jpeg, image/webp).
 */
export async function salvarLogo(
  uri: string,
  name: string,
  type: string
): Promise<EmpresaResponse> {
  const formData = new FormData();

  // No native o RN converte { uri, name, type } em parte de arquivo. No web o
  // FormData é o do browser e um objeto simples viraria a string "[object Object]",
  // então é preciso buscar o blob a partir da uri antes de anexar.
  if (Platform.OS === 'web') {
    // O terceiro argumento preserva o nome original: sem ele o browser nomeia
    // a parte como "blob" e o backend perde a extensão ao validar o formato.
    formData.append('arquivo', await arquivoComoBlob(uri, type), name);
  } else {
    formData.append('arquivo', { uri, name, type } as any);
  }

  const { data } = await api.put<EmpresaResponse>('/api/v1/empresa/logo', formData);
  return data;
}

async function arquivoComoBlob(uri: string, type: string): Promise<Blob> {
  const resposta = await fetch(uri);
  return new Blob([await resposta.arrayBuffer()], { type });
}

/**
 * Retorna a URL crua da rota da logo.
 * Só serve se a requisição puder autenticada sozinha (cookie no web) — no
 * nativo não há cookie, então use carregarLogo().
 */
export function getLogoUrl(): string {
  return `${EMPRESA_API_URL}/api/v1/empresa/logo`;
}

/**
 * Retorna a origem da logo para o <Image> do expo-image.
 *
 * No nativo não dá para usar blob nem object URL: URL.createObjectURL é API de
 * browser e não existe no React Native, e o responseType 'blob' do axios segue
 * a mesma especificação. O expo-image aceita headers no source, então ele mesmo
 * faz a requisição — sem download, sem memória retida e sem passar pela
 * validação de tamanho do container.
 *
 * Sem token, segue com a URL pura: o Android tem cookie jar nativo e a sessão
 * pode estar nele (é o caso quando o login foi feito sem "lembrar sessão", ou
 * depois de um reload, em que o SecureStore não tem o token). Exigir o Bearer
 * aqui transformava "sessão via cookie" em "logo não existe".
 */
export async function getLogoSource(versao?: number): Promise<LogoSource> {
  const token = await getToken();

  // A query de versão existe por causa de dois caches independentes:
  //   1. o do expo-image, em memória, chaveado pela URL e que ignora o
  //      Cache-Control do servidor;
  //   2. o HTTP do navegador, que só é neutralizado pelo no-store do backend.
  // Sem ela, trocar a logo continua mostrando a anterior.
  const cacheBuster = versao === undefined ? '' : `?v=${versao}`;
  const uri = `${getLogoUrl()}${cacheBuster}`;

  if (!token) {
    logFalhaPreview(
      'getLogoSource',
      'sem token em storage; seguindo com a URL pura para o cookie jar autenticar',
    );
    return { uri };
  }

  return { uri, headers: { Authorization: `Bearer ${token}` } };
}

/**
 * Baixa a logo e devolve um object URL utilizável em <Image>. Só web.
 */
export async function buscarLogo(): Promise<string | null> {
  try {
    const { data } = await api.get<Blob>('/api/v1/empresa/logo', {
      responseType: 'blob',
    });

    return URL.createObjectURL(data);
  } catch (error) {
    logFalhaPreview('buscarLogo', describeErro(error));
    return null;
  }
}

/**
 * Entrega a origem da logo pronta para o <Image>, escolhida por plataforma.
 *
 * Retorna:
 * - null: falha na autenticação ou erro inesperado (não confirma a ausência da logo).
 * - string: object URL (Web).
 * - LogoSource: { uri, headers } (Native).
 */
export function carregarLogo(versao?: number): Promise<LogoSource | string | null> {
  return Platform.OS === 'web' ? buscarLogo() : getLogoSource(versao);
}

function logFalhaPreview(etapa: string, detalhe: string): void {
  if (__DEV__) {
    console.warn(`[empresa/logo] ${etapa} falhou: ${detalhe}`);
  }
}

function describeErro(error: unknown): string {
  const axiosError = error as { response?: { status?: number }; message?: string };
  const status = axiosError?.response?.status;
  return status ? `HTTP ${status}` : (axiosError?.message ?? 'erro desconhecido');
}
