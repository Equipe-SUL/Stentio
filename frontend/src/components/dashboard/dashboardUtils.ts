import type { Pendencia } from "./types";

export type FiltroPendencias = "TODAS" | "ATRASADAS" | "ETAPA_OS" | "ORCAMENTO";

const dataEmMs = (iso: string | null): number => {
  if (!iso) return Number.POSITIVE_INFINITY;
  const time = new Date(iso).getTime();
  return Number.isNaN(time) ? Number.POSITIVE_INFINITY : time;
};

/** Ordenação determinística com datas nulas no final; não altera o array original. */
export function ordenarPendencias(pendencias: Pendencia[]): Pendencia[] {
  return [...pendencias].sort((a, b) =>
    dataEmMs(a.data_limite) - dataEmMs(b.data_limite) ||
    a.tipo_entidade.localeCompare(b.tipo_entidade) ||
    a.id.localeCompare(b.id)
  );
}

export function filtrarPendencias(pendencias: Pendencia[], filtro: FiltroPendencias) {
  if (filtro === "TODAS") return pendencias;
  if (filtro === "ATRASADAS") return pendencias.filter(p => p.status === "ATRASADO");
  return pendencias.filter(p => p.tipo_entidade === filtro);
}

/** Não marcar ETAPA_OS como atrasada apenas pela data: RN.1 depende do arquivo final. */
export function ehAtrasada(pendencia: Pendencia): boolean {
  return pendencia.tipo_entidade === "ETAPA_OS" && pendencia.status === "ATRASADO";
}

export function prazoVencido(iso: string | null): boolean {
  return Number.isFinite(dataEmMs(iso)) && dataEmMs(iso) < Date.now();
}

export function formatarData(iso: string | null): string {
  const ms = dataEmMs(iso);
  if (!Number.isFinite(ms)) return "Sem prazo";
  return new Intl.DateTimeFormat("pt-BR", {
    dateStyle: "short", timeStyle: "short",
  }).format(new Date(ms));
}

export function formatarPercentual(valor: number): string {
  return new Intl.NumberFormat("pt-BR", {
    minimumFractionDigits: 1, maximumFractionDigits: 1,
  }).format(valor) + "%";
}

/** Evita rotas externas/não autorizadas; utiliza o link_acao do Core, sem montar pelo id da etapa. */
export function rotaDeAcao(link: string): `/ordens/${string}` | `/orcamentos/${string}` | null {
  if (/^\/(ordens|orcamentos)\/[a-zA-Z0-9-]+\/?$/.test(link)) {
    return link as `/ordens/${string}` | `/orcamentos/${string}`;
  }
  return null;
}
