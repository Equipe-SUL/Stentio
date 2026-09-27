import React, { useState, useEffect, useMemo } from "react";
import {
  View,
  Text,
  ScrollView,
  TouchableOpacity,
  Modal,
  TextInput,
  ActivityIndicator,
  useWindowDimensions,
  KeyboardAvoidingView,
  Platform,
} from "react-native";
import { Ionicons } from "@expo/vector-icons";
import { Picker } from "@react-native-picker/picker";
import { useSafeAreaInsets } from "react-native-safe-area-context";
type StatusSolicitacao =
  | "Aguardando Orçamento"
  | "Em Análise"
  | "Aprovado"
  | "Em Andamento"
  | "Concluído"
  | "Cancelado";
interface Solicitacao {
  id: string;
  cliente: string;
  tipo: string;
  idiomaOrigem: string;
  idiomaDestino: string;
  status: StatusSolicitacao;
  data: string;
}
const FILTROS: ("Todos" | StatusSolicitacao)[] = [
  "Todos",
  "Aguardando Orçamento",
  "Em Análise",
  "Aprovado",
  "Em Andamento",
  "Concluído",
  "Cancelado",
];
export default function SolicitacoesScreen() {
  const { width } = useWindowDimensions();
  const insets = useSafeAreaInsets();
  const isMobile = width < 768;

  const [isLoading, setIsLoading] = useState(true);
  const [solicitacoes, setSolicitacoes] = useState<Solicitacao[]>([]);
  const [filtroAtivo, setFiltroAtivo] = useState<"Todos" | StatusSolicitacao>(
    "Todos",
  );
  const [isModalVisible, setIsModalVisible] = useState(false);
  // Estados do Form do Modal
  const [novoCliente, setNovoCliente] = useState("");
  const [novoTipoServico, setNovoTipoServico] = useState("Tradução Técnica");
  const [novoIdiomaOrigem, setNovoIdiomaOrigem] = useState("PT-BR");
  const [novoIdiomaDestino, setNovoIdiomaDestino] = useState("EN-US");
  const [novaObservacao, setNovaObservacao] = useState("");
  // Listas simuladas para os selects (no futuro virão da API)
  const tiposServicoDisponiveis = [
    "Tradução Técnica",
    "Revisão",
    "Localização",
    "Tradução Jurídica",
    "Tradução",
    "Interpretação",
  ];
  const idiomasDisponiveis = [
    "PT-BR",
    "EN-US",
    "ES-MX",
    "ZH-CN",
    "DE-DE",
    "FR-FR",
    "JA-JP",
  ];
  useEffect(() => {
    const fetchSolicitacoes = async () => {
      try {
        await new Promise((resolve) => setTimeout(resolve, 800));
        const mockData: Solicitacao[] = [
          {
            id: "SOL-2024-089",
            cliente: "Embraer S.A.",
            tipo: "Tradução Técnica",
            idiomaOrigem: "PT-BR",
            idiomaDestino: "EN-US",
            status: "Aguardando Orçamento",
            data: "18/09/2024",
          },
          {
            id: "SOL-2024-088",
            cliente: "Natura Cosméticos",
            tipo: "Revisão",
            idiomaOrigem: "EN-US",
            idiomaDestino: "ES-MX",
            status: "Em Análise",
            data: "17/09/2024",
          },
          {
            id: "SOL-2024-087",
            cliente: "Itaú Unibanco",
            tipo: "Localização",
            idiomaOrigem: "PT-BR",
            idiomaDestino: "ZH-CN",
            status: "Aprovado",
            data: "16/09/2024",
          },
          {
            id: "SOL-2024-086",
            cliente: "Totvs S.A.",
            tipo: "Tradução Jurídica",
            idiomaOrigem: "PT-BR",
            idiomaDestino: "DE-DE",
            status: "Concluído",
            data: "15/09/2024",
          },
          {
            id: "SOL-2024-085",
            cliente: "Rafael Mendonça",
            tipo: "Tradução",
            idiomaOrigem: "EN-US",
            idiomaDestino: "PT-BR",
            status: "Em Andamento",
            data: "14/09/2024",
          },
        ];
        setSolicitacoes(mockData);
      } catch (error) {
        console.error(error);
      } finally {
        setIsLoading(false);
      }
    };
    fetchSolicitacoes();
  }, []);
  const solicitacoesFiltradas = useMemo(() => {
    if (filtroAtivo === "Todos") return solicitacoes;
    return solicitacoes.filter((s) => s.status === filtroAtivo);
  }, [solicitacoes, filtroAtivo]);
  const getStatusStyle = (status: StatusSolicitacao) => {
    switch (status) {
      case "Aguardando Orçamento":
        return "bg-amber-100 text-amber-700";
      case "Em Análise":
        return "bg-blue-100 text-blue-700";
      case "Aprovado":
        return "bg-emerald-100 text-emerald-700";
      case "Em Andamento":
        return "bg-indigo-100 text-indigo-700";
      case "Concluído":
        return "bg-zinc-100 text-zinc-600";
      case "Cancelado":
        return "bg-red-100 text-red-700";
      default:
        return "bg-zinc-100 text-zinc-600";
    }
  };
  const handleCloseModal = () => {
    setIsModalVisible(false);
    setNovoCliente("");
    setNovoTipoServico("Tradução Técnica");
    setNovoIdiomaOrigem("PT-BR");
    setNovoIdiomaDestino("EN-US");
    setNovaObservacao("");
  };
  const handleRegistrar = () => {
    if (!novoCliente) return;
    const novaReq: Solicitacao = {
      id: `SOL-2024-${Math.floor(Math.random() * 900) + 100}`,
      cliente: novoCliente,
      tipo: novoTipoServico,
      idiomaOrigem: novoIdiomaOrigem,
      idiomaDestino: novoIdiomaDestino,
      status: "Aguardando Orçamento",
      data: new Date().toLocaleDateString("pt-BR"),
    };
    setSolicitacoes([novaReq, ...solicitacoes]);
    handleCloseModal();
  };
  if (isLoading) {
    return (
      <View className="flex-1 items-center justify-center bg-[#f4f4f5]">
        <ActivityIndicator size="large" color="#8c5230" />
      </View>
    );
  }
  return (
    <View className="flex-1 bg-[#f4f4f5]" style={{ minWidth: 0 }}>
      <ScrollView
        style={{ flex: 1 }}
        contentContainerStyle={{
          padding: isMobile ? 16 : 32,
          paddingBottom: 32,
        }}
        showsVerticalScrollIndicator={false}
        keyboardShouldPersistTaps="handled"
      >
        {/* Header */}
        <View
          style={{
            flexDirection: isMobile ? "column" : "row",
            flexWrap: "wrap",
            justifyContent: "space-between",
            alignItems: isMobile ? "stretch" : "center",
            gap: 16,
            marginBottom: 24,
          }}
        >
          <View style={{ flexShrink: 1, minWidth: 0 }}>
            <Text
              className="font-bold text-zinc-900 mb-1"
              style={{ fontSize: isMobile ? 26 : 30 }}
            >
              Solicitações
            </Text>
            <Text className="text-zinc-500 text-sm">
              Registros de solicitações de serviço dos clientes
            </Text>
          </View>
          <TouchableOpacity
            onPress={() => setIsModalVisible(true)}
            accessibilityRole="button"
            accessibilityLabel="Nova solicitação"
            style={{ minHeight: 48, justifyContent: "center" }}
            className="bg-[#a05a3c] px-5 py-3 rounded-lg flex-row items-center hover:bg-[#8c5230] transition-colors shadow-sm"
          >
            <Text className="text-white font-bold text-sm">
              + Nova Solicitação
            </Text>
          </TouchableOpacity>
        </View>
        {/* Filtros em Abas (Pills) */}
        <ScrollView
          horizontal={isMobile}
          showsHorizontalScrollIndicator={false}
          style={{ flexGrow: 0, marginBottom: 24 }}
          contentContainerStyle={{
            flexDirection: "row",
            flexWrap: isMobile ? "nowrap" : "wrap",
            gap: 8,
          }}
        >
          {FILTROS.map((filtro) => {
            const isAtivo = filtroAtivo === filtro;
            return (
              <TouchableOpacity
                key={filtro}
                onPress={() => setFiltroAtivo(filtro)}
                accessibilityRole="button"
                accessibilityState={{ selected: isAtivo }}
                style={{ minHeight: 44, justifyContent: "center" }}
                className={`px-4 py-2 rounded-full border transition-colors ${
                  isAtivo
                    ? "bg-[#a05a3c] border-[#a05a3c]"
                    : "bg-white border-zinc-200 hover:border-[#a05a3c]/40"
                }`}
              >
                <Text
                  className={`text-sm font-medium ${isAtivo ? "text-white" : "text-zinc-600"}`}
                >
                  {filtro}
                </Text>
              </TouchableOpacity>
            );
          })}
        </ScrollView>
        {isMobile ? (
          <View style={{ gap: 12 }}>
            {solicitacoesFiltradas.map((item) => (
              <View
                key={item.id}
                className="bg-white border border-zinc-100 rounded-2xl shadow-sm"
                style={{ padding: 16, minWidth: 0 }}
              >
                <View
                  style={{
                    flexDirection: "row",
                    flexWrap: "wrap",
                    justifyContent: "space-between",
                    gap: 8,
                    marginBottom: 12,
                  }}
                >
                  <Text className="text-xs font-semibold text-[#a05a3c]">
                    {item.id}
                  </Text>
                  <Text className="text-xs text-zinc-400">{item.data}</Text>
                </View>
                <Text
                  className="text-lg font-bold text-zinc-800"
                  style={{ marginBottom: 4 }}
                >
                  {item.cliente}
                </Text>
                <Text
                  className="text-sm text-zinc-600"
                  style={{ marginBottom: 12 }}
                >
                  {item.tipo}
                </Text>
                <View
                  style={{
                    flexDirection: "row",
                    alignItems: "center",
                    flexWrap: "wrap",
                    gap: 8,
                    marginBottom: 16,
                  }}
                >
                  <Ionicons name="language-outline" size={18} color="#71717a" />
                  <Text className="text-sm text-zinc-600">
                    {item.idiomaOrigem} → {item.idiomaDestino}
                  </Text>
                </View>
                <View
                  style={{
                    flexDirection: "row",
                    flexWrap: "wrap",
                    alignItems: "center",
                    justifyContent: "space-between",
                    gap: 12,
                  }}
                >
                  <View
                    className={`${getStatusStyle(item.status).split(" ")[0]} rounded-full`}
                    style={{
                      paddingHorizontal: 12,
                      paddingVertical: 6,
                      flexShrink: 1,
                    }}
                  >
                    <Text
                      className={`text-xs font-semibold ${getStatusStyle(item.status).split(" ")[1]}`}
                    >
                      {item.status}
                    </Text>
                  </View>
                  {/* Mantém o botão original; a ação de detalhes ainda não foi implementada. */}
                  <TouchableOpacity
                    accessibilityRole="button"
                    accessibilityLabel={`Ver solicitação ${item.id}`}
                    className="border border-zinc-200 rounded-xl"
                    style={{
                      minHeight: 44,
                      minWidth: 56,
                      alignItems: "center",
                      justifyContent: "center",
                      paddingHorizontal: 14,
                    }}
                  >
                    <Text className="text-sm font-semibold text-[#a05a3c]">
                      Ver
                    </Text>
                  </TouchableOpacity>
                </View>
              </View>
            ))}
            {solicitacoesFiltradas.length === 0 && (
              <View
                className="bg-white rounded-2xl border border-zinc-100"
                style={{ padding: 24, alignItems: "center", gap: 12 }}
              >
                <Ionicons
                  name="document-text-outline"
                  size={32}
                  color="#a1a1aa"
                />
                <Text className="text-sm text-zinc-500 text-center">
                  Nenhuma solicitação encontrada para este filtro.
                </Text>
              </View>
            )}
          </View>
        ) : (
          <ScrollView
            horizontal
            showsHorizontalScrollIndicator
            contentContainerStyle={{ flexGrow: 1 }}
          >
            <View
              className="bg-white border border-zinc-100 rounded-2xl shadow-sm overflow-hidden"
              style={{ minWidth: 1040, flex: 1 }}
            >
              {/* Cabeçalho da Tabela */}
              <View className="flex-row items-center border-b border-zinc-100 p-6">
                <Text className="flex-[0.8] text-xs font-bold text-zinc-400">
                  ID
                </Text>
                <Text className="flex-[1.5] text-xs font-bold text-zinc-400">
                  Cliente
                </Text>
                <Text className="flex-[1.5] text-xs font-bold text-zinc-400">
                  Tipo
                </Text>
                <Text className="flex-[1.2] text-xs font-bold text-zinc-400">
                  Par de Idiomas
                </Text>
                <Text className="flex-[1.5] text-xs font-bold text-zinc-400">
                  Status
                </Text>
                <Text className="flex-[0.8] text-xs font-bold text-zinc-400">
                  Data
                </Text>
                <Text className="w-16 text-xs font-bold text-zinc-400 text-center"></Text>
              </View>
              {/* Corpo da Tabela */}
              {solicitacoesFiltradas.map((item, index) => (
                <View
                  key={item.id}
                  className={`flex-row items-center p-6 ${index !== solicitacoesFiltradas.length - 1 ? "border-b border-zinc-50" : ""}`}
                >
                  <Text className="flex-[0.8] text-xs font-medium text-[#a05a3c]">
                    {item.id}
                  </Text>
                  <Text className="flex-[1.5] text-sm font-bold text-zinc-800">
                    {item.cliente}
                  </Text>
                  <Text className="flex-[1.5] text-sm text-zinc-600">
                    {item.tipo}
                  </Text>
                  <Text className="flex-[1.2] text-sm text-zinc-500 uppercase tracking-wide">
                    {item.idiomaOrigem} ➔ {item.idiomaDestino}
                  </Text>
                  <View className="flex-[1.5]">
                    <View
                      className={`${getStatusStyle(item.status)} px-3 py-1 rounded-full self-start`}
                    >
                      <Text
                        className={`text-xs font-medium ${getStatusStyle(item.status).split(" ")[1]}`}
                      >
                        {item.status}
                      </Text>
                    </View>
                  </View>
                  <Text className="flex-[0.8] text-sm text-zinc-400">
                    {item.data}
                  </Text>
                  <View className="w-16 items-end">
                    <TouchableOpacity className="border border-zinc-300 px-3 py-1.5 rounded-lg hover:bg-zinc-50 transition-colors">
                      <Text className="text-xs font-medium text-zinc-600">
                        Ver
                      </Text>
                    </TouchableOpacity>
                  </View>
                </View>
              ))}
              {solicitacoesFiltradas.length === 0 && (
                <View className="p-10 items-center justify-center">
                  <Text className="text-zinc-500 text-sm">
                    Nenhuma solicitação encontrada para este filtro.
                  </Text>
                </View>
              )}
            </View>
          </ScrollView>
        )}
      </ScrollView>
      {/* Modal de Nova Solicitação */}
      <Modal
        visible={isModalVisible}
        transparent={true}
        animationType="fade"
        onRequestClose={handleCloseModal}
      >
        <KeyboardAvoidingView
          behavior={
            Platform.OS === "ios"
              ? "padding"
              : Platform.OS === "android"
                ? "height"
                : undefined
          }
          style={{ flex: 1, backgroundColor: "rgba(0,0,0,0.4)" }}
        >
          <View
            style={{
              flex: 1,
              justifyContent: "center",
              alignItems: "center",
              paddingTop: insets.top + 12,
              paddingBottom: insets.bottom + 12,
              paddingLeft: insets.left + 12,
              paddingRight: insets.right + 12,
            }}
          >
            <View
              className="bg-white rounded-2xl shadow-xl overflow-hidden"
              style={{
                width: "100%",
                maxWidth: 520,
                maxHeight: "100%",
                flexShrink: 1,
              }}
            >
              <View
                className="flex-row justify-between items-center border-b border-zinc-100"
                style={{
                  paddingHorizontal: isMobile ? 16 : 24,
                  paddingVertical: 12,
                  flexShrink: 0,
                }}
              >
                <Text
                  className="text-lg font-bold text-zinc-900"
                  style={{ flex: 1 }}
                >
                  Nova Solicitação
                </Text>
                <TouchableOpacity
                  onPress={handleCloseModal}
                  accessibilityRole="button"
                  accessibilityLabel="Fechar nova solicitação"
                  style={{
                    width: 44,
                    height: 44,
                    alignItems: "center",
                    justifyContent: "center",
                  }}
                >
                  <Ionicons name="close" size={24} color="#a1a1aa" />
                </TouchableOpacity>
              </View>
              <ScrollView
                style={{ flexShrink: 1 }}
                contentContainerStyle={{ padding: isMobile ? 16 : 24 }}
                keyboardShouldPersistTaps="handled"
                keyboardDismissMode={
                  Platform.OS === "ios" ? "interactive" : "on-drag"
                }
                showsVerticalScrollIndicator
              >
                <View className="mb-5">
                  <Text className="text-sm font-semibold text-zinc-700 mb-2">
                    Cliente
                  </Text>
                  <TextInput
                    value={novoCliente}
                    onChangeText={setNovoCliente}
                    placeholder="Nome do cliente"
                    placeholderTextColor="#a1a1aa"
                    className="w-full border border-zinc-200 rounded-xl px-4 py-3 bg-white text-zinc-900 text-base focus:border-[#a05a3c]"
                  />
                </View>
                <View className="mb-5">
                  <Text className="text-sm font-semibold text-zinc-700 mb-2">
                    Tipo de serviço
                  </Text>
                  <View className="border border-zinc-200 rounded-xl bg-white overflow-hidden focus:border-[#a05a3c]">
                    <Picker
                      selectedValue={novoTipoServico}
                      onValueChange={(itemValue) =>
                        setNovoTipoServico(itemValue)
                      }
                      style={{
                        height: Platform.OS === "ios" ? 180 : 52,
                        width: "100%",
                        color: "#18181b",
                        backgroundColor: "transparent",
                      }}
                      itemStyle={{
                        height: 180,
                        fontSize: 16,
                        color: "#18181b",
                      }}
                    >
                      {tiposServicoDisponiveis.map((tipo) => (
                        <Picker.Item key={tipo} label={tipo} value={tipo} />
                      ))}
                    </Picker>
                  </View>
                </View>
                <View
                  style={{
                    flexDirection: isMobile ? "column" : "row",
                    gap: 16,
                    marginBottom: 20,
                  }}
                >
                  <View style={{ flex: isMobile ? undefined : 1, minWidth: 0 }}>
                    <Text className="text-sm font-semibold text-zinc-700 mb-2">
                      Idioma de origem
                    </Text>
                    <View className="border border-zinc-200 rounded-xl bg-white overflow-hidden focus:border-[#a05a3c]">
                      <Picker
                        selectedValue={novoIdiomaOrigem}
                        onValueChange={(itemValue) =>
                          setNovoIdiomaOrigem(itemValue)
                        }
                        style={{
                          height: Platform.OS === "ios" ? 180 : 52,
                          width: "100%",
                          color: "#18181b",
                          backgroundColor: "transparent",
                        }}
                        itemStyle={{
                          height: 180,
                          fontSize: 16,
                          color: "#18181b",
                        }}
                      >
                        {idiomasDisponiveis.map((idioma) => (
                          <Picker.Item
                            key={`origem-${idioma}`}
                            label={idioma}
                            value={idioma}
                          />
                        ))}
                      </Picker>
                    </View>
                  </View>
                  <View style={{ flex: isMobile ? undefined : 1, minWidth: 0 }}>
                    <Text className="text-sm font-semibold text-zinc-700 mb-2">
                      Idioma de destino
                    </Text>
                    <View className="border border-zinc-200 rounded-xl bg-white overflow-hidden focus:border-[#a05a3c]">
                      <Picker
                        selectedValue={novoIdiomaDestino}
                        onValueChange={(itemValue) =>
                          setNovoIdiomaDestino(itemValue)
                        }
                        style={{
                          height: Platform.OS === "ios" ? 180 : 52,
                          width: "100%",
                          color: "#18181b",
                          backgroundColor: "transparent",
                        }}
                        itemStyle={{
                          height: 180,
                          fontSize: 16,
                          color: "#18181b",
                        }}
                      >
                        {idiomasDisponiveis.map((idioma) => (
                          <Picker.Item
                            key={`destino-${idioma}`}
                            label={idioma}
                            value={idioma}
                          />
                        ))}
                      </Picker>
                    </View>
                  </View>
                </View>
                {/* Upload de Arquivo Simulado */}
                <View className="mb-5">
                  <Text className="text-sm font-semibold text-zinc-700 mb-2">
                    Arquivo (máx. 20MB)
                  </Text>
                  <TouchableOpacity className="border-2 border-dashed border-[#a05a3c]/30 bg-[#a05a3c]/5 rounded-xl p-5 items-center justify-center hover:bg-[#a05a3c]/10 transition-colors">
                    <Text className="text-[#a05a3c] font-medium text-sm mb-1">
                      Toque para selecionar o arquivo
                    </Text>
                    <Text className="text-zinc-400 text-xs text-center">
                      PDF, DOCX, XLSX, PPTX — máx. 20MB
                    </Text>
                  </TouchableOpacity>
                </View>
                <View className="mb-8">
                  <Text className="text-sm font-semibold text-zinc-700 mb-2">
                    Observações
                  </Text>
                  <TextInput
                    value={novaObservacao}
                    onChangeText={setNovaObservacao}
                    placeholder="Detalhes adicionais sobre a solicitação..."
                    placeholderTextColor="#a1a1aa"
                    multiline={true}
                    numberOfLines={4}
                    textAlignVertical="top"
                    className="w-full border border-zinc-200 rounded-xl px-4 py-3 bg-white text-zinc-900 text-base focus:border-[#a05a3c] min-h-[100px]"
                  />
                </View>
                <View
                  style={{
                    flexDirection: width < 360 ? "column" : "row",
                    gap: 12,
                  }}
                >
                  <TouchableOpacity
                    onPress={handleCloseModal}
                    style={{
                      flex: width < 360 ? undefined : 1,
                      minHeight: 48,
                      justifyContent: "center",
                    }}
                    accessibilityRole="button"
                    className="py-3.5 border border-zinc-200 rounded-xl items-center hover:bg-zinc-50 transition-colors"
                  >
                    <Text className="text-zinc-600 font-semibold text-sm">
                      Cancelar
                    </Text>
                  </TouchableOpacity>
                  <TouchableOpacity
                    onPress={handleRegistrar}
                    style={{
                      flex: width < 360 ? undefined : 1,
                      minHeight: 48,
                      justifyContent: "center",
                    }}
                    accessibilityRole="button"
                    className="py-3.5 bg-[#a05a3c] rounded-xl items-center hover:bg-[#8c5230] transition-colors"
                  >
                    <Text className="text-white font-bold text-sm">
                      Registrar
                    </Text>
                  </TouchableOpacity>
                </View>
              </ScrollView>
            </View>
          </View>
        </KeyboardAvoidingView>
      </Modal>
    </View>
  );
}
