import { useEffect, useState } from "react";
import { Ionicons } from "@expo/vector-icons";
import axios from "axios";
import {
  ActivityIndicator, Pressable, ScrollView, Text, View,
} from "react-native";
import { getApiErrorMessage } from "../../lib/api";
import { useSession } from "../../lib/session";
import { formatarPercentual } from "./dashboardUtils";
import { MetricCard } from "./MetricCard";
import { PendenciasList } from "./PendenciasList";
import { useDashboard } from "./useDashboard";

const INTERVALOS = [30, 60, 120] as const;

function mensagemDeErro(erro: unknown): string {
  if (axios.isAxiosError(erro)) {
    if (erro.response?.status === 401) return "Sua sessão expirou. Entre novamente para visualizar o painel.";
    if (erro.response?.status === 403) return "Seu perfil não tem permissão para consultar esses dados.";
  }
  return getApiErrorMessage(erro);
}

export function DashboardScreen() {
  const { usuario } = useSession();
  const [largura, setLargura] = useState(0);
  const [autoAtualizar, setAutoAtualizar] = useState(true);
  const [intervaloSegundos, setIntervaloSegundos] = useState<number>(60);
  const [agora, setAgora] = useState(Date.now());

  const {
    resumo, pendencias, atualizar, atualizando, ultimaAtualizacao, atualizacaoAtiva,
  } = useDashboard(usuario?.id ?? "", autoAtualizar, intervaloSegundos * 1000);

  useEffect(() => {
    if (!atualizacaoAtiva) return;
    const timer = setInterval(() => setAgora(Date.now()), 1000);
    return () => clearInterval(timer);
  }, [atualizacaoAtiva]);

  const conteudo = resumo.data;
  const totalPendentes = conteudo
    ? conteudo.total_orcamentos_em_elaboracao + conteudo.total_orcamentos_aguardando_aprovacao
    : undefined;
  const compacto = largura < 720;
  const gap = 14;
  const colunas = largura >= 920 ? 4 : largura >= 475 ? 2 : 1;
  const larguraCard = largura > 0
    ? Math.max(0, (largura - gap * (colunas - 1)) / colunas)
    : "100%";
  const taxaConversao = conteudo?.taxa_conversao_orcamentos;
  const porcentagemBarra = Math.min(100, Math.max(0, taxaConversao ?? 0));
  const proximaAtualizacao = Number.isFinite(ultimaAtualizacao)
    ? Math.max(0, Math.ceil((ultimaAtualizacao + intervaloSegundos * 1000 - agora) / 1000))
    : null;
  const dataUltimaAtualizacao = Number.isFinite(ultimaAtualizacao)
    ? new Intl.DateTimeFormat("pt-BR", { hour: "2-digit", minute: "2-digit", second: "2-digit" }).format(new Date(ultimaAtualizacao))
    : null;
  const erroResumo = resumo.isError ? mensagemDeErro(resumo.error) : undefined;
  const erroPendencias = pendencias.isError ? mensagemDeErro(pendencias.error) : undefined;

  return (
    <ScrollView
      className="flex-1 bg-[#f8f7f5]"
      keyboardShouldPersistTaps="handled"
      contentContainerStyle={{ flexGrow: 1, paddingBottom: 48 }}
    >
      <View
        className="mx-auto w-full max-w-6xl gap-6 px-4 py-7 md:px-8 md:py-9"
        onLayout={e => setLargura(e.nativeEvent.layout.width - (e.nativeEvent.layout.width >= 768 ? 64 : 32))}
      >
        {/* Título e status, já dentro do AppShell existente do Stentio. */}
        <View className="gap-4">
          <View className="flex-row flex-wrap items-center gap-2">
            <Text className="text-[11px] font-bold tracking-widest text-[#966942]">PAINEL</Text>
            <Ionicons name="chevron-forward" size={12} color="#b8aba0" />
            <Text className="text-[11px] font-semibold tracking-widest text-[#81766b]">VISÃO GERAL</Text>
          </View>
          <View className="flex-row flex-wrap items-end justify-between gap-5">
            <View className="gap-2" style={{ minWidth: 220, flex: 1 }}>
              <Text className="text-3xl font-semibold tracking-tight text-[#2e241d] md:text-4xl">
                Dashboard
              </Text>
              <Text className="max-w-xl text-sm leading-6 text-[#81766b]">
                Acompanhe os indicadores e as pendências operacionais em um só lugar.
              </Text>
              <View className="flex-row flex-wrap items-center gap-2">
                <Ionicons name="shield-checkmark-outline" size={14} color="#8a765d" />
                <Text className="text-xs text-[#8a765d]">
                  Visão por perfil: {usuario?.role === "GESTOR_PROJETO" ? "Gestor de Projetos" : "Administrador"}
                </Text>
              </View>
            </View>

            <View className="items-start gap-3" style={{ minWidth: compacto ? 0 : 300 }}>
              <View className="flex-row flex-wrap items-center gap-2">
                <Pressable
                  accessibilityRole="switch"
                  accessibilityState={{ checked: autoAtualizar }}
                  accessibilityLabel="Atualização automática"
                  onPress={() => setAutoAtualizar(v => !v)}
                  className="flex-row items-center gap-2 rounded-xl border border-[#e5ded6] bg-white px-3 py-2.5"
                >
                  <View
                    className="h-[18px] w-8 justify-center rounded-full px-0.5"
                    style={{ backgroundColor: autoAtualizar ? "#745331" : "#c8c4c0" }}
                  >
                    <View
                      className="h-3.5 w-3.5 rounded-full bg-white"
                      style={{ alignSelf: autoAtualizar ? "flex-end" : "flex-start" }}
                    />
                  </View>
                  <Text className="text-xs font-semibold text-[#685d52]">Automática</Text>
                </Pressable>
                <Pressable
                  accessibilityRole="button"
                  accessibilityLabel={`Intervalo de atualização: ${intervaloSegundos} segundos. Toque para alterar.`}
                  onPress={() => setIntervaloSegundos(valor => INTERVALOS[(INTERVALOS.indexOf(valor as typeof INTERVALOS[number]) + 1) % INTERVALOS.length])}
                  className="flex-row items-center gap-2 rounded-xl border border-[#e5ded6] bg-white px-3 py-2.5"
                >
                  <Ionicons name="time-outline" size={14} color="#826b53" />
                  <Text className="text-xs font-semibold text-[#65594f]">{intervaloSegundos} segundos</Text>
                  <Ionicons name="chevron-down" size={12} color="#8e8379" />
                </Pressable>
                <Pressable
                  accessibilityRole="button"
                  accessibilityLabel="Atualizar indicadores e pendências agora"
                  disabled={atualizando}
                  onPress={() => { void atualizar(); }}
                  className="flex-row items-center gap-2 rounded-xl bg-[#70502e] px-4 py-3 active:bg-[#553b21]"
                  style={{ opacity: atualizando ? 0.65 : 1 }}
                >
                  {atualizando ? (
                    <ActivityIndicator size="small" color="#ffffff" />
                  ) : (
                    <Ionicons name="refresh-outline" size={16} color="#ffffff" />
                  )}
                  <Text className="text-xs font-semibold text-white">Atualizar</Text>
                </Pressable>
              </View>
              <View className="flex-row flex-wrap items-center gap-2">
                <View className="h-2 w-2 rounded-full" style={{ backgroundColor: atualizando ? "#d0a05c" : "#3c9f79" }} />
                <Text className="text-[11px] text-[#85796d]">
                  {atualizando ? "Buscando dados..." : dataUltimaAtualizacao ? `Atualizado às ${dataUltimaAtualizacao}` : "Aguardando primeira atualização"}
                  {autoAtualizar
                    ? atualizacaoAtiva && proximaAtualizacao !== null
                      ? `  ·  Próxima em ${proximaAtualizacao}s`
                      : "  ·  Auto-refresh pausado"
                    : "  ·  Auto-refresh desligado"}
                </Text>
              </View>
            </View>
          </View>
        </View>

        {/* CA.1: 4 contadores superiores. Dados do contrato resumo. */}
        <View className="flex-row flex-wrap" style={{ gap }}>
          <MetricCard
            largura={larguraCard}
            titulo="Orçamentos em elaboração"
            descricao="Propostas sendo preparadas"
            valor={conteudo?.total_orcamentos_em_elaboracao}
            icone="document-text-outline" cor="#76553a" fundoIcone="#f4eee8"
          />
          <MetricCard
            largura={larguraCard}
            titulo="Aguardando aprovação"
            descricao="Aguardando retorno do cliente"
            valor={conteudo?.total_orcamentos_aguardando_aprovacao}
            icone="time-outline" cor="#b28d39" fundoIcone="#fbf3dc"
          />
          <MetricCard
            largura={larguraCard}
            titulo="OS em andamento"
            descricao="Ordens de serviço em execução"
            valor={conteudo?.total_os_em_andamento}
            icone="layers-outline" cor="#557cb2" fundoIcone="#ebf1fa"
          />
          <MetricCard
            largura={larguraCard}
            titulo="Etapas atrasadas"
            descricao="Precisam de atenção imediata"
            valor={conteudo?.total_etapas_atrasadas}
            icone="alert-circle-outline" cor="#b75d54" fundoIcone="#fcebea"
          />
        </View>

        {erroResumo ? (
          <View className="flex-row flex-wrap items-center gap-3 rounded-xl border border-[#f3d1ca] bg-[#fff1ed] px-4 py-3">
            <Ionicons name="alert-circle-outline" size={18} color="#b34c3e" />
            <Text className="flex-1 text-xs text-[#9f493e]">Indicadores: {erroResumo}</Text>
            <Pressable onPress={() => { void atualizar(); }} accessibilityRole="button">
              <Text className="text-xs font-bold text-[#8a3f32]">Tentar novamente</Text>
            </Pressable>
          </View>
        ) : null}

        {/* Resumo de orçamentos e taxa de conversão: não há meta histórica no contrato,
            então NÃO inventamos tendência ou meta de 60% vistas no protótipo. */}
        <View
           style={{
            flexDirection: compacto ? "column" : "row",
            flexWrap: compacto ? "nowrap" : "wrap",
            gap,
          }}
>
          <View
          className="justify-center gap-2 rounded-2xl border border-[#e8e2db] bg-[#fffdfb] px-5 py-5"
          style={{
          width: compacto ? "100%" : undefined,
          flexGrow: compacto ? 0 : 1,
          flexShrink: compacto ? 0 : 1,
          minWidth: compacto ? 0 : 220,
          }}
>
            <View className="flex-row items-center gap-2">
              <Ionicons name="receipt-outline" size={17} color="#86623e" />
              <Text className="text-xs font-semibold tracking-wide text-[#8a7460]">ORÇAMENTOS PENDENTES</Text>
            </View>
            <View className="flex-row items-end gap-3">
              <Text className="text-3xl font-semibold text-[#342820]">{totalPendentes ?? "—"}</Text>
              <Text className="mb-1 flex-1 text-xs leading-5 text-[#82766b]">
                {conteudo ? `${conteudo.total_orcamentos_em_elaboracao} em elaboração e ${conteudo.total_orcamentos_aguardando_aprovacao} aguardando aprovação` : "Resumo consolidado"}
              </Text>
            </View>
          </View>
          <View
           className="gap-4 rounded-2xl border border-[#e8e2db] bg-white px-5 py-5"
            style={{
             width: compacto ? "100%" : undefined,
               flexGrow: compacto ? 0 : 1,
                 flexShrink: compacto ? 0 : 1,
                  minWidth: compacto ? 0 : 270,
                     }}
>
            <View className="flex-row items-center gap-2">
              <Ionicons name="trending-up-outline" size={17} color="#86623e" />
              <Text className="text-xs font-semibold tracking-wide text-[#8a7460]">TAXA DE CONVERSÃO</Text>
            </View>
            <View className="flex-row flex-wrap items-center gap-5">
              <Text className="text-4xl font-semibold text-[#6f4e2d]">
                {taxaConversao === undefined ? "—" : formatarPercentual(taxaConversao)}
              </Text>
              <View className="min-w-[130px] flex-1 gap-2">
                <View className="h-2.5 overflow-hidden rounded-full bg-[#ece8e2]">
                  <View className="h-full rounded-full bg-[#775633]" style={{ width: `${porcentagemBarra}%` }} />
                </View>
                <Text className="text-[11px] text-[#8d8177]">Orçamentos aprovados entre os concluídos</Text>
              </View>
            </View>
          </View>
        </View>

        {/* CA.2: filtros de lista, prazo, estados e navegação pelo link_acao. */}
        <PendenciasList
          dados={pendencias.data}
          carregando={pendencias.isLoading}
          erro={erroPendencias}
          onTentarNovamente={() => { void atualizar(); }}
        />
      </View>
    </ScrollView>
  );
}
