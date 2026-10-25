import { Platform } from "react-native";
import { coreApi } from "../../lib/api";
import type {
  ClienteOpcao,
  FiltrosOrdemServico,
  OrdemServicoDetalhe,
  OrdemServicoPagina,
} from "./types";

const ENDPOINTS = {
  ordens: "/api/v1/ordens-servico",
  clientes: "/api/v1/clientes",
} as const;

export async function listarOrdensServico(
  filtros: FiltrosOrdemServico = {},
): Promise<OrdemServicoPagina> {
  const params: Record<string, string | number> = {
    pagina: filtros.pagina ?? 1,
    tamanho_pagina: filtros.tamanhoPagina ?? 10,
  };

  if (filtros.status && filtros.status !== "TODOS") {
    params.status = filtros.status;
  }

  if (filtros.clienteId) {
    params.cliente_id = filtros.clienteId;
  }

  const { data } = await coreApi.get<OrdemServicoPagina>(ENDPOINTS.ordens, { params });
  return data;
}

export async function buscarOrdemServicoPorId(id: string): Promise<OrdemServicoDetalhe> {
  const { data } = await coreApi.get<OrdemServicoDetalhe>(`${ENDPOINTS.ordens}/${id}`);
  return data;
}

export async function baixarArquivoOrdemServico(
  url: string,
  nomeArquivo: string,
): Promise<void> {
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

function extrairLista<T>(data: unknown): T[] {
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

export async function listarClientesOpcoes(): Promise<ClienteOpcao[]> {
  try {
    const { data } = await coreApi.get(ENDPOINTS.clientes, {
      params: { size: 100, sort: "nomeEmpresa" },
    });
    const lista = extrairLista<{ id: string; nomeEmpresa: string }>(data);
    return lista
      .map((c) => ({ id: c.id, nomeEmpresa: c.nomeEmpresa }))
      .sort((a, b) => a.nomeEmpresa.localeCompare(b.nomeEmpresa, "pt-BR"));
  } catch {
    return [];
  }
}
