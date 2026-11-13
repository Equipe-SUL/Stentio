import { coreApi } from "../../lib/api";
import type { DashboardPendencias, DashboardResumo } from "./types";

const DASHBOARD_URL = "/api/v1/dashboard";

/** Retorna os indicadores agregados autorizados para a sessão atual. */
export async function buscarResumo(): Promise<DashboardResumo> {
  const { data } = await coreApi.get<DashboardResumo>(`${DASHBOARD_URL}/resumo`);
  return data;
}

/** Retorna as pendências priorizadas pelo serviço Core. */
export async function buscarPendencias(): Promise<DashboardPendencias> {
  const { data } = await coreApi.get<DashboardPendencias>(`${DASHBOARD_URL}/pendencias`);
  return data;
}
