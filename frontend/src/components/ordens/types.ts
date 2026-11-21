export type StatusOrdemServico =
  | "PENDENTE_ALOCACAO"
  | "EM_PLANEJAMENTO"
  | "EM_EXECUCAO"
  | "AGUARDANDO_VERIFICACAO"
  | "ENCERRADA"
  | "CONCLUIDA"
  | "FINALIZADA"
  | "CANCELADA"
  | string;

export interface OrdemServicoResumo {
  id: string;
  codigo_visual: string;
  status: StatusOrdemServico;
  cliente_id: string;
  cliente_nome: string;
  data_criacao: string;
}

export interface OrdemServicoPagina {
  pagina: number;
  tamanho_pagina: number;
  total_elementos: number;
  conteudo: OrdemServicoResumo[];
}

export interface OrdemServicoArquivo {
  id: string;
  nome: string;
  url: string;
}

export interface OrdemServicoSolicitacao {
  id: string;
  descricao: string;
  categoria: string;
  idioma_origem: string;
  idioma_destino: string;
  arquivos: OrdemServicoArquivo[];
}

export interface OrdemServicoCliente {
  id: string;
  nome: string;
}

export interface OrdemServicoEtapaResumo {
  id: string;
  ordem: number;
  nome: string;
  status:
    | "PENDENTE"
    | "AGUARDANDO_ETAPA_ANTERIOR"
    | "ALOCADO"
    | "DISPARADA"
    | "ACEITA"
    | "ENTREGUE"
    | string;
  entregue_em?: string | null;
  arquivo_nome?: string | null;
  recurso_nome?: string | null;
}

export interface OrdemServicoDetalhe {
  id: string;
  codigo: string;
  status: StatusOrdemServico;
  orcamento_id: string;
  data_criacao: string;
  entregue_em?: string | null;
  entregue_por?: string | null;
  motivo_revisao?: string | null;
  cliente: OrdemServicoCliente;
  solicitacao: OrdemServicoSolicitacao;
  etapas?: OrdemServicoEtapaResumo[];
}

export interface FiltrosOrdemServico {
  status?: string;
  clienteId?: string;
  pagina?: number;
  tamanhoPagina?: number;
}

export interface ClienteOpcao {
  id: string;
  nomeEmpresa: string;
}

// --- US-015: Revisão Final & Entrega ao Cliente ---

export interface EntregaFinalCliente {
  id: string;
  nome: string;
  email: string | null;
}

export interface EntregaFinalEtapa {
  etapa_id: string;
  nome: string;
  ordem: number;
  status: string;
  entregue_em: string | null;
  arquivo_nome: string | null;
  arquivo_url: string;
}

export interface EntregaFinalPacote {
  etapa_id: string;
  arquivo_nome: string | null;
  tipo_conteudo: string | null;
  tamanho_bytes: number;
  pode_enviar: boolean;
  bloqueios: string[];
}

export interface EntregaFinalRevisaoResponse {
  ordem_servico_id: string;
  codigo: string;
  status: StatusOrdemServico;
  motivo_revisao: string | null;
  cliente: EntregaFinalCliente;
  etapas: EntregaFinalEtapa[];
  pacote: EntregaFinalPacote;
}