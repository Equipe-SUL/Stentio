export type StatusOrdemServico =
  | "PENDENTE_ALOCACAO"
  | "EM_PLANEJAMENTO"
  | "EM_EXECUCAO"
  | "AGUARDANDO_VERIFICACAO"
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