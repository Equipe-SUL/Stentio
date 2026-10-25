export interface ItemOrcamento {
  'tipo_de_serviço': string;
  quantidade: number;
  unidade: string | null;
  valor_unitario: number;
  subtotal: number;
}
export type StatusOrcamento = 'EM_ELABORACAO' | 'AGUARDANDO_APROVACAO' | 'APROVADO' | 'REJEITADO';
export interface OrcamentoPublico {
  id: string;
  status: StatusOrcamento;
  valor_total: number;
  data_expiracao: string;
  itens: ItemOrcamento[];
}
export interface DecisaoOrcamento {
  id: string;
  status: 'APROVADO' | 'REJEITADO';
  data_decisao: string;
}

export class ErroOrcamento extends Error {
  indisponivel: boolean;
  constructor(message: string, indisponivel = false) {
    super(message); this.indisponivel = indisponivel;
  }
}
export const LINK_INDISPONIVEL = 'Este link está inválido, expirado ou já foi utilizado. Solicite um novo link à equipe de atendimento.';
export function podeDecidir(status: string): boolean {
  return status === 'AGUARDANDO_APROVACAO';
}
export function validarMotivo(motivo: string): string | null {
  if (!motivo.trim()) return 'Informe o motivo da rejeição.';
  if (motivo.length > 2000) return 'O motivo deve ter no máximo 2000 caracteres.';
  return null;
}

export function baseUrl(): string {
  // Em produção, o Nginx encaminha /api/v1/public/orcamentos ao Core.
  // No Expo local, o Core é acessado diretamente; respeita origem explícita.
  const configurada = process.env.EXPO_PUBLIC_CORE_API_URL;
  if (configurada !== undefined) return configurada.replace(/\/$/, '');
  if (process.env.NODE_ENV === 'production') return '';
  if (typeof window !== 'undefined') {
    const url = new URL(window.location.origin);
    url.port = '8082';
    return url.origin;
  }
  return 'http://localhost:8082';
}
export function nomeUnidade(unidade: string | null): string {
  if (!unidade) return 'Não informada';
  return ({ PALAVRA: 'Palavra', PAGINA: 'Página', HORA: 'Hora' } as Record<string, string>)[unidade] ?? unidade;
}
async function requisitar<T>(path: string, init?: RequestInit): Promise<T> {
  const controller = new AbortController();
  const timeout = setTimeout(() => controller.abort(), 15000);
  try {
    const response = await fetch(baseUrl() + '/api/v1/public/orcamentos' + path, {
      ...init, credentials: 'omit', cache: 'no-store', referrerPolicy: 'no-referrer',
      signal: controller.signal,
    });
    if (!response.ok) {
      const body = await response.json().catch(() => ({}));
      const indisponivel = response.status === 400 && body.message === 'Link inválido, expirado ou já utilizado';
      throw new ErroOrcamento(indisponivel ? LINK_INDISPONIVEL :
        response.status === 400 ? 'Não foi possível enviar a decisão. Verifique o motivo informado.' :
        'Não foi possível concluir a solicitação. Tente novamente.', indisponivel);
    }
    return await response.json() as T;
  } catch (error) {
    if (error instanceof ErroOrcamento) throw error;
    throw new ErroOrcamento('Não foi possível conectar ao servidor. Verifique sua conexão e tente novamente.');
  } finally { clearTimeout(timeout); }
}
export function consultarOrcamento(token: string): Promise<OrcamentoPublico> {
  if (!token.trim()) return Promise.reject(new ErroOrcamento(LINK_INDISPONIVEL, true));
  return requisitar('?token=' + encodeURIComponent(token));
}
export function decidirOrcamento(token: string, acao: 'aprovar' | 'rejeitar', motivo: string): Promise<DecisaoOrcamento> {
  if (acao === 'rejeitar') {
    const erro = validarMotivo(motivo);
    if (erro) return Promise.reject(new ErroOrcamento(erro));
  }
  return requisitar('/' + acao, {
    method: 'POST', headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(acao === 'aprovar' ? { token } : { token, motivo: motivo.trim() }),
  });
}

export interface EmpresaPublica {
  nome: string;
  cnpj: string;
  endereco: string;
  telefone: string | null;
  logo_disponivel: boolean;
}

export interface ArquivoPublico {
  id: string;
  nome: string;
  tipo_conteudo?: string | null;
  tamanho_bytes: number;
}

export interface CategoriaPublica {
  id: string;
  nome: string;
}

export interface IdiomaPublico {
  id: string;
  nome: string;
  codigo_iso: string;
}

export interface SolicitacaoPublica {
  id: string;
  descricao?: string | null;
  data_recebimento?: string | null;
  categoria?: CategoriaPublica | null;
  idioma_origem?: IdiomaPublico | null;
  idioma_destino?: IdiomaPublico | null;
  arquivos?: ArquivoPublico[];
}

export interface OrcamentoPublico {
  id: string;
  status: StatusOrcamento;
  valor_total: number;
  data_expiracao: string;
  itens: ItemOrcamento[];
  numero_versao?: number | null;
  criado_em?: string | null;
  solicitacao?: SolicitacaoPublica | null;
}

export function getLogoEmpresaPublicaUrl(token: string): string {
  if (!token.trim()) return '';
  return baseUrl() + '/api/v1/public/orcamentos/empresa/logo?token=' + encodeURIComponent(token);
}

export function getUrlArquivoPublico(token: string, arquivoId: string): string {
  if (!token.trim() || !arquivoId) return '';
  return baseUrl() + '/api/v1/public/orcamentos/arquivos/' + arquivoId + '?token=' + encodeURIComponent(token);
}

export async function consultarEmpresaPublica(token: string): Promise<EmpresaPublica> {
  if (!token.trim()) throw new ErroOrcamento(LINK_INDISPONIVEL, true);
  return requisitar('/empresa?token=' + encodeURIComponent(token));
}

export async function baixarArquivoPublico(token: string, arquivoId: string, nome: string): Promise<void> {
  if (!token.trim() || !arquivoId) throw new ErroOrcamento(LINK_INDISPONIVEL, true);
  const url = getUrlArquivoPublico(token, arquivoId);
  const response = await fetch(url, { credentials: 'omit', cache: 'no-store', referrerPolicy: 'no-referrer' });
  if (!response.ok) {
    throw new ErroOrcamento('Não foi possível baixar o arquivo.');
  }
  const blob = await response.blob();
  const link = document.createElement('a');
  link.href = window.URL.createObjectURL(blob);
  link.download = nome || 'arquivo';
  document.body.appendChild(link);
  link.click();
  document.body.removeChild(link);
  window.URL.revokeObjectURL(link.href);
}
