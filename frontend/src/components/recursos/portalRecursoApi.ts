import { Platform } from "react-native";
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
  situacao: "PENDENTE" | "AGUARDANDO_ETAPA_ANTERIOR" | "ALOCADO" | "DISPARADA" | "ACEITA" | "ENTREGUE" | string;
  prazo_estimado_horas: number;
  data_limite_entrega: string | null;
  aceito_em?: string | null;
  arquivo_entrega_url?: string | null;
  arquivo_entrega_nome?: string | null;
}

export interface EtapasPortalRecurso {
  disponiveis: DemandaRecurso[];
  minhas: DemandaRecurso[];
}

export interface EntregaEtapaResponse {
  etapaId: string;
  status: "ENTREGUE";
  entregueEm: string;
  arquivo: { nome: string; url: string };
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

export async function listarEtapasPortal(): Promise<EtapasPortalRecurso> {
  const { data } = await coreApi.get<DemandaRecurso[] | Partial<EtapasPortalRecurso>>("/api/v1/recurso/etapas");
  if (Array.isArray(data)) {
    return {
      disponiveis: data.filter((etapa) => etapa.situacao === "DISPARADA"),
      minhas: data.filter((etapa) => etapa.situacao !== "DISPARADA"),
    };
  }
  return {
    disponiveis: data.disponiveis ?? [],
    minhas: data.minhas ?? [],
  };
}

export async function aceitarEtapa(etapaId: string): Promise<void> {
  await coreApi.post(`/api/v1/recurso/etapas/${etapaId}/aceitar`);
}

export async function recusarEtapa(etapaId: string): Promise<void> {
  await coreApi.post(`/api/v1/recurso/etapas/${etapaId}/recusar`);
}

export async function enviarEntrega(
  etapaId: string,
  arquivo: { uri: string; nome: string; tipo: string; file?: File },
  aoProgredir?: (percentual: number) => void,
): Promise<EntregaEtapaResponse> {
  const form = new FormData();
  if (Platform.OS === "web" && arquivo.file) {
    form.append("arquivo", arquivo.file, arquivo.nome);
  } else {
    form.append("arquivo", { uri: arquivo.uri, name: arquivo.nome, type: arquivo.tipo } as unknown as Blob);
  }
  const { data } = await coreApi.post<EntregaEtapaResponse>(
    `/api/v1/recurso/etapas/${etapaId}/entrega`,
    form,
    { timeout: 0, onUploadProgress: (event) => {
      if (event.total) aoProgredir?.(Math.round((event.loaded / event.total) * 100));
    } },
  );
  return data;
}

export async function baixarEntrega(url: string, nomeArquivo: string): Promise<void> {
  if (Platform.OS === "web" && typeof window !== "undefined") {
    const { data } = await coreApi.get(url, { responseType: "blob" });
    const blobUrl = window.URL.createObjectURL(new Blob([data]));
    const link = document.createElement("a");
    link.href = blobUrl;
    link.download = nomeArquivo;
    document.body.appendChild(link);
    link.click();
    link.remove();
    window.URL.revokeObjectURL(blobUrl);
  }
}

export async function buscarMeuPerfilRecurso(): Promise<PerfilRecurso> {
  const { data } = await coreApi.get<PerfilRecurso>("/api/v1/recurso/perfil");
  return data;
}
