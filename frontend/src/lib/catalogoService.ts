import { coreApi } from "./api";
import type { FieldDef, FiltroLista, FiltroStatus } from "../components/usuarios/types";

export function ativoDeFiltro(status: FiltroStatus): boolean | undefined {
  if (status === "ativos") return true;
  if (status === "inativos") return false;
  return undefined;
}

function montarQuery(filtro: FiltroLista): string {
  const params = new URLSearchParams();
  if (filtro.nome?.trim()) params.set("nome", filtro.nome.trim());
  if (filtro.ativo !== undefined) params.set("ativo", String(filtro.ativo));
  // Busca única: sem paginação para listar o catálogo inteiro em uma chamada.
  params.set("size", "100");
  return params.toString();
}

// --- Categorias de projeto -------------------------------------------------

export interface Categoria {
  id: string;
  nome: string;
}

export const camposCategoria: FieldDef<Categoria>[] = [
  { key: "nome", label: "Nome", type: "text" },
];

export async function listarCategorias(filtro: FiltroLista = {}): Promise<Categoria[]> {
  const { data } = await coreApi.get<{ content: Categoria[] }>(
    `/api/v1/categorias-projeto?${montarQuery(filtro)}`,
  );
  return data.content ?? data;
}

export async function criarCategoria(valores: Omit<Categoria, "id">): Promise<Categoria> {
  const { data } = await coreApi.post<Categoria>("/api/v1/categorias-projeto", { nome: valores.nome });
  return data;
}

export async function atualizarCategoria(id: string, valores: Omit<Categoria, "id">) {
  await coreApi.put(`/api/v1/categorias-projeto/${id}`, { nome: valores.nome });
}

export async function excluirCategoria(id: string) {
  await coreApi.delete(`/api/v1/categorias-projeto/${id}`);
}

// --- Tipos de serviço ------------------------------------------------------

export interface Servico {
  id: string;
  nome: string;
  ativo: boolean;
}

export const camposServico: FieldDef<Servico>[] = [
  { key: "nome", label: "Nome", type: "text" },
  { key: "ativo", label: "Status", type: "boolean" },
];

export async function listarServicos(filtro: FiltroLista = {}): Promise<Servico[]> {
  const { data } = await coreApi.get<{ content: Servico[] }>(
    `/api/v1/tipos-servico?${montarQuery(filtro)}`,
  );
  return data.content ?? data;
}

export async function criarServico(valores: Omit<Servico, "id">): Promise<Servico> {
  const { data } = await coreApi.post<Servico>("/api/v1/tipos-servico", { nome: valores.nome });
  return data;
}

export async function atualizarServico(id: string, valores: Omit<Servico, "id">) {
  await coreApi.put(`/api/v1/tipos-servico/${id}`, { nome: valores.nome });
}

export async function atualizarStatusServico(id: string, ativo: boolean) {
  await coreApi.patch(`/api/v1/tipos-servico/${id}/status`, { ativo });
}

// Soft delete: sem DELETE físico no Core (associação com recursos); apenas desativa.
export async function excluirServico(id: string) {
  await atualizarStatusServico(id, false);
}

// --- Idiomas ---------------------------------------------------------------

export interface Idioma {
  id: string;
  nome: string;
  codigoIso: string;
  ativo: boolean;
}

export const camposIdioma: FieldDef<Idioma>[] = [
  { key: "nome", label: "Nome", type: "text" },
  { key: "codigoIso", label: "Código (ISO)", type: "iso", placeholder: "ex: pt, pt-BR" },
  { key: "ativo", label: "Status", type: "boolean" },
];

export async function listarIdiomas(filtro: FiltroLista = {}): Promise<Idioma[]> {
  const { data } = await coreApi.get<{ content: Idioma[] }>(
    `/api/v1/idiomas?${montarQuery(filtro)}`,
  );
  return data.content ?? data;
}

export async function criarIdioma(valores: Omit<Idioma, "id">): Promise<Idioma> {
  const { data } = await coreApi.post<Idioma>("/api/v1/idiomas", {
    nome: valores.nome,
    codigoIso: valores.codigoIso,
  });
  return data;
}

export async function atualizarIdioma(id: string, valores: Omit<Idioma, "id">) {
  await coreApi.put(`/api/v1/idiomas/${id}`, {
    nome: valores.nome,
    codigoIso: valores.codigoIso,
  });
}

export async function atualizarStatusIdioma(id: string, ativo: boolean) {
  await coreApi.patch(`/api/v1/idiomas/${id}/status`, { ativo });
}

// Soft delete: sem DELETE físico no Core (associação com recursos); apenas desativa.
export async function excluirIdioma(id: string) {
  await atualizarStatusIdioma(id, false);
}
