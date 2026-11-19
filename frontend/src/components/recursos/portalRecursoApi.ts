import { coreApi } from "../../lib/api";
import type { PerfilRecursoCriado, UnidadePrecoRecurso } from "./cadastroRecursoApi";

export interface DemandaRecurso {
  etapa_id: string;
  ordem_servico_id: string;
  ordem_servico_codigo: string;
  ordem: number;
  nome: string;
  tipo_servico: string;
  idioma_origem: string;
  idioma_destino: string;
  situacao: "PENDENTE" | "AGUARDANDO_ETAPA_ANTERIOR" | "ALOCADO" | string;
  prazo_estimado_horas: number;
  data_limite_entrega: string | null;
}

export interface PerfilRecurso extends PerfilRecursoCriado {
  precos: {
    id?: string;
    idiomaOrigem: { id: string; nome: string; codigoIso: string };
    idiomaDestino: { id: string; nome: string; codigoIso: string };
    unidade: UnidadePrecoRecurso;
    valor: number;
  }[];
}

export async function listarMinhasDemandas(): Promise<DemandaRecurso[]> {
  const { data } = await coreApi.get<DemandaRecurso[]>("/api/v1/recurso/etapas");
  return data;
}

export async function buscarMeuPerfilRecurso(): Promise<PerfilRecurso> {
  const { data } = await coreApi.get<PerfilRecurso>("/api/v1/recurso/perfil");
  return data;
}
