import { coreApi } from "./api";
import type { FiltrosLogsAuditoria, PaginaLogsAuditoria } from "../components/auditoria/types";

const CAMINHO_AUDITORIA = "/api/v1/auditoria/logs";

/**
 * Busca paginada e somente-leitura dos registros de alteração de status.
 * O Core aplica os filtros, ordena do mais recente ao mais antigo e valida
 * a permissão ADMIN. Reutiliza a sessão do coreApi (cookie/Bearer).
 */
export async function listarLogsAuditoria(
  filtros: FiltrosLogsAuditoria = {},
): Promise<PaginaLogsAuditoria> {
  const { data } = await coreApi.get<PaginaLogsAuditoria>(CAMINHO_AUDITORIA, {
    params: filtros,
  });
  return data;
}
