export type StatusEtapaWorkflow =
  | "PENDENTE"
  | "AGUARDANDO_ETAPA_ANTERIOR"
  | "EM_ANDAMENTO"
  | "CONCLUIDA";

export interface EtapaWorkflow {
  id?: string;
  ordem: number;
  nome: string;
  tipo_servico_id: string;
  tipo_servico_nome?: string;
  prazo_estimado_horas: number;
  status: StatusEtapaWorkflow;
  recurso_id?: string | null;
  recurso_nome?: string | null;
}

export interface OrdemServicoWorkflowResponse {
  ordem_servico_id: string;
  status_os: string;
  etapas: EtapaWorkflow[];
}

export interface ModeloWorkflowEtapaItem {
  id?: string;
  ordem: number;
  nome: string;
  tipo_servico_id: string;
  tipo_servico_nome?: string;
  prazo_estimado_horas: number;
}

export interface ModeloWorkflow {
  id: string;
  nome: string;
  descricao?: string;
  ativo?: boolean;
  etapas: ModeloWorkflowEtapaItem[];
}

export interface SalvarEtapaPayload {
  ordem: number;
  nome: string;
  tipo_servico_id: string;
  tipo_servico?: string;
  prazo_estimado_horas: number;
}

export interface SalvarWorkflowPayload {
  etapas: SalvarEtapaPayload[];
}

export interface CriarModeloWorkflowPayload {
  nome: string;
  descricao?: string;
  etapas: SalvarEtapaPayload[];
}

export interface TipoServicoOption {
  id: string;
  nome: string;
}

export interface RecursoOption {
  id: string;
  nome: string;
  email?: string;
  telefone?: string;
  ativo?: boolean;
  tiposServico?: { id: string; nome: string }[];
}
