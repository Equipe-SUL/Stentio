import { Platform } from "react-native";
import { coreApi } from "../../lib/api";

export type StatusCertificacao = "PENDENTE" | "APROVADA" | "REJEITADA";

export interface IdiomaCertificacao {
  id: string;
  nome: string;
  codigo_iso?: string;
  codigoIso?: string;
}

export interface CertificacaoResponse {
  id: string;
  recurso_id: string;
  recurso_nome?: string;
  idioma_origem: IdiomaCertificacao;
  idioma_destino: IdiomaCertificacao;
  status: StatusCertificacao;
  arquivo_nome: string;
  enviado_em: string;
  analisado_em: string | null;
  motivo_rejeicao: string | null;
}

export interface CertificacaoPaginaResponse {
  total_registros: number;
  pagina_atual: number;
  tamanho_pagina: number;
  total_paginas: number;
  certificacoes: CertificacaoResponse[];
}

export interface ArquivoCertificacao {
  uri: string;
  name: string;
  type?: string;
  size?: number;
  file?: File | Blob;
}

/**
 * Download de arquivo genérico compatível com web.
 */
async function baixarArquivoViaBlob(url: string, nomeArquivo: string): Promise<void> {
  if (Platform.OS === "web" && typeof window !== "undefined") {
    const { data } = await coreApi.get(url, { responseType: "blob" });
    const blobUrl = window.URL.createObjectURL(new Blob([data]));
    const link = document.createElement("a");
    link.href = blobUrl;
    link.setAttribute("download", nomeArquivo);
    document.body.appendChild(link);
    link.click();
    link.remove();
    window.URL.revokeObjectURL(blobUrl);
  }
}

// -------------------------------------------------------------
// Ações do Gestor de Projetos / Admin (/api/v1/recursos/certificacoes)
// -------------------------------------------------------------

export async function listarCertificacoesGestor(params?: {
  status?: StatusCertificacao;
  pagina?: number;
  tamanho_pagina?: number;
}): Promise<CertificacaoPaginaResponse> {
  const query = new URLSearchParams();
  if (params?.status) query.set("status", params.status);
  if (params?.pagina) query.set("pagina", String(params.pagina));
  if (params?.tamanho_pagina) query.set("tamanho_pagina", String(params.tamanho_pagina));

  const url = `/api/v1/recursos/certificacoes${query.toString() ? `?${query.toString()}` : ""}`;
  const { data } = await coreApi.get<CertificacaoPaginaResponse>(url);
  return data;
}

export async function aprovarCertificacao(id: string): Promise<CertificacaoResponse> {
  const { data } = await coreApi.post<CertificacaoResponse>(
    `/api/v1/recursos/certificacoes/${id}/aprovar`
  );
  return data;
}

export async function rejeitarCertificacao(id: string, motivo: string): Promise<CertificacaoResponse> {
  const { data } = await coreApi.post<CertificacaoResponse>(
    `/api/v1/recursos/certificacoes/${id}/rejeitar`,
    { motivo }
  );
  return data;
}

export async function baixarArquivoCertificacaoGestor(id: string, nomeArquivo: string): Promise<void> {
  await baixarArquivoViaBlob(`/api/v1/recursos/certificacoes/${id}/arquivo`, nomeArquivo);
}

// -------------------------------------------------------------
// Ações do Profissional / Recurso (/api/v1/recurso/certificacoes)
// -------------------------------------------------------------

export async function listarMinhasCertificacoes(): Promise<CertificacaoResponse[]> {
  const { data } = await coreApi.get<CertificacaoResponse[]>("/api/v1/recurso/certificacoes");
  return data ?? [];
}

export async function enviarCertificacao(
  idiomaOrigemId: string,
  idiomaDestinoId: string,
  arquivo: ArquivoCertificacao
): Promise<CertificacaoResponse> {
  const formData = new FormData();
  formData.append("idioma_origem_id", idiomaOrigemId);
  formData.append("idioma_destino_id", idiomaDestinoId);

  if (Platform.OS === "web" && arquivo.file) {
    formData.append("arquivo", arquivo.file, arquivo.name);
  } else {
    formData.append("arquivo", {
      uri: arquivo.uri,
      name: arquivo.name,
      type: arquivo.type ?? "application/octet-stream",
    } as unknown as Blob);
  }

  const { data } = await coreApi.post<CertificacaoResponse>(
    "/api/v1/recurso/certificacoes",
    formData
  );
  return data;
}

export async function baixarMinhaCertificacao(id: string, nomeArquivo: string): Promise<void> {
  await baixarArquivoViaBlob(`/api/v1/recurso/certificacoes/${id}/arquivo`, nomeArquivo);
}
