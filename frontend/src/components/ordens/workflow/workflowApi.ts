import { coreApi } from "../../../lib/api";
import { storage } from "../../../lib/storage";
import type {
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

// Chave local para persistir enriquecimentos de alocação e status caso a OS ainda não
// tenha esses campos totalmente persistidos pelo endpoint base
function extraStorageKey(osId: string): string {
  return `@stentio:etapas_extras:${osId}`;
}

interface EtapaExtraInfo {
  recurso_id?: string | null;
  recurso_nome?: string | null;
  status?: EtapaWorkflow["status"];
}

async function carregarExtrasLocais(osId: string): Promise<Record<string, EtapaExtraInfo>> {
  try {
    if (typeof window !== "undefined" && window.localStorage) {
      const rawWeb = window.localStorage.getItem(extraStorageKey(osId));
      if (rawWeb) return JSON.parse(rawWeb) as Record<string, EtapaExtraInfo>;
    }
    const raw = await storage.getItem(extraStorageKey(osId));
    if (!raw) return {};
    return JSON.parse(raw) as Record<string, EtapaExtraInfo>;
  } catch {
    return {};
  }
}

export async function salvarExtrasLocais(
  osId: string,
  extras: Record<string, EtapaExtraInfo>,
): Promise<void> {
  try {
    const raw = JSON.stringify(extras);
    if (typeof window !== "undefined" && window.localStorage) {
      window.localStorage.setItem(extraStorageKey(osId), raw);
    }
    await storage.setItem(extraStorageKey(osId), raw);
  } catch (err) {
    console.warn("Falha ao salvar metadados locais de etapas:", err);
  }
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
  try {
    const { data } = await coreApi.get<OrdemServicoWorkflowResponse>(ENDPOINTS.etapasOs(osId));
    const extras = await carregarExtrasLocais(osId);

    const etapasEnriquecidas: EtapaWorkflow[] = (data.etapas || []).map((etapa, idx) => {
      const chave = etapa.id || `ordem_${etapa.ordem}`;
      const extra = extras[chave] || {};

      let statusCalculado = etapa.status;
      if (extra.status) {
        statusCalculado = extra.status;
      } else if (!statusCalculado) {
        // CA.4: A primeira etapa inicia PENDENTE e as demais AGUARDANDO_ETAPA_ANTERIOR
        statusCalculado = idx === 0 ? "PENDENTE" : "AGUARDANDO_ETAPA_ANTERIOR";
      }

      return {
        ...etapa,
        status: statusCalculado,
        recurso_id: extra.recurso_id !== undefined ? extra.recurso_id : etapa.recurso_id,
        recurso_nome: extra.recurso_nome || undefined,
      };
    });

    return {
      ordem_servico_id: data.ordem_servico_id || osId,
      status_os: data.status_os || "EM_PLANEJAMENTO",
      etapas: etapasEnriquecidas.sort((a, b) => a.ordem - b.ordem),
    };
  } catch (err: unknown) {
    const axiosErr = err as { response?: { status?: number } };
    if (axiosErr.response?.status === 404) {
      return {
        ordem_servico_id: osId,
        status_os: "PENDENTE_ALOCACAO",
        etapas: [],
      };
    }
    throw err;
  }
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

  // Prepara payload estritamente compatível
  const payload: SalvarWorkflowPayload = {
    etapas: etapas
      .sort((a, b) => a.ordem - b.ordem)
      .map((e, index) => ({
        ordem: index + 1,
        nome: e.nome.trim(),
        tipo_servico_id: e.tipo_servico_id,
        tipo_servico: e.tipo_servico_nome,
        prazo_estimado_horas: Number(e.prazo_estimado_horas),
      })),
  };

  const { data } = await coreApi.post<OrdemServicoWorkflowResponse>(ENDPOINTS.etapasOs(osId), payload);

  // Preserva metadados locais de alocação de recurso e status
  const extrasParaSalvar: Record<string, EtapaExtraInfo> = {};
  etapas.forEach((e) => {
    const chave = e.id || `ordem_${e.ordem}`;
    if (e.recurso_id || e.status) {
      extrasParaSalvar[chave] = {
        recurso_id: e.recurso_id,
        recurso_nome: e.recurso_nome,
        status: e.status,
      };
    }
  });
  await salvarExtrasLocais(osId, extrasParaSalvar);

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
  // Limpa extras locais antigos ao clonar novo modelo
  await salvarExtrasLocais(osId, {});
  return data;
}

/**
 * Lista modelos de workflow pré-definidos (GET /api/v1/modelos-workflow)
 */
export async function listarModelosWorkflow(): Promise<ModeloWorkflow[]> {
  try {
    const { data } = await coreApi.get<ModeloWorkflow[]>(ENDPOINTS.modelosWorkflow, {
      params: { ativo: true },
    });
    return extrairLista<ModeloWorkflow>(data);
  } catch {
    return [];
  }
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
    etapas: payload.etapas
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
  try {
    const { data } = await coreApi.get<{ content: TipoServicoOption[] }>(ENDPOINTS.tiposServico, {
      params: { ativo: true, size: 100 },
    });
    const lista = extrairLista<TipoServicoOption>(data);
    return lista.map((t) => ({ id: t.id, nome: t.nome }));
  } catch {
    return [];
  }
}

/**
 * Lista catálogo de recursos cadastrados disponíveis para alocação
 */
export async function listarRecursosWorkflow(): Promise<RecursoOption[]> {
  try {
    const { data } = await coreApi.get<{ content: RecursoOption[] }>(ENDPOINTS.recursos, {
      params: { ativo: true, size: 100 },
    });
    const lista = extrairLista<RecursoOption>(data);
    return lista.map((r) => ({
      id: r.id,
      nome: r.nome,
      email: r.email,
      telefone: r.telefone,
      ativo: r.ativo,
      tiposServico: r.tiposServico,
    }));
  } catch {
    return [];
  }
}
