import { useQuery } from "@tanstack/react-query";
import { buscarResumo } from "./dashboardApi";

/** O cache por usuário evita mostrar dados de outra sessão. */
export function useDashboard(usuarioId: string) {
  const resumo = useQuery({
    queryKey: ["dashboard", usuarioId, "resumo"],
    queryFn: buscarResumo,
    enabled: Boolean(usuarioId),
    retry: 1,
  });

  return { resumo };
}
