import React, { useState, useEffect } from "react";
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
  BackHandler,
} from "react-native";
import { Ionicons } from "@expo/vector-icons";
import { Picker } from "@react-native-picker/picker";
import { useSafeAreaInsets } from "react-native-safe-area-context";
type StatusOrcamento =
  "Rascunho" | "Enviado" | "Aprovado" | "Recusado" | "Expirado";
interface OrcamentoItem {
  id: string;
  servico: string;
  parIdiomas: string;
  quantidade: number;
  unidade: string;
  precoUnitario: number;
  total: number;
}
interface Orcamento {
  id: string;
  cliente: string;
  solicitacao: string;
  dataCriacao: string;
  dataValidade: string;
  status: StatusOrcamento;
  valorTotal: number;
  linkAprovacao?: string;
  itens: OrcamentoItem[];
}
export default function OrcamentosScreen() {
  const { width } = useWindowDimensions();
  const insets = useSafeAreaInsets();
  const isMobile = width < 768;
  const isCompact = width < 1024;

  const [isLoading, setIsLoading] = useState(true);
  const [orcamentos, setOrcamentos] = useState<Orcamento[]>([]);
  const [selectedOrcamentoId, setSelectedOrcamentoId] = useState<string | null>(
    null,
  );
  const [isModalVisible, setIsModalVisible] = useState(false);
  useEffect(() => {
    if (!isCompact || !selectedOrcamentoId || isModalVisible) return;
    const subscription = BackHandler.addEventListener(
      "hardwareBackPress",
      () => {
        setSelectedOrcamentoId(null);
        return true;
      },
    );
    return () => subscription.remove();
  }, [isCompact, selectedOrcamentoId, isModalVisible]);

  // Estados do Modal
  const [novoCliente, setNovoCliente] = useState("");
  const [novaSolicitacao, setNovaSolicitacao] = useState("");
  const [novoItens, setNovoItens] = useState([
    {
      id: Math.random().toString(),
      servico: "Tradução",
      parIdiomas: "PT-BR ➔ EN-US",
      quantidade: "1000",
      unidade: "palavras",
      precoUnitario: "0,12",
    },
  ]);
  useEffect(() => {
    const fetchOrcamentos = async () => {
      try {
        await new Promise((resolve) => setTimeout(resolve, 800));
        const mockData: Orcamento[] = [
          {
            id: "ORC-2024-042",
            cliente: "Embraer S.A.",
            solicitacao: "SOL-2024-089",
            dataCriacao: "18/09/2024",
            dataValidade: "18/10/2024",
            status: "Enviado",
            valorTotal: 544.0,
            linkAprovacao: "https://stentio.com.br/orcamento/tk_emb_xK2p9wQa",
            itens: [
              {
                id: "1",
                servico: "Tradução Técnica",
                parIdiomas: "PT-BR ➔ EN-US",
                quantidade: 3200,
                unidade: "palavras",
                precoUnitario: 0.12,
                total: 384.0,
              },
              {
                id: "2",
                servico: "Revisão",
                parIdiomas: "PT-BR ➔ EN-US",
                quantidade: 3200,
                unidade: "palavras",
                precoUnitario: 0.05,
                total: 160.0,
              },
            ],
          },
          {
            id: "ORC-2024-041",
            cliente: "Itaú Unibanco",
            solicitacao: "SOL-2024-087",
            dataCriacao: "16/09/2024",
            dataValidade: "16/10/2024",
            status: "Aprovado",
            valorTotal: 7200.0,
            itens: [
              {
                id: "3",
                servico: "Localização",
                parIdiomas: "PT-BR ➔ ZH-CN",
                quantidade: 40,
                unidade: "horas",
                precoUnitario: 180.0,
                total: 7200.0,
              },
            ],
          },
        ];
        setOrcamentos(mockData);
      } catch (error) {
        console.error(error);
      } finally {
        setIsLoading(false);
      }
    };
    fetchOrcamentos();
  }, []);
  const formatCurrency = (value: number) => {
    return `R$ ${value.toFixed(2).replace(".", ",")}`;
  };
  const getStatusStyle = (status: StatusOrcamento) => {
    switch (status) {
      case "Enviado":
        return "bg-blue-100 text-blue-700";
      case "Aprovado":
        return "bg-green-100 text-green-700";
      case "Rascunho":
        return "bg-zinc-100 text-zinc-600";
      case "Recusado":
        return "bg-red-100 text-red-700";
      case "Expirado":
        return "bg-orange-100 text-orange-700";
      default:
        return "bg-zinc-100 text-zinc-600";
    }
  };
  const handleAddItem = () => {
    setNovoItens([
      ...novoItens,
      {
        id: Math.random().toString(),
        servico: "Tradução",
        parIdiomas: "PT-BR ➔ EN-US",
        quantidade: "1",
        unidade: "palavras",
        precoUnitario: "0,00",
      },
    ]);
  };
  const handleUpdateItem = (id: string, field: string, value: string) => {
    setNovoItens(
      novoItens.map((item) =>
        item.id === id ? { ...item, [field]: value } : item,
      ),
    );
  };
  const handleRemoveItem = (id: string) => {
    if (novoItens.length > 1) {
      setNovoItens(novoItens.filter((item) => item.id !== id));
    }
  };
  const calcularTotalItem = (quantidade: string, precoUnitario: string) => {
    const qtd = parseFloat(quantidade.replace(",", ".")) || 0;
    const preco = parseFloat(precoUnitario.replace(",", ".")) || 0;
    return qtd * preco;
  };
  const calcularTotalEstimado = () => {
    return novoItens.reduce(
      (acc, item) =>
        acc + calcularTotalItem(item.quantidade, item.precoUnitario),
      0,
    );
  };
  const handleCloseModal = () => {
    setIsModalVisible(false);
    setNovoCliente("");
    setNovaSolicitacao("");
    setNovoItens([
      {
        id: Math.random().toString(),
        servico: "Tradução",
        parIdiomas: "PT-BR ➔ EN-US",
        quantidade: "1000",
        unidade: "palavras",
        precoUnitario: "0,12",
      },
    ]);
  };
  const handleCreateOrcamento = () => {
    if (!novoCliente) return;
    const dataAtual = new Date().toLocaleDateString("pt-BR");
    const validade = new Date();
    validade.setDate(validade.getDate() + 30);
    const novoOrcamento: Orcamento = {
      id: `ORC-2024-0${Math.floor(Math.random() * 90) + 10}`,
      cliente: novoCliente,
      solicitacao: novaSolicitacao || "N/A",
      dataCriacao: dataAtual,
      dataValidade: validade.toLocaleDateString("pt-BR"),
      status: "Rascunho",
      valorTotal: calcularTotalEstimado(),
      itens: novoItens.map((item) => ({
        id: item.id,
        servico: item.servico,
        parIdiomas: item.parIdiomas,
        quantidade: parseFloat(item.quantidade.replace(",", ".")) || 0,
        unidade: item.unidade,
        precoUnitario: parseFloat(item.precoUnitario.replace(",", ".")) || 0,
        total: calcularTotalItem(item.quantidade, item.precoUnitario),
      })),
    };
    setOrcamentos([novoOrcamento, ...orcamentos]);
    setSelectedOrcamentoId(novoOrcamento.id);
    handleCloseModal();
  };
  const selectedOrcamento = orcamentos.find(
    (o) => o.id === selectedOrcamentoId,
  );
  if (isLoading) {
    return (
      <View className="flex-1 items-center justify-center bg-[#f4f4f5]">
        <ActivityIndicator size="large" color="#8c5230" />
      </View>
    );
  }
  return (
    <View
      className="flex-1 bg-[#f4f4f5]"
      style={{ padding: isMobile ? 16 : 32, minWidth: 0, minHeight: 0 }}
    >
      <View
        style={{
          flexDirection: isMobile ? "column" : "row",
          flexWrap: "wrap",
          alignItems: isMobile ? "stretch" : "center",
          justifyContent: "space-between",
          gap: 12,
          marginBottom: isMobile ? 16 : 24,
        }}
      >
        <View>
          <Text
            className="font-bold text-zinc-900 mb-1"
            style={{ fontSize: isMobile ? 26 : 30 }}
          >
            Orçamentos
          </Text>
          <Text className="text-zinc-500 text-sm">
            Elaboração e gestão de propostas comerciais
          </Text>
        </View>
        <TouchableOpacity
          onPress={() => setIsModalVisible(true)}
          accessibilityRole="button"
          style={{ minHeight: 48, justifyContent: "center" }}
          className="bg-[#a05a3c] px-5 py-3 rounded-lg flex-row items-center hover:bg-[#8c5230] transition-colors shadow-sm"
        >
          <Text className="text-white font-bold text-sm">+ Novo Orçamento</Text>
        </TouchableOpacity>
      </View>
      <View
        style={{
          flex: 1,
          flexDirection: isCompact ? "column" : "row",
          gap: 24,
          minHeight: 0,
          minWidth: 0,
        }}
      >
        <View
          style={{
            flex: 1,
            minWidth: 0,
            minHeight: 0,
            display: isCompact && selectedOrcamento ? "none" : "flex",
          }}
        >
          <ScrollView
            style={{ flex: 1 }}
            showsVerticalScrollIndicator={false}
            contentContainerStyle={{ paddingBottom: 20 }}
          >
            {orcamentos.map((orc) => {
              const isActive = selectedOrcamentoId === orc.id;
              return (
                <TouchableOpacity
                  key={orc.id}
                  onPress={() => setSelectedOrcamentoId(orc.id)}
                  accessibilityRole="button"
                  accessibilityLabel={`Abrir orçamento ${orc.id} de ${orc.cliente}`}
                  accessibilityState={{ selected: isActive }}
                  className={`p-5 rounded-2xl border mb-4 bg-white transition-all ${
                    isActive
                      ? "border-zinc-900 shadow-md"
                      : "border-zinc-200 hover:border-zinc-400"
                  }`}
                >
                  <View
                    style={{
                      flexDirection: "row",
                      flexWrap: "wrap",
                      justifyContent: "space-between",
                      alignItems: "center",
                      gap: 8,
                      marginBottom: 12,
                    }}
                  >
                    <Text
                      className={`text-xs font-bold ${isActive ? "text-[#a05a3c]" : "text-zinc-500"}`}
                    >
                      {orc.id}
                    </Text>
                    <View
                      className={`${getStatusStyle(orc.status)} px-2 py-1 rounded-full`}
                    >
                      <Text
                        className={`text-xs font-bold uppercase ${getStatusStyle(orc.status).split(" ")[1]}`}
                      >
                        {orc.status}
                      </Text>
                    </View>
                  </View>
                  <Text className="text-base font-bold text-zinc-900 mb-1">
                    {orc.cliente}
                  </Text>
                  <Text className="text-xs text-zinc-400 mb-4">
                    Criado em {orc.dataCriacao} · Expira {orc.dataValidade}
                  </Text>
                  <Text className="text-lg font-bold text-[#8c5230]">
                    {formatCurrency(orc.valorTotal)}
                  </Text>
                </TouchableOpacity>
              );
            })}
          </ScrollView>
        </View>
        <View
          style={{
            flex: isCompact ? 1 : 2,
            minWidth: 0,
            minHeight: 0,
            display: isCompact && !selectedOrcamento ? "none" : "flex",
          }}
        >
          {isCompact && selectedOrcamento && (
            <TouchableOpacity
              onPress={() => setSelectedOrcamentoId(null)}
              accessibilityRole="button"
              accessibilityLabel="Voltar à lista de orçamentos"
              style={{
                flexDirection: "row",
                alignItems: "center",
                minHeight: 44,
                gap: 8,
                marginBottom: 8,
              }}
            >
              <Ionicons name="arrow-back" size={20} color="#8c5230" />
              <Text className="text-sm font-semibold text-[#8c5230]">
                Todos os orçamentos
              </Text>
            </TouchableOpacity>
          )}
          {selectedOrcamento ? (
            <ScrollView
              style={{ flex: 1 }}
              className="bg-white rounded-3xl border border-zinc-200 shadow-sm"
              showsVerticalScrollIndicator={false}
              contentContainerStyle={{ paddingBottom: 16 }}
            >
              <View
                className="border-b border-zinc-100"
                style={{ padding: isMobile ? 16 : 24 }}
              >
                <View style={{ gap: 16, marginBottom: 24 }}>
                  <View>
                    <View
                      style={{
                        flexDirection: "row",
                        flexWrap: "wrap",
                        alignItems: "center",
                        gap: 8,
                        marginBottom: 8,
                      }}
                    >
                      <Text
                        className="font-bold text-zinc-900"
                        style={{ fontSize: isMobile ? 20 : 24 }}
                      >
                        {selectedOrcamento.id}
                      </Text>
                      <View
                        className={`${getStatusStyle(selectedOrcamento.status)} px-3 py-1 rounded-full`}
                      >
                        <Text
                          className={`text-xs font-bold uppercase ${getStatusStyle(selectedOrcamento.status).split(" ")[1]}`}
                        >
                          {selectedOrcamento.status}
                        </Text>
                      </View>
                    </View>
                    <Text className="text-zinc-500 text-sm">
                      Cliente: {selectedOrcamento.cliente} · Solicitação:{" "}
                      {selectedOrcamento.solicitacao}
                    </Text>
                    <Text className="text-zinc-400 text-xs mt-1">
                      Criado: {selectedOrcamento.dataCriacao} · Válido até:{" "}
                      {selectedOrcamento.dataValidade}
                    </Text>
                  </View>
                  <View
                    style={{
                      flexDirection: isMobile ? "column" : "row",
                      flexWrap: "wrap",
                      gap: 12,
                    }}
                  >
                    <TouchableOpacity
                      accessibilityRole="button"
                      style={{ minHeight: 48, justifyContent: "center" }}
                      className="bg-[#a05a3c] px-4 py-2.5 rounded-lg flex-row items-center hover:bg-[#8c5230] transition-colors"
                    >
                      <Ionicons
                        name="mail-outline"
                        size={18}
                        color="#fff"
                        style={{ marginRight: 8 }}
                      />
                      <Text className="text-white font-bold text-sm">
                        Enviar por E-mail
                      </Text>
                    </TouchableOpacity>
                    <TouchableOpacity
                      accessibilityRole="button"
                      style={{ minHeight: 48, justifyContent: "center" }}
                      className="bg-white border border-zinc-300 px-4 py-2.5 rounded-lg flex-row items-center hover:bg-zinc-50 transition-colors"
                    >
                      <Ionicons
                        name="download-outline"
                        size={18}
                        color="#52525b"
                        style={{ marginRight: 8 }}
                      />
                      <Text className="text-zinc-700 font-bold text-sm">
                        Gerar PDF
                      </Text>
                    </TouchableOpacity>
                  </View>
                </View>
                {selectedOrcamento.linkAprovacao && (
                  <View className="bg-orange-50 border border-orange-200/60 rounded-xl p-5 mb-2">
                    <Text className="text-orange-800 text-xs font-bold uppercase tracking-wider mb-2">
                      Link de Aprovação do Cliente
                    </Text>
                    <ScrollView
                      horizontal
                      showsHorizontalScrollIndicator
                      style={{ marginBottom: 8 }}
                    >
                      <Text
                        selectable
                        className="text-zinc-600 font-medium text-sm"
                      >
                        {selectedOrcamento.linkAprovacao}
                      </Text>
                    </ScrollView>
                    <Text className="text-zinc-400 text-xs">
                      Válido por 30 dias · o cliente pode aprovar ou recusar sem
                      precisar criar uma conta.
                    </Text>
                  </View>
                )}
              </View>
              <View style={{ padding: isMobile ? 16 : 24 }}>
                <Text className="text-base font-bold text-zinc-900 mb-6">
                  Itens do Orçamento
                </Text>
                {isCompact ? (
                  <View style={{ gap: 12 }}>
                    {selectedOrcamento.itens.map((item) => (
                      <View
                        key={item.id}
                        className="bg-zinc-50 border border-zinc-100 rounded-xl"
                        style={{ padding: 14, gap: 8 }}
                      >
                        <Text className="text-base font-semibold text-zinc-800">
                          {item.servico}
                        </Text>
                        <Text className="text-sm text-zinc-500">
                          {item.parIdiomas}
                        </Text>
                        <Text className="text-sm text-zinc-600">
                          {item.quantidade.toLocaleString("pt-BR")}{" "}
                          {item.unidade} × {formatCurrency(item.precoUnitario)}
                        </Text>
                        <View
                          style={{
                            flexDirection: "row",
                            flexWrap: "wrap",
                            justifyContent: "space-between",
                            alignItems: "center",
                            gap: 8,
                          }}
                        >
                          <Text className="text-xs text-zinc-500">
                            Subtotal
                          </Text>
                          <Text className="text-base font-bold text-[#8c5230]">
                            {formatCurrency(item.total)}
                          </Text>
                        </View>
                      </View>
                    ))}
                  </View>
                ) : (
                  <ScrollView
                    horizontal
                    showsHorizontalScrollIndicator
                    contentContainerStyle={{ flexGrow: 1 }}
                  >
                    <View style={{ minWidth: 640, flex: 1 }}>
                      <View className="flex-row border-b border-zinc-200 pb-3 mb-4">
                        <Text className="flex-[2] text-xs font-bold text-zinc-400">
                          Serviço
                        </Text>
                        <Text className="flex-[2] text-xs font-bold text-zinc-400">
                          Par de idiomas
                        </Text>
                        <Text className="flex-1 text-xs font-bold text-zinc-400 text-right">
                          Qtd.
                        </Text>
                        <Text className="flex-1 text-xs font-bold text-zinc-400 text-center">
                          Unid.
                        </Text>
                        <Text className="flex-1 text-xs font-bold text-zinc-400 text-right">
                          R$ Unit.
                        </Text>
                        <Text className="flex-1 text-xs font-bold text-zinc-400 text-right">
                          Total
                        </Text>
                      </View>
                      {selectedOrcamento.itens.map((item, idx) => (
                        <View
                          key={item.id}
                          className={`flex-row items-center py-4 ${idx !== selectedOrcamento.itens.length - 1 ? "border-b border-zinc-50" : ""}`}
                        >
                          <Text className="flex-[2] text-sm text-zinc-800 font-medium">
                            {item.servico}
                          </Text>
                          <Text className="flex-[2] text-sm text-zinc-500">
                            {item.parIdiomas}
                          </Text>
                          <Text className="flex-1 text-sm text-zinc-700 text-right">
                            {item.quantidade.toLocaleString("pt-BR")}
                          </Text>
                          <Text className="flex-1 text-sm text-zinc-500 text-center">
                            {item.unidade}
                          </Text>
                          <Text className="flex-1 text-sm text-zinc-700 text-right">
                            {formatCurrency(item.precoUnitario)}
                          </Text>
                          <Text className="flex-1 text-sm font-bold text-zinc-900 text-right">
                            {formatCurrency(item.total)}
                          </Text>
                        </View>
                      ))}
                    </View>
                  </ScrollView>
                )}
                <View className="mt-8 pt-6 border-t border-zinc-200 items-end">
                  <Text className="text-sm text-zinc-500 mb-1">
                    Total do Orçamento
                  </Text>
                  <Text className="text-3xl font-bold text-[#8c5230]">
                    {formatCurrency(selectedOrcamento.valorTotal)}
                  </Text>
                </View>
              </View>
            </ScrollView>
          ) : (
            <View className="flex-1 bg-white rounded-3xl border border-zinc-200 shadow-sm items-center justify-center">
              <View className="w-16 h-16 bg-zinc-50 rounded-2xl items-center justify-center mb-4">
                <Ionicons
                  name="document-text-outline"
                  size={32}
                  color="#d4d4d8"
                />
              </View>
              <Text className="text-zinc-500 text-base font-medium">
                Selecione um orçamento
              </Text>
            </View>
          )}
        </View>
      </View>
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
                maxWidth: 672,
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
                  Novo Orçamento
                </Text>
                <TouchableOpacity
                  onPress={handleCloseModal}
                  accessibilityRole="button"
                  accessibilityLabel="Fechar novo orçamento"
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
                    className="w-full border border-zinc-200 rounded-xl px-4 py-3 bg-white text-zinc-900 text-base focus:border-[#a05a3c]"
                  />
                </View>
                <View className="mb-6">
                  <Text className="text-sm font-semibold text-zinc-700 mb-2">
                    Solicitação vinculada
                  </Text>
                  <TextInput
                    value={novaSolicitacao}
                    onChangeText={setNovaSolicitacao}
                    placeholder="SOL-2024-XXX"
                    className="w-full border border-zinc-200 rounded-xl px-4 py-3 bg-white text-zinc-900 text-base focus:border-[#a05a3c]"
                  />
                </View>
                <View className="mb-4 flex-row justify-between items-center">
                  <Text className="text-sm font-semibold text-zinc-700">
                    Itens
                  </Text>
                  <TouchableOpacity
                    onPress={handleAddItem}
                    accessibilityRole="button"
                    style={{ minHeight: 44, justifyContent: "center" }}
                  >
                    <Text className="text-[#a05a3c] font-semibold text-sm">
                      + Adicionar item
                    </Text>
                  </TouchableOpacity>
                </View>
                {novoItens.map((item, index) => (
                  <View
                    key={item.id}
                    className="bg-zinc-50 border border-zinc-200 rounded-xl p-4 mb-4 relative"
                  >
                    <View
                      style={{
                        flexDirection: "row",
                        justifyContent: "space-between",
                        alignItems: "center",
                        marginBottom: 12,
                      }}
                    >
                      <Text className="text-sm font-bold text-zinc-700">
                        Item {index + 1}
                      </Text>
                      {novoItens.length > 1 && (
                        <TouchableOpacity
                          onPress={() => handleRemoveItem(item.id)}
                          accessibilityRole="button"
                          accessibilityLabel={`Remover item ${index + 1}`}
                          style={{ width: 44, height: 44 }}
                          className="bg-white border border-red-200 rounded-full items-center justify-center"
                        >
                          <Ionicons name="close" size={14} color="#ef4444" />
                        </TouchableOpacity>
                      )}
                    </View>
                    <View
                      style={{
                        flexDirection: isMobile ? "column" : "row",
                        gap: 12,
                        marginBottom: 12,
                      }}
                    >
                      <View
                        style={{ flex: isMobile ? undefined : 1, minWidth: 0 }}
                      >
                        <Text className="text-xs font-semibold text-zinc-500 mb-1.5">
                          Serviço
                        </Text>
                        <View className="border border-zinc-200 rounded-lg bg-white overflow-hidden">
                          <Picker
                            selectedValue={item.servico}
                            onValueChange={(val) =>
                              handleUpdateItem(item.id, "servico", val)
                            }
                            style={{
                              height: Platform.OS === "ios" ? 180 : 52,
                              width: "100%",
                              fontSize: 16,
                              color: "#18181b",
                            }}
                            itemStyle={{
                              height: 180,
                              fontSize: 16,
                              color: "#18181b",
                            }}
                          >
                            <Picker.Item label="Tradução" value="Tradução" />
                            <Picker.Item
                              label="Tradução Técnica"
                              value="Tradução Técnica"
                            />
                            <Picker.Item label="Revisão" value="Revisão" />
                            <Picker.Item
                              label="Localização"
                              value="Localização"
                            />
                          </Picker>
                        </View>
                      </View>
                      <View
                        style={{ flex: isMobile ? undefined : 1, minWidth: 0 }}
                      >
                        <Text className="text-xs font-semibold text-zinc-500 mb-1.5">
                          Par de idiomas
                        </Text>
                        <View className="border border-zinc-200 rounded-lg bg-white overflow-hidden">
                          <Picker
                            selectedValue={item.parIdiomas}
                            onValueChange={(val) =>
                              handleUpdateItem(item.id, "parIdiomas", val)
                            }
                            style={{
                              height: Platform.OS === "ios" ? 180 : 52,
                              width: "100%",
                              fontSize: 16,
                              color: "#18181b",
                            }}
                            itemStyle={{
                              height: 180,
                              fontSize: 16,
                              color: "#18181b",
                            }}
                          >
                            <Picker.Item
                              label="PT-BR ➔ EN-US"
                              value="PT-BR ➔ EN-US"
                            />
                            <Picker.Item
                              label="EN-US ➔ PT-BR"
                              value="EN-US ➔ PT-BR"
                            />
                            <Picker.Item
                              label="PT-BR ➔ ES-MX"
                              value="PT-BR ➔ ES-MX"
                            />
                            <Picker.Item
                              label="PT-BR ➔ ZH-CN"
                              value="PT-BR ➔ ZH-CN"
                            />
                          </Picker>
                        </View>
                      </View>
                    </View>
                    <View
                      style={{
                        flexDirection: isMobile ? "column" : "row",
                        alignItems: isMobile ? "stretch" : "flex-end",
                        gap: 12,
                      }}
                    >
                      <View
                        style={{
                          flex: isMobile ? undefined : 1.2,
                          minWidth: 0,
                        }}
                      >
                        <Text className="text-xs font-semibold text-zinc-500 mb-1.5">
                          Quantidade
                        </Text>
                        <TextInput
                          value={item.quantidade}
                          onChangeText={(val) =>
                            handleUpdateItem(item.id, "quantidade", val)
                          }
                          keyboardType="decimal-pad"
                          style={{ minHeight: 48 }}
                          className="border border-zinc-200 rounded-lg px-3 py-2 bg-white text-zinc-900 text-base"
                        />
                      </View>
                      <View
                        style={{ flex: isMobile ? undefined : 1, minWidth: 0 }}
                      >
                        <Text className="text-xs font-semibold text-zinc-500 mb-1.5">
                          Unidade
                        </Text>
                        <View className="border border-zinc-200 rounded-lg bg-white overflow-hidden">
                          <Picker
                            selectedValue={item.unidade}
                            onValueChange={(val) =>
                              handleUpdateItem(item.id, "unidade", val)
                            }
                            style={{
                              height: Platform.OS === "ios" ? 180 : 52,
                              width: "100%",
                              fontSize: 16,
                              color: "#18181b",
                            }}
                            itemStyle={{
                              height: 180,
                              fontSize: 16,
                              color: "#18181b",
                            }}
                          >
                            <Picker.Item label="palavras" value="palavras" />
                            <Picker.Item label="horas" value="horas" />
                            <Picker.Item label="minutos" value="minutos" />
                          </Picker>
                        </View>
                      </View>
                      <View
                        style={{ flex: isMobile ? undefined : 1, minWidth: 0 }}
                      >
                        <Text className="text-xs font-semibold text-zinc-500 mb-1.5">
                          R$ unit.
                        </Text>
                        <TextInput
                          value={item.precoUnitario}
                          onChangeText={(val) =>
                            handleUpdateItem(item.id, "precoUnitario", val)
                          }
                          keyboardType="decimal-pad"
                          style={{ minHeight: 48 }}
                          className="border border-zinc-200 rounded-lg px-3 py-2 bg-white text-zinc-900 text-base"
                        />
                      </View>
                    </View>
                    <View
                      className="mt-4 pt-3 border-t border-zinc-200/60"
                      style={{
                        flexDirection: "row",
                        flexWrap: "wrap",
                        justifyContent: "space-between",
                        alignItems: "center",
                        gap: 8,
                      }}
                    >
                      <Text className="text-xs text-zinc-500">
                        Subtotal do item
                      </Text>
                      <Text className="text-sm font-bold text-[#8c5230]">
                        {formatCurrency(
                          calcularTotalItem(
                            item.quantidade,
                            item.precoUnitario,
                          ),
                        )}
                      </Text>
                    </View>
                  </View>
                ))}
                <View
                  className="mt-4 py-5 border-t border-zinc-200"
                  style={{
                    flexDirection: "row",
                    flexWrap: "wrap",
                    justifyContent: "space-between",
                    alignItems: "center",
                    gap: 8,
                  }}
                >
                  <Text className="text-base font-bold text-zinc-700">
                    Total Estimado:
                  </Text>
                  <Text className="text-2xl font-bold text-[#8c5230]">
                    {formatCurrency(calcularTotalEstimado())}
                  </Text>
                </View>
                <View
                  style={{
                    flexDirection: isMobile ? "column" : "row",
                    gap: 12,
                    marginTop: 16,
                  }}
                >
                  <TouchableOpacity
                    onPress={handleCloseModal}
                    accessibilityRole="button"
                    style={{
                      flex: isMobile ? undefined : 1,
                      minHeight: 48,
                      justifyContent: "center",
                    }}
                    className="py-3.5 border border-zinc-200 rounded-xl items-center hover:bg-zinc-50 transition-colors"
                  >
                    <Text className="text-zinc-600 font-semibold text-sm">
                      Cancelar
                    </Text>
                  </TouchableOpacity>
                  <TouchableOpacity
                    onPress={handleCreateOrcamento}
                    accessibilityRole="button"
                    style={{
                      flex: isMobile ? undefined : 1,
                      minHeight: 48,
                      justifyContent: "center",
                    }}
                    className="py-3.5 bg-[#a05a3c] rounded-xl items-center hover:bg-[#8c5230] transition-colors"
                  >
                    <Text className="text-white font-bold text-sm">
                      Criar Orçamento
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
