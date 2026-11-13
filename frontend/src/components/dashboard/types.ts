/** Contratos da US-020, conforme os DTOs JSON do serviço Core. */
export interface DashboardResumo {
  total_orcamentos_em_elaboracao: number;
  total_orcamentos_aguardando_aprovacao: number;
  total_os_em_andamento: number;
  total_etapas_atrasadas: number;
  taxa_conversao_orcamentos: number;
}

export type TipoPendencia = "ETAPA_OS" | "ORCAMENTO";

export interface Pendencia {
  id: string;
  tipo_entidade: TipoPendencia;
  titulo: string;
  status: string;
  /** Instante ISO 8601, ou null quando não houver prazo definido. */
  data_limite: string | null;
  /** Caminho de navegação fornecido pelo backend (não montar usando id). */
  link_acao: string;
}

export interface DashboardPendencias {
  total_itens: number;
  pendencias: Pendencia[];
}
