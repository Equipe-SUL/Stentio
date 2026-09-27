import React, { useState, useEffect } from "react";
import {
  View,
  Text,
  ScrollView,
  TouchableOpacity,
  ActivityIndicator,
  useWindowDimensions,
} from "react-native";
import { useRouter } from "expo-router";
interface DashboardMetrics {
  clientesAtivos: { valor: number; variacao: string };
  orcamentosPendentes: { valor: number; info: string };
  projetosAndamento: { valor: number; info: string };
  receitaMes: { valor: string; variacao: string };
}
interface Solicitacao {
  id: string;
  cliente: string;
  tipo: string;
  idiomas: string;
  status: "Aguardando Orçamento" | "Em Análise" | "Aprovado" | "Concluído";
  data: string;
}
interface UsoSistema {
  recursosAtivos: { atual: number; total: number };
  idiomasCadastrados: { atual: number; total: number };
}
export default function DashboardScreen() {
  const { width } = useWindowDimensions();
  const isMobile = width < 768;
  const isWide = width >= 1200;
  const router = useRouter();
  const [isLoading, setIsLoading] = useState(true);
  const [metrics, setMetrics] = useState<DashboardMetrics | null>(null);
  const [solicitacoes, setSolicitacoes] = useState<Solicitacao[]>([]);
  const [usoSistema, setUsoSistema] = useState<UsoSistema | null>(null);
  useEffect(() => {
    const fetchDashboardData = async () => {
      try {
        // TODO: Substituir por chamada real à API no futuro
        await new Promise((resolve) => setTimeout(resolve, 800));
        setMetrics({
          clientesAtivos: { valor: 142, variacao: "+8 este mês" },
          orcamentosPendentes: { valor: 23, info: "5 aguardam aprovação" },
          projetosAndamento: { valor: 37, info: "12 com prazo próximo" },
          receitaMes: { valor: "R$ 48.920", variacao: "+14% vs mês anterior" },
        });
        setSolicitacoes([
          {
            id: "SOL-2024-089",
            cliente: "Embraer S.A.",
            tipo: "Tradução Técnica",
            idiomas: "PT-BR ➔ EN-US",
            status: "Aguardando Orçamento",
            data: "18/09/2024",
          },
          {
            id: "SOL-2024-088",
            cliente: "Natura Cosméticos",
            tipo: "Revisão",
            idiomas: "EN-US ➔ ES-MX",
            status: "Em Análise",
            data: "17/09/2024",
          },
          {
            id: "SOL-2024-087",
            cliente: "Itaú Unibanco",
            tipo: "Localização",
            idiomas: "PT-BR ➔ ZH-CN",
            status: "Aprovado",
            data: "16/09/2024",
          },
        ]);
        setUsoSistema({
          recursosAtivos: { atual: 18, total: 25 },
          idiomasCadastrados: { atual: 12, total: 20 },
        });
      } catch (error) {
        console.error("Erro ao carregar dashboard:", error);
      } finally {
        setIsLoading(false);
      }
    };
    fetchDashboardData();
  }, []);
  const getStatusStyle = (status: Solicitacao["status"]) => {
    switch (status) {
      case "Aguardando Orçamento":
        return { bg: "bg-orange-100", text: "text-orange-700" };
      case "Em Análise":
        return { bg: "bg-blue-100", text: "text-blue-700" };
      case "Aprovado":
        return { bg: "bg-green-100", text: "text-green-700" };
      case "Concluído":
        return { bg: "bg-zinc-100", text: "text-zinc-700" };
      default:
        return { bg: "bg-zinc-100", text: "text-zinc-700" };
    }
  };
  if (isLoading || !metrics || !usoSistema) {
    return (
      <View className="flex-1 items-center justify-center bg-[#f4f4f5]">
        <ActivityIndicator size="large" color="#8c5230" />
      </View>
    );
  }
  return (
    <ScrollView
      style={{ flex: 1, minWidth: 0 }}
      contentContainerStyle={{ padding: isMobile ? 20 : 32, paddingBottom: 32 }}
      showsVerticalScrollIndicator={false}
      keyboardShouldPersistTaps="handled"
    >
      <View className="mb-8">
        <Text
          className="font-bold text-zinc-900 mb-1"
          style={{ fontSize: isMobile ? 24 : 30 }}
        >
          Painel Administrativo
        </Text>
        <Text className="text-zinc-500 text-sm">
          Visão geral operacional —{" "}
          {new Date().toLocaleDateString("pt-BR", {
            day: "numeric",
            month: "long",
            year: "numeric",
          })}
        </Text>
      </View>
      <View
        style={{
          flexDirection: "row",
          flexWrap: "wrap",
          gap: isMobile ? 12 : 16,
          marginBottom: 28,
        }}
      >
        <View
          style={{
            flexGrow: isMobile ? 0 : 1,
            flexBasis: isMobile ? "46%" : isWide ? "20%" : "45%",
            minWidth: 0,
            padding: isMobile ? 16 : 24,
          }}
          className="bg-white rounded-2xl border border-zinc-100 shadow-sm"
        >
          <View className="w-8 h-1 bg-[#8c5230] mb-4 rounded-full" />
          <Text
            className="font-bold text-zinc-900 mb-1"
            style={{ fontSize: isMobile ? 30 : 36 }}
          >
            {metrics.clientesAtivos.valor}
          </Text>
          <Text className="text-zinc-600 font-medium mb-1">
            Clientes Ativos
          </Text>
          <Text className="text-zinc-400 text-xs">
            {metrics.clientesAtivos.variacao}
          </Text>
        </View>
        <View
          style={{
            flexGrow: isMobile ? 0 : 1,
            flexBasis: isMobile ? "46%" : isWide ? "20%" : "45%",
            minWidth: 0,
            padding: isMobile ? 16 : 24,
          }}
          className="bg-white rounded-2xl border border-zinc-100 shadow-sm"
        >
          <View className="w-8 h-1 bg-[#8c5230] mb-4 rounded-full" />
          <Text
            className="font-bold text-zinc-900 mb-1"
            style={{ fontSize: isMobile ? 30 : 36 }}
          >
            {metrics.orcamentosPendentes.valor}
          </Text>
          <Text className="text-zinc-600 font-medium mb-1">
            Orçamentos Pendentes
          </Text>
          <Text className="text-[#8c5230] text-xs">
            {metrics.orcamentosPendentes.info}
          </Text>
        </View>
        <View
          style={{
            flexGrow: isMobile ? 0 : 1,
            flexBasis: isMobile ? "46%" : isWide ? "20%" : "45%",
            minWidth: 0,
            padding: isMobile ? 16 : 24,
          }}
          className="bg-white rounded-2xl border border-zinc-100 shadow-sm"
        >
          <View className="w-8 h-1 bg-[#8c5230] mb-4 rounded-full" />
          <Text
            className="font-bold text-zinc-900 mb-1"
            style={{ fontSize: isMobile ? 30 : 36 }}
          >
            {metrics.projetosAndamento.valor}
          </Text>
          <Text className="text-zinc-600 font-medium mb-1">
            Projetos em Andamento
          </Text>
          <Text className="text-zinc-400 text-xs">
            {metrics.projetosAndamento.info}
          </Text>
        </View>
        <View
          style={{
            flexGrow: isMobile ? 0 : 1,
            flexBasis: isMobile ? "46%" : isWide ? "20%" : "45%",
            minWidth: 0,
            padding: isMobile ? 16 : 24,
          }}
          className="bg-white rounded-2xl border border-zinc-100 shadow-sm"
        >
          <View className="w-8 h-1 bg-[#8c5230] mb-4 rounded-full" />
          <Text
            className="font-bold text-zinc-900 mb-1"
            style={{ fontSize: isMobile ? 30 : 36 }}
          >
            {metrics.receitaMes.valor}
          </Text>
          <Text className="text-zinc-600 font-medium mb-1">Receita do Mês</Text>
          <Text className="text-zinc-400 text-xs">
            {metrics.receitaMes.variacao}
          </Text>
        </View>
      </View>
      <View style={{ flexDirection: isWide ? "row" : "column", gap: 24 }}>
        <View
          style={{
            flex: isWide ? 2 : undefined,
            minWidth: 0,
            padding: isMobile ? 16 : 24,
          }}
          className="bg-white rounded-2xl border border-zinc-100 shadow-sm"
        >
          <View
            className="flex-row justify-between items-center mb-6"
            style={{ flexWrap: "wrap", gap: 8 }}
          >
            <Text className="text-lg font-bold text-zinc-900">
              Solicitações Recentes
            </Text>
            <TouchableOpacity
              accessibilityRole="button"
              onPress={() => router.navigate("/(admin)/solicitacoes")}
              style={{ minHeight: 44, justifyContent: "center" }}
            >
              <Text className="text-[#8c5230] font-medium">Ver todas ➔</Text>
            </TouchableOpacity>
          </View>
          {isMobile ? (
            solicitacoes.map((item) => {
              const statusStyle = getStatusStyle(item.status);
              return (
                <View key={item.id} className="border-b border-zinc-100 py-4">
                  <Text className="text-xs text-[#8c5230] mb-2">{item.id}</Text>
                  <Text className="text-base font-bold text-zinc-800 mb-1">
                    {item.cliente}
                  </Text>
                  <Text className="text-sm text-zinc-600 mb-1">
                    {item.tipo}
                  </Text>
                  <Text className="text-sm text-zinc-500 mb-3">
                    {item.idiomas}
                  </Text>
                  <View
                    style={{
                      flexDirection: "row",
                      flexWrap: "wrap",
                      alignItems: "center",
                      gap: 8,
                      justifyContent: "space-between",
                    }}
                  >
                    <View
                      className={`${statusStyle.bg} px-2 py-1 rounded self-start`}
                    >
                      <Text
                        className={`text-xs font-medium ${statusStyle.text}`}
                      >
                        {item.status}
                      </Text>
                    </View>
                    <Text className="text-xs text-zinc-500">{item.data}</Text>
                  </View>
                </View>
              );
            })
          ) : (
            <ScrollView horizontal showsHorizontalScrollIndicator>
              <View style={{ minWidth: 760, flexGrow: 1 }}>
                <View className="flex-row border-b border-zinc-100 pb-3 mb-3">
                  <Text className="flex-1 text-xs font-bold text-zinc-400">
                    ID
                  </Text>
                  <Text className="flex-[2] text-xs font-bold text-zinc-400">
                    CLIENTE
                  </Text>
                  <Text className="flex-[2] text-xs font-bold text-zinc-400">
                    TIPO
                  </Text>
                  <Text className="flex-[2] text-xs font-bold text-zinc-400">
                    PAR DE IDIOMAS
                  </Text>
                  <Text className="flex-[1.5] text-xs font-bold text-zinc-400">
                    STATUS
                  </Text>
                  <Text className="flex-1 text-xs font-bold text-zinc-400 text-right">
                    DATA
                  </Text>
                </View>
                {solicitacoes.map((item) => {
                  const statusStyle = getStatusStyle(item.status);
                  return (
                    <View
                      key={item.id}
                      className="flex-row items-center border-b border-zinc-50 py-4"
                    >
                      <Text className="flex-1 text-xs text-[#8c5230] font-medium">
                        {item.id}
                      </Text>
                      <Text className="flex-[2] text-sm font-bold text-zinc-800">
                        {item.cliente}
                      </Text>
                      <Text className="flex-[2] text-sm text-zinc-600">
                        {item.tipo}
                      </Text>
                      <Text className="flex-[2] text-sm text-zinc-600">
                        {item.idiomas}
                      </Text>
                      <View className="flex-[1.5]">
                        <View
                          className={`${statusStyle.bg} px-2 py-1 rounded self-start`}
                        >
                          <Text
                            className={`text-xs font-medium ${statusStyle.text}`}
                          >
                            {item.status}
                          </Text>
                        </View>
                      </View>
                      <Text className="flex-1 text-sm text-zinc-500 text-right">
                        {item.data}
                      </Text>
                    </View>
                  );
                })}
              </View>
            </ScrollView>
          )}
        </View>
        <View style={{ flex: isWide ? 1 : undefined, gap: 24 }}>
          <View className="bg-white rounded-2xl border border-zinc-100 p-6 shadow-sm">
            <Text className="text-xs font-bold text-zinc-400 mb-4 tracking-wider">
              AÇÕES RÁPIDAS
            </Text>
            <TouchableOpacity className="mb-4">
              <Text className="font-bold text-zinc-800">Nova Solicitação</Text>
              <Text className="text-sm text-zinc-400">
                Registrar pedido de serviço
              </Text>
            </TouchableOpacity>
            <TouchableOpacity className="mb-4">
              <Text className="font-bold text-zinc-800">Novo Orçamento</Text>
              <Text className="text-sm text-zinc-400">
                Elaborar proposta comercial
              </Text>
            </TouchableOpacity>
            <TouchableOpacity>
              <Text className="font-bold text-zinc-800">
                Gerenciar Usuários
              </Text>
              <Text className="text-sm text-zinc-400">Controle de acesso</Text>
            </TouchableOpacity>
          </View>
          <View className="bg-white rounded-2xl border border-zinc-100 p-6 shadow-sm">
            <Text className="text-xs font-bold text-zinc-400 mb-4 tracking-wider">
              USO DO SISTEMA
            </Text>
            <View className="mb-4">
              <View className="flex-row justify-between mb-1">
                <Text className="text-sm text-zinc-600 font-medium">
                  Recursos Ativos
                </Text>
                <Text className="text-sm text-zinc-400">
                  {usoSistema.recursosAtivos.atual}/
                  {usoSistema.recursosAtivos.total}
                </Text>
              </View>
              <View className="h-1.5 bg-zinc-100 rounded-full overflow-hidden">
                <View
                  className="h-full bg-[#8c5230]"
                  style={{
                    width: `${(usoSistema.recursosAtivos.atual / usoSistema.recursosAtivos.total) * 100}%`,
                  }}
                />
              </View>
            </View>
            <View className="mb-4">
              <View className="flex-row justify-between mb-1">
                <Text className="text-sm text-zinc-600 font-medium">
                  Idiomas Cadastrados
                </Text>
                <Text className="text-sm text-zinc-400">
                  {usoSistema.idiomasCadastrados.atual}/
                  {usoSistema.idiomasCadastrados.total}
                </Text>
              </View>
              <View className="h-1.5 bg-zinc-100 rounded-full overflow-hidden">
                <View
                  className="h-full bg-[#8c5230]"
                  style={{
                    width: `${(usoSistema.idiomasCadastrados.atual / usoSistema.idiomasCadastrados.total) * 100}%`,
                  }}
                />
              </View>
            </View>
          </View>
        </View>
      </View>
    </ScrollView>
  );
}
