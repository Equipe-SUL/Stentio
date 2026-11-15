import { coreApi } from "../../../lib/api";
import type {
  AlocarRecursoPayload,
  CriarModeloWorkflowPayload,
  EtapaWorkflow,
  ModeloWorkflow,
  OrdemServicoWorkflowResponse,
  RecursoOption,
  SalvarWorkflowPayload,
  TipoServicoOption,
} from "./types";

const ENDPOINTS = {
  etapasOs: (osId: string) => `/api/v1/ordens-servico/${osId}/etapas`,
  aplicarModelo: (osId: string, modeloId: string) =>
    `/api/v1/ordens-servico/${osId}/etapas/aplicar-modelo/${modeloId}`,
  alocacao: (osId: string, etapaId: string) =>
    `/api/v1/ordens-servico/${osId}/etapas/${etapaId}/alocacao`,
  modelosWorkflow: "/api/v1/modelos-workflow",
  tiposServico: "/api/v1/tipos-servico",
  recursos: "/api/v1/recursos",
};

function extrairLista<T>(data: unknown): T[] {
  if (!data) return [];
  if (Array.isArray(data)) return data as T[];
  const content = (data as { content?: unknown } | null)?.content;
  if (Array.isArray(content)) return content as T[];
  const embedded = (data as { _embedded?: Record<string, unknown> } | null)?._embedded;
  if (embedded) {
    const primeiraLista = Object.values(embedded).find(Array.isArray);
    if (Array.isArray(primeiraLista)) return primeiraLista as T[];
  }
  return [];
}

/**
 * Validação RN.1 e RN.2 da sequência de etapas antes de enviar
 */
export function validarSequenciaEtapas(
  etapas: { ordem: number; prazo_estimado_horas: number; nome: string; tipo_servico_id: string }[],
): { valido: boolean; mensagem?: string } {
  if (!etapas || etapas.length === 0) {
    return { valido: false, mensagem: "Informe ao menos uma etapa no workflow." };
  }

  // RN.2: Prazo estimado em horas de cada etapa deve ser obrigatoriamente maior que zero
  for (const e of etapas) {
    if (!e.nome?.trim()) {
      return { valido: false, mensagem: "Toda etapa deve ter um nome preenchido." };
    }
    if (!e.tipo_servico_id) {
      return { valido: false, mensagem: `A etapa "${e.nome}" precisa de um tipo de serviço selecionado.` };
    }
    if (!e.prazo_estimado_horas || e.prazo_estimado_horas <= 0) {
      return {
        valido: false,
        mensagem: `A etapa "${e.nome}" possui prazo inválido (${e.prazo_estimado_horas}h). O prazo deve ser maior que zero (RN.2).`,
      };
    }
  }

  // RN.1: Não é permitido salvar uma sequência contendo números de ordem duplicados ou lacunas
  const ordens = etapas.map((e) => e.ordem).sort((a, b) => a - b);
  for (let i = 0; i < ordens.length; i++) {
    const esperado = i + 1;
    if (ordens[i] !== esperado) {
      return {
        valido: false,
        mensagem: `A numeração de ordem contém falhas ou duplicatas (esperado ${esperado}, encontrado ${ordens[i]}). A sequência deve ser 1, 2, 3... (RN.1).`,
      };
    }
  }

  return { valido: true };
}

/**
 * Consulta a lista e árvore de etapas de uma Ordem de Serviço (GET /api/v1/ordens-servico/{os_id}/etapas)
 */
export async function buscarWorkflowOrdem(osId: string): Promise<OrdemServicoWorkflowResponse> {
  const { data } = await coreApi.get<OrdemServicoWorkflowResponse>(ENDPOINTS.etapasOs(osId));

  const etapas = (data.etapas || [])
    .map((etapa, idx) => ({
      ...etapa,
      status:
        etapa.status ?? (idx === 0 ? "PENDENTE" : "AGUARDANDO_ETAPA_ANTERIOR"),
    }))
    .sort((a, b) => a.ordem - b.ordem);

  return {
    ordem_servico_id: data.ordem_servico_id || osId,
    status_os: data.status_os,
    etapas,
  };
}

/**
 * Criação/Substituição manual da sequência de etapas (POST /api/v1/ordens-servico/{os_id}/etapas)
 */
export async function salvarWorkflowManual(
  osId: string,
  etapas: EtapaWorkflow[],
): Promise<OrdemServicoWorkflowResponse> {
  const validacao = validarSequenciaEtapas(etapas);
  if (!validacao.valido) {
    throw new Error(validacao.mensagem);
  }

  const payload: SalvarWorkflowPayload = {
    etapas: [...etapas]
      .sort((a, b) => a.ordem - b.ordem)
      .map((e, index) => ({
        ordem: index + 1,
        nome: e.nome.trim(),
        tipo_servico_id: e.tipo_servico_id,
        prazo_estimado_horas: Number(e.prazo_estimado_horas),
      })),
  };

  const { data } = await coreApi.post<OrdemServicoWorkflowResponse>(
    ENDPOINTS.etapasOs(osId),
    payload,
  );
  return data;
}

/**
 * Aplica um modelo pré-definido na Ordem de Serviço (POST /api/v1/ordens-servico/{os_id}/etapas/aplicar-modelo/{modelo_id})
 */
export async function aplicarModeloWorkflow(
  osId: string,
  modeloId: string,
): Promise<OrdemServicoWorkflowResponse> {
  const { data } = await coreApi.post<OrdemServicoWorkflowResponse>(
    ENDPOINTS.aplicarModelo(osId, modeloId),
  );
  return data;
}

/**
 * Aloca um recurso a uma etapa existente (POST /api/v1/ordens-servico/{os_id}/etapas/{etapa_id}/alocacao)
 */
export async function alocarRecursoEtapa(
  osId: string,
  etapaId: string,
  payload: AlocarRecursoPayload,
): Promise<void> {
  await coreApi.post(ENDPOINTS.alocacao(osId, etapaId), payload);
}

/**
 * Lista modelos de workflow pré-definidos (GET /api/v1/modelos-workflow)
 */
export async function listarModelosWorkflow(): Promise<ModeloWorkflow[]> {
  const { data } = await coreApi.get<ModeloWorkflow[]>(ENDPOINTS.modelosWorkflow, {
    params: { ativo: true },
  });
  return extrairLista<ModeloWorkflow>(data);
}

/**
 * Cria e salva um novo modelo de workflow (POST /api/v1/modelos-workflow)
 */
export async function salvarNovoModeloWorkflow(
  payload: CriarModeloWorkflowPayload,
): Promise<ModeloWorkflow> {
  const validacao = validarSequenciaEtapas(payload.etapas);
  if (!validacao.valido) {
    throw new Error(validacao.mensagem);
  }

  const { data } = await coreApi.post<ModeloWorkflow>(ENDPOINTS.modelosWorkflow, {
    nome: payload.nome.trim(),
    descricao: payload.descricao?.trim() || undefined,
    etapas: [...payload.etapas]
      .sort((a, b) => a.ordem - b.ordem)
      .map((e, index) => ({
        ordem: index + 1,
        nome: e.nome.trim(),
        tipo_servico_id: e.tipo_servico_id,
        prazo_estimado_horas: Number(e.prazo_estimado_horas),
      })),
  });

  return data;
}

/**
 * Lista catálogo de tipos de serviço disponíveis
 */
export async function listarTiposServicoWorkflow(): Promise<TipoServicoOption[]> {
  const { data } = await coreApi.get<{ content: TipoServicoOption[] }>(ENDPOINTS.tiposServico, {
    params: { ativo: true, size: 100 },
  });
  return extrairLista<TipoServicoOption>(data).map((t) => ({ id: t.id, nome: t.nome }));
}

/**
 * Lista catálogo de recursos cadastrados disponíveis para alocação
 */
export async function listarRecursosWorkflow(): Promise<RecursoOption[]> {
  const { data } = await coreApi.get<{ content: RecursoOption[] }>(ENDPOINTS.recursos, {
    params: { ativo: true, size: 100 },
  });
  return extrairLista<RecursoOption>(data).map((r) => ({
    id: r.id,
    nome: r.nome,
    email: r.email,
    telefone: r.telefone,
    ativo: r.ativo,
    tiposServico: r.tiposServico,
  }));
}
