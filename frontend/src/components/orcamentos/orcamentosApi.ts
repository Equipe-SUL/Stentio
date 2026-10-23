import { coreApi } from "../../lib/api";

export type StatusOrcamento = "EM_ELABORACAO" | "AGUARDANDO_APROVACAO" | "APROVADO" | "REJEITADO";
export type UnidadeCobranca = "PALAVRA" | "HORA" | "PAGINA";
export interface OrcamentoResumo { id: string; versao: number; cliente_id: string; solicitacao_id: string; status: StatusOrcamento; valor_total: number; data_expiracao: string; }
export interface OrcamentoServico { id?: string; tipo_servico: string; quantidade: number; unidade: UnidadeCobranca; valor_unitario: number; subtotal?: number; }
export interface OrcamentoItem { id?: string; ordem: number; idioma_origem_id: string; idioma_destino_id: string; subtotal_item?: number; servicos: OrcamentoServico[]; }
export interface OrcamentoDetalhe { id: string; versao: number; cliente_id: string; solicitacao_id: string; status: StatusOrcamento; valor_total: number; data_expiracao: string; itens: OrcamentoItem[]; }
export interface PaginaOrcamentos { pagina: number; tamanho_pagina: number; total_elementos: number; conteudo: OrcamentoResumo[]; }
export type OrcamentoItemInput = Pick<OrcamentoItem, "ordem" | "idioma_origem_id" | "idioma_destino_id" | "servicos">;

export async function listarOrcamentos(pagina: number, tamanhoPagina: number, status?: StatusOrcamento) {
  const { data } = await coreApi.get<PaginaOrcamentos>("/api/v1/orcamentos", { params: { pagina, tamanho_pagina: tamanhoPagina, ...(status ? { status } : {}) } });
  return data;
}
export async function buscarOrcamento(id: string) {
  const { data } = await coreApi.get<OrcamentoDetalhe>(`/api/v1/orcamentos/${id}`);
  return data;
}
export async function criarCabecalho(solicitacaoId: string, clienteId: string, dataExpiracao: string) {
  const { data } = await coreApi.post<OrcamentoDetalhe>("/api/v1/orcamentos", { solicitacao_id: solicitacaoId, cliente_id: clienteId, data_expiracao: dataExpiracao });
  return data;
}
export async function salvarItens(id: string, itens: OrcamentoItemInput[]) {
  const { data } = await coreApi.post<OrcamentoDetalhe>(`/api/v1/orcamentos/${id}/itens`, { itens });
  return data;
}
