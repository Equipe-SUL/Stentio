/** Contrato JSON real do Core (US-019). */
export type EntidadeAuditada = "ORCAMENTO" | "ORDEM_SERVICO" | "ORDEM_SERVICO_ETAPA";
export type OrigemAcaoAuditoria = "PAINEL_WEB" | "TOKEN_PUBLICO" | "SISTEMA";

export interface UsuarioAuditoria {
  id: string;
  nome: string;
  perfil: string;
}

export interface LogAuditoria {
  id: string;
  entidade: EntidadeAuditada;
  entidade_id: string;
  referencia_legivel: string;
  status_anterior: string | null;
  status_novo: string;
  usuario: UsuarioAuditoria;
  origem_acao: OrigemAcaoAuditoria;
  /** ISO 8601 UTC, por exemplo 2026-11-14T14:32:10Z. */
  data_hora: string;
  observacao: string | null;
}

export interface PaginaLogsAuditoria {
  total_registros: number;
  pagina_atual: number;
  tamanho_pagina: number;
  total_paginas: number;
  logs: LogAuditoria[];
}

/** Parâmetros opcionais de GET /api/v1/auditoria/logs. A página começa em 1. */
export interface FiltrosLogsAuditoria {
  entidade?: EntidadeAuditada;
  entidade_id?: string;
  usuario_id?: string;
  data_inicio?: string;
  data_fim?: string;
  busca?: string;
  page?: number;
  size?: number;
}
