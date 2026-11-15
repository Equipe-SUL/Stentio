import { useQuery } from "@tanstack/react-query";
import { buscarResumo, buscarPendencias } from "./dashboardApi";

/** O cache por usuário evita mostrar dados de outra sessão. */
export function useDashboard(usuarioId: string) {
  const resumo = useQuery({
    queryKey: ["dashboard", usuarioId, "resumo"],
    queryFn: buscarResumo,
    enabled: Boolean(usuarioId),
    retry: 1,
  });

  const pendencias = useQuery({
    queryKey: ["dashboard", usuarioId, "pendencias"],
    queryFn: buscarPendencias,
    enabled: Boolean(usuarioId),
    retry: 1,
  });

  return { resumo, pendencias };
}
