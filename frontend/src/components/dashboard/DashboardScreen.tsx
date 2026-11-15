import { Ionicons } from "@expo/vector-icons";
import axios from "axios";
import { ActivityIndicator, ScrollView, Text, View } from "react-native";
import { getApiErrorMessage } from "../../lib/api";
import { useSession } from "../../lib/session";
import { MetricCard } from "./MetricCard";
import { useDashboard } from "./useDashboard";

function mensagemErro(erro: unknown): string {
  if (axios.isAxiosError(erro) && erro.response?.status === 401) {
    return "Sua sessão expirou. Faça login novamente.";
  }
  if (axios.isAxiosError(erro) && erro.response?.status === 403) {
    return "Seu perfil não pode visualizar estes indicadores.";
  }
  return getApiErrorMessage(erro);
}

export function DashboardScreen() {
  const { usuario } = useSession();
  const { resumo } = useDashboard(usuario?.id ?? "");
  const dados = resumo.data;

  return (
    <ScrollView className="flex-1 bg-[#f8f7f5]" contentContainerStyle={{ padding: 24, paddingBottom: 48 }}>
      <View className="mx-auto w-full max-w-6xl gap-6">
        <View className="gap-2">
          <Text className="text-xs font-bold tracking-widest text-[#97734d]">PAINEL · VISÃO GERAL</Text>
          <Text className="text-3xl font-semibold text-[#2D1A11]">Dashboard</Text>
          <Text className="text-sm text-[#7a7067]">Acompanhe as métricas operacionais em tempo real.</Text>
          <View className="flex-row items-center gap-2">
            <Ionicons name="shield-checkmark-outline" size={15} color="#745331" />
            <Text className="text-xs text-[#745331]">
              Perfil: {usuario?.role === "GESTOR_PROJETO" ? "Gestor de Projetos" : "Administrador"}
            </Text>
          </View>
        </View>
        {resumo.isLoading && !dados ? (
          <View className="flex-row items-center gap-2"><ActivityIndicator color="#745331" /><Text>Carregando indicadores...</Text></View>
        ) : null}
        {resumo.isError ? (
          <View className="rounded-xl border border-red-200 bg-red-50 p-4">
            <Text className="text-sm text-red-700">{mensagemErro(resumo.error)}</Text>
          </View>
        ) : null}
        <View className="flex-row flex-wrap gap-4">
          <MetricCard titulo="Orçamentos em elaboração" descricao="Propostas sendo preparadas" valor={dados?.total_orcamentos_em_elaboracao} icone="document-text-outline" cor="#76553a" fundoIcone="#f4eee8" largura={230} />
          <MetricCard titulo="Aguardando aprovação" descricao="Aguardando retorno do cliente" valor={dados?.total_orcamentos_aguardando_aprovacao} icone="time-outline" cor="#b28d39" fundoIcone="#fbf3dc" largura={230} />
          <MetricCard titulo="OS em andamento" descricao="Ordens de serviço em execução" valor={dados?.total_os_em_andamento} icone="layers-outline" cor="#557cb2" fundoIcone="#ebf1fa" largura={230} />
          <MetricCard titulo="Etapas atrasadas" descricao="Precisam de atenção imediata" valor={dados?.total_etapas_atrasadas} icone="alert-circle-outline" cor="#b75d54" fundoIcone="#fcebea" largura={230} />
        </View>
        <View className="rounded-2xl border border-[#e8e2db] bg-white p-5">
          <Text className="text-xs font-bold tracking-wider text-[#8a7460]">ORÇAMENTOS PENDENTES</Text>
          <Text className="mt-2 text-3xl font-semibold text-[#2e241d]">
            {dados ? dados.total_orcamentos_em_elaboracao + dados.total_orcamentos_aguardando_aprovacao : "—"}
          </Text>
          <Text className="mt-3 text-xs font-semibold tracking-wider text-[#8a7460]">TAXA DE CONVERSÃO</Text>
          <Text className="mt-1 text-3xl font-semibold text-[#6f4e2d]">
            {dados ? `${dados.taxa_conversao_orcamentos.toLocaleString("pt-BR", { minimumFractionDigits: 1, maximumFractionDigits: 1 })}%` : "—"}
          </Text>
        </View>
      </View>
    </ScrollView>
  );
}
