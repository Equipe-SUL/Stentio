import { Platform } from "react-native";
import { coreApi } from "../../lib/api";
import type { PerfilRecursoCriado, UnidadePrecoRecurso } from "./cadastroRecursoApi";

export type SituacaoDemandaRecurso =
  | "PENDENTE"
  | "AGUARDANDO_ETAPA_ANTERIOR"
  | "ALOCADO"
  | "DISPARADA"
  | "ACEITA"
  | "ENTREGUE"
  | string;

export interface DemandaRecurso {
  etapa_id: string;
  ordem_servico_id: string;
  ordem_servico_codigo: string;
  ordem: number;
  nome: string;
  tipo_servico: string;
  idioma_origem: string;
  idioma_destino: string;
  situacao: SituacaoDemandaRecurso;
  prazo_estimado_horas: number;
  data_limite_entrega: string | null;
  liberada?: boolean;
  entregue_em?: string | null;
  arquivo_nome?: string | null;
}

export interface AvancoEtapaResponse {
  proxima_etapa_id: string;
  proxima_etapa_status: "DISPARADA" | "ALOCADO" | "ACEITA" | "PENDENTE" | string;
  convite_automatico: boolean;
  recursos_notificados: number;
}

export interface EntregaEtapaResponse {
  etapa_id: string;
  status: "ENTREGUE" | string;
  entregue_em: string;
  arquivo_nome: string;
  avanco: AvancoEtapaResponse | null;
  ordem_servico: {
    id: string;
    status: "EM_EXECUCAO" | "AGUARDANDO_VERIFICACAO" | string;
  };
}

export interface ArquivoEntregaPayload {
  uri: string;
  name: string;
  mimeType?: string;
  file?: File; // Web
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

export async function aceitarEtapaRecurso(etapaId: string): Promise<void> {
  await coreApi.post(`/api/v1/recurso/etapas/${etapaId}/aceitar`);
}

export async function entregarEtapaRecurso(
  etapaId: string,
  arquivo: ArquivoEntregaPayload,
): Promise<EntregaEtapaResponse> {
  const formData = new FormData();

  if (Platform.OS === "web" && arquivo.file) {
    formData.append("arquivo", arquivo.file, arquivo.name);
  } else {
    formData.append("arquivo", {
      uri: arquivo.uri,
      name: arquivo.name,
      type: arquivo.mimeType || "application/octet-stream",
    } as unknown as Blob);
  }

  const { data } = await coreApi.post<EntregaEtapaResponse>(
    `/api/v1/recurso/etapas/${etapaId}/entregar`,
    formData,
    {
      headers: {
        "Content-Type": "multipart/form-data",
      },
    },
  );
  return data;
}

export async function buscarMeuPerfilRecurso(): Promise<PerfilRecurso> {
  const { data } = await coreApi.get<PerfilRecurso>("/api/v1/recurso/perfil");
  return data;
}