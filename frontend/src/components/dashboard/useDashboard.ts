import { useCallback, useEffect, useState } from "react";
import { AppState, Platform } from "react-native";
import { useFocusEffect } from "expo-router";
import { useQuery } from "@tanstack/react-query";
import { buscarPendencias, buscarResumo } from "./dashboardApi";

/**
 * Um hook para os dois endpoints. Com react-query não se misturam estados de
 * erros, não se duplicam chamadas em componentes e o refresh é coordenado.
 * Atualização suspensa quando a página perde foco ou o app vai para background.
 */
export function useDashboard(usuarioId: string, autoAtualizar: boolean, intervaloMs: number) {
  const [focada, setFocada] = useState(false);
  const [visivel, setVisivel] = useState(true);
  const [atualizandoManual, setAtualizandoManual] = useState(false);

  useFocusEffect(useCallback(() => {
    setFocada(true);
    return () => setFocada(false);
  }, []));

  useEffect(() => {
    const assinatura = AppState.addEventListener("change", state => {
      setVisivel(state === "active");
    });

    // No browser, abas ocultas também devem suspender o polling.
    if (Platform.OS === "web" && typeof document !== "undefined") {
      const aoMudarVisibilidade = () => setVisivel(!document.hidden);
      document.addEventListener("visibilitychange", aoMudarVisibilidade);
      aoMudarVisibilidade();
      return () => {
        assinatura.remove();
        document.removeEventListener("visibilitychange", aoMudarVisibilidade);
      };
    }
    return () => assinatura.remove();
  }, []);

  const intervalo = autoAtualizar && focada && visivel ? intervaloMs : false;

  const resumo = useQuery({
    queryKey: ["dashboard", usuarioId, "resumo"],
    enabled: Boolean(usuarioId),
    queryFn: buscarResumo,
    refetchInterval: intervalo,
    refetchOnWindowFocus: false,
    staleTime: 20_000,
    retry: 1,
  });

  const pendencias = useQuery({
    queryKey: ["dashboard", usuarioId, "pendencias"],
    enabled: Boolean(usuarioId),
    queryFn: buscarPendencias,
    refetchInterval: intervalo,
    refetchOnWindowFocus: false,
    staleTime: 20_000,
    retry: 1,
  });

  const atualizar = useCallback(async () => {
    setAtualizandoManual(true);
    try {
      await Promise.all([resumo.refetch(), pendencias.refetch()]);
    } finally {
      setAtualizandoManual(false);
    }
  }, [resumo.refetch, pendencias.refetch]);

  return {
    resumo,
    pendencias,
    atualizar,
    atualizando: atualizandoManual || resumo.isFetching || pendencias.isFetching,
    ultimaAtualizacao: Math.min(
      resumo.dataUpdatedAt || Number.POSITIVE_INFINITY,
      pendencias.dataUpdatedAt || Number.POSITIVE_INFINITY,
    ),
    atualizacaoAtiva: Boolean(intervalo),
  };
}
