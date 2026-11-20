import type { EntidadeAuditada, OrigemAcaoAuditoria } from "./types";

export const NOME_ENTIDADE: Record<EntidadeAuditada, string> = {
  ORCAMENTO: "Orçamento",
  ORDEM_SERVICO: "Ordem de Serviço",
  ORDEM_SERVICO_ETAPA: "Etapa de OS",
};

export const NOME_ORIGEM: Record<OrigemAcaoAuditoria, string> = {
  PAINEL_WEB: "Painel web",
  TOKEN_PUBLICO: "Link público",
  SISTEMA: "Sistema",
};

export function formatarStatus(status?: string | null): string {
  if (!status) return "Não informado";
  const separado = status.replace(/_/g, " ").toLocaleLowerCase("pt-BR");
  return separado.charAt(0).toLocaleUpperCase("pt-BR") + separado.slice(1);
}

export function formatarDataHora(iso: string): string {
  const data = new Date(iso);
  if (Number.isNaN(data.getTime())) return iso;
  return new Intl.DateTimeFormat("pt-BR", {
    day: "2-digit",
    month: "2-digit",
    year: "numeric",
    hour: "2-digit",
    minute: "2-digit",
  }).format(data);
}

export function iniciais(nome: string): string {
  const partes = nome.trim().split(/\s+/).filter(Boolean);
  if (!partes.length) return "?";
  return (partes.length > 1 ? partes[0][0] + partes[partes.length - 1][0] : partes[0].slice(0, 2)).toUpperCase();
}
