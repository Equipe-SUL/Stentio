import axios from 'axios';
import { Platform } from 'react-native';
import { getToken } from './auth';

// --- Tipos -------------------------------------------------------------------

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
  const configured = process.env.EXPO_PUBLIC_EMPRESA_API_URL ?? DEFAULT_EMPRESA_API_URL;

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
 * Retorna a URL da rota da logo, para uso direto em <Image>.
 * Só funciona se o servidor aceitar a requisição sem o header Authorization
 * (ex.: imagem pública) — prefira buscarLogo().
 */
export function getLogoUrl(): string {
  return `${EMPRESA_API_URL}/api/v1/empresa/logo`;
}

/**
 * Baixa a logo já autenticada e devolve uma URI utilizável em <Image>.
 *
 * Não dá para apontar o <Image> direto para a rota: no web ele vira um <img>,
 * que não aceita header Authorization, e o cookie SameSite=Lax não é enviado em
 * requisição cross-site. Buscar via axios (que carrega o token) e entregar um
 * object URL resolve os dois casos.
 *
 * Retorna null quando ainda não há logo.
 */
export async function buscarLogo(): Promise<string | null> {
  try {
    const { data } = await api.get<Blob>('/api/v1/empresa/logo', {
      responseType: 'blob',
    });

    return URL.createObjectURL(data);
  } catch {
    // 404 = empresa sem logo ainda. 401/403 = sessão sem permissão.
    // Nos demais casos também devolvemos null para não derrubar a tela por
    // causa da prévia, que é apenas decorativa.
    return null;
  }
}
