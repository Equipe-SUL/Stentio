export type StatusOrdemServico = "PENDENTE_ALOCACAO" | string;

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

export interface OrdemServicoDetalhe {
  id: string;
  codigo: string;
  status: StatusOrdemServico;
  orcamento_id: string;
  data_criacao: string;
  cliente: OrdemServicoCliente;
  solicitacao: OrdemServicoSolicitacao;
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
