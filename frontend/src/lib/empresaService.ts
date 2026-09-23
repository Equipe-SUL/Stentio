import { coreApi } from './api';

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

// --- Funções -----------------------------------------------------------------

/**
 * Busca os dados atuais da empresa.
 * Retorna null se a empresa ainda não foi cadastrada (404 / erro de servidor).
 */
export async function buscarEmpresa(): Promise<EmpresaResponse | null> {
  try {
    const { data } = await coreApi.get<EmpresaResponse>('/api/v1/empresa');
    return data;
  } catch (error: any) {
    // 404 ou 500 indica que ainda não existe registro — retorna null para exibir
    // o formulário vazio ao invés de uma mensagem de erro genérica.
    const status = error?.response?.status;
    if (status === 404 || status === 500) {
      return null;
    }
    throw error;
  }
}

/**
 * Cria ou atualiza os dados da empresa (PUT idempotente no backend).
 */
export async function salvarEmpresa(request: EmpresaRequest): Promise<EmpresaResponse> {
  const { data } = await coreApi.put<EmpresaResponse>('/api/v1/empresa', request);
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

  // FormData aceita tanto File (web) quanto o objeto nativo do Expo.
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  formData.append('arquivo', { uri, name, type } as any);

  const { data } = await coreApi.put<EmpresaResponse>('/api/v1/empresa/logo', formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
  });
  return data;
}

/**
 * Retorna a URL pública da logo da empresa para uso em <Image>.
 */
export function getLogoUrl(coreApiBaseUrl: string): string {
  return `${coreApiBaseUrl}/api/v1/empresa/logo`;
}

