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
} from "react-native";
import { Ionicons } from "@expo/vector-icons";
import { Picker } from "@react-native-picker/picker";
import { useSafeAreaInsets } from "react-native-safe-area-context";
interface Idioma {
  id: string;
  codigoIso: string;
}
interface TipoServico {
  id: string;
  nome: string;
}
interface PrecoEntry {
  id: string;
  tipoServico: string;
  idiomaOrigem: string;
  idiomaDestino: string;
  unidade: string;
  valorUnitario: number;
}
export default function TabelaPrecosScreen() {
  const { width } = useWindowDimensions();
  const insets = useSafeAreaInsets();
  const isMobile = width < 768;

  const [isLoading, setIsLoading] = useState(true);
  const [precos, setPrecos] = useState<PrecoEntry[]>([]);
  const [idiomasDisponiveis, setIdiomasDisponiveis] = useState<Idioma[]>([]);
  const [tiposServicoDisponiveis, setTiposServicoDisponiveis] = useState<
    TipoServico[]
  >([]);
  const [isModalVisible, setIsModalVisible] = useState(false);
  const [novoTipoServico, setNovoTipoServico] = useState("");
  const [novoIdiomaOrigem, setNovoIdiomaOrigem] = useState("");
  const [novoIdiomaDestino, setNovoIdiomaDestino] = useState("");
  const [novaUnidade, setNovaUnidade] = useState("Por palavra");
  const [novoValor, setNovoValor] = useState("");
  useEffect(() => {
    const fetchData = async () => {
      try {
        await new Promise((resolve) => setTimeout(resolve, 800));
        const mockIdiomas: Idioma[] = [
          { id: "1", codigoIso: "PT-BR" },
          { id: "2", codigoIso: "EN-US" },
          { id: "3", codigoIso: "ES-MX" },
          { id: "4", codigoIso: "DE-DE" },
          { id: "5", codigoIso: "FR-FR" },
          { id: "6", codigoIso: "ZH-CN" },
          { id: "7", codigoIso: "JA-JP" },
          { id: "8", codigoIso: "IT-IT" },
        ];
        const mockTipos: TipoServico[] = [
          { id: "1", nome: "Tradução" },
          { id: "2", nome: "Revisão" },
          { id: "3", nome: "Localização" },
          { id: "4", nome: "Interpretação" },
          { id: "5", nome: "Legendagem" },
        ];
        const mockPrecos: PrecoEntry[] = [
          {
            id: "1",
            tipoServico: "Tradução",
            idiomaOrigem: "PT-BR",
            idiomaDestino: "EN-US",
            unidade: "por palavra",
            valorUnitario: 0.12,
          },
          {
            id: "2",
            tipoServico: "Tradução",
            idiomaOrigem: "EN-US",
            idiomaDestino: "PT-BR",
            unidade: "por palavra",
            valorUnitario: 0.1,
          },
          {
            id: "3",
            tipoServico: "Tradução",
            idiomaOrigem: "PT-BR",
            idiomaDestino: "DE-DE",
            unidade: "por palavra",
            valorUnitario: 0.16,
          },
          {
            id: "4",
            tipoServico: "Revisão",
            idiomaOrigem: "PT-BR",
            idiomaDestino: "EN-US",
            unidade: "por palavra",
            valorUnitario: 0.06,
          },
          {
            id: "5",
            tipoServico: "Revisão",
            idiomaOrigem: "EN-US",
            idiomaDestino: "PT-BR",
            unidade: "por palavra",
            valorUnitario: 0.05,
          },
          {
            id: "6",
            tipoServico: "Localização",
            idiomaOrigem: "PT-BR",
            idiomaDestino: "ZH-CN",
            unidade: "por hora",
            valorUnitario: 180.0,
          },
          {
            id: "7",
            tipoServico: "Interpretação",
            idiomaOrigem: "PT-BR",
            idiomaDestino: "EN-US",
            unidade: "por hora",
            valorUnitario: 250.0,
          },
        ];
        setIdiomasDisponiveis(mockIdiomas);
        setTiposServicoDisponiveis(mockTipos);
        setPrecos(mockPrecos);
        if (mockTipos.length > 0) setNovoTipoServico(mockTipos[0].nome);
        if (mockIdiomas.length > 0) {
          setNovoIdiomaOrigem(mockIdiomas[0].codigoIso);
          setNovoIdiomaDestino(
            mockIdiomas[1]?.codigoIso || mockIdiomas[0].codigoIso,
          );
        }
      } catch (error) {
        console.error(error);
      } finally {
        setIsLoading(false);
      }
    };
    fetchData();
  }, []);
  const formatCurrency = (value: number) => {
    return `R$ ${value.toFixed(2).replace(".", ",")}`;
  };
  const handleCloseModal = () => {
    setIsModalVisible(false);
    setNovoValor("");
    if (tiposServicoDisponiveis.length > 0)
      setNovoTipoServico(tiposServicoDisponiveis[0].nome);
    if (idiomasDisponiveis.length > 0) {
      setNovoIdiomaOrigem(idiomasDisponiveis[0].codigoIso);
      setNovoIdiomaDestino(
        idiomasDisponiveis[1]?.codigoIso || idiomasDisponiveis[0].codigoIso,
      );
    }
    setNovaUnidade("Por palavra");
  };
  const handleSave = () => {
    if (!novoValor) return;
    const parsedValor = parseFloat(novoValor.replace(",", "."));
    if (isNaN(parsedValor)) return;
    const newEntry: PrecoEntry = {
      id: Math.random().toString(),
      tipoServico: novoTipoServico,
      idiomaOrigem: novoIdiomaOrigem,
      idiomaDestino: novoIdiomaDestino,
      unidade: novaUnidade.toLowerCase(),
      valorUnitario: parsedValor,
    };
    setPrecos([newEntry, ...precos]);
    handleCloseModal();
  };
  const handleRemove = (id: string) => {
    setPrecos(precos.filter((p) => p.id !== id));
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
        keyboardShouldPersistTaps="handled"
        showsVerticalScrollIndicator={false}
      >
        <View
          style={{
            flexDirection: isMobile ? "column" : "row",
            flexWrap: "wrap",
            alignItems: isMobile ? "stretch" : "center",
            justifyContent: "space-between",
            gap: 16,
            marginBottom: 24,
          }}
        >
          <View style={{ flexShrink: 1, minWidth: 0 }}>
            <Text
              className="font-bold text-zinc-900 mb-1"
              style={{ fontSize: isMobile ? 26 : 30 }}
            >
              Tabela de Preços
            </Text>
            <Text className="text-zinc-500 text-sm">
              Preços por tipo de serviço e par de idiomas — alimenta sugestões
              automáticas em orçamentos
            </Text>
          </View>
          <TouchableOpacity
            onPress={() => setIsModalVisible(true)}
            accessibilityRole="button"
            accessibilityLabel="Nova entrada de preço"
            style={{ minHeight: 48, justifyContent: "center" }}
            className="bg-[#a05a3c] px-5 py-3 rounded-lg flex-row items-center hover:bg-[#8c5230] transition-colors shadow-sm"
          >
            <Text className="text-white font-bold text-sm">+ Nova Entrada</Text>
          </TouchableOpacity>
        </View>
        {isMobile ? (
          <View style={{ gap: 12 }}>
            {precos.map((preco) => (
              <View
                key={preco.id}
                className="bg-white rounded-2xl border border-zinc-100 shadow-sm"
                style={{ padding: 16, minWidth: 0 }}
              >
                <View
                  className="bg-[#8c5230]/10 rounded-full self-start"
                  style={{
                    paddingHorizontal: 12,
                    paddingVertical: 6,
                    marginBottom: 16,
                    maxWidth: "100%",
                  }}
                >
                  <Text className="text-sm font-semibold text-[#8c5230]">
                    {preco.tipoServico}
                  </Text>
                </View>
                <View
                  style={{
                    flexDirection: "row",
                    alignItems: "center",
                    flexWrap: "wrap",
                    gap: 12,
                    marginBottom: 16,
                  }}
                >
                  <View style={{ flexShrink: 1 }}>
                    <Text className="text-xs text-zinc-400 mb-1">Origem</Text>
                    <Text className="text-base font-semibold text-zinc-700">
                      {preco.idiomaOrigem}
                    </Text>
                  </View>
                  <Ionicons
                    name="arrow-forward-outline"
                    size={20}
                    color="#a1a1aa"
                  />
                  <View style={{ flexShrink: 1 }}>
                    <Text className="text-xs text-zinc-400 mb-1">Destino</Text>
                    <Text className="text-base font-semibold text-zinc-700">
                      {preco.idiomaDestino}
                    </Text>
                  </View>
                </View>
                <View
                  className="border-t border-zinc-100"
                  style={{ paddingTop: 14, marginBottom: 16 }}
                >
                  <Text className="text-xs text-zinc-400 mb-1">
                    Valor unitário
                  </Text>
                  <View
                    style={{
                      flexDirection: "row",
                      alignItems: "baseline",
                      flexWrap: "wrap",
                      gap: 8,
                    }}
                  >
                    <Text className="text-2xl font-bold text-[#8c5230]">
                      {formatCurrency(preco.valorUnitario)}
                    </Text>
                    <Text className="text-sm text-zinc-500">
                      {preco.unidade}
                    </Text>
                  </View>
                </View>
                <View
                  style={{ flexDirection: "row", flexWrap: "wrap", gap: 10 }}
                >
                  {/* O botão Editar ainda não possui ação no código original. */}
                  <TouchableOpacity
                    accessibilityRole="button"
                    accessibilityLabel={`Editar preço de ${preco.tipoServico}`}
                    className="border border-zinc-200 rounded-xl"
                    style={{
                      flexGrow: 1,
                      flexBasis: 100,
                      minHeight: 44,
                      alignItems: "center",
                      justifyContent: "center",
                      padding: 10,
                    }}
                  >
                    <Text className="text-sm font-semibold text-zinc-600">
                      Editar
                    </Text>
                  </TouchableOpacity>
                  <TouchableOpacity
                    onPress={() => handleRemove(preco.id)}
                    accessibilityRole="button"
                    accessibilityLabel={`Remover preço de ${preco.tipoServico}, ${preco.idiomaOrigem} para ${preco.idiomaDestino}`}
                    className="border border-red-200 bg-red-50 rounded-xl"
                    style={{
                      flexGrow: 1,
                      flexBasis: 100,
                      minHeight: 44,
                      alignItems: "center",
                      justifyContent: "center",
                      padding: 10,
                    }}
                  >
                    <Text className="text-sm font-semibold text-red-500">
                      Remover
                    </Text>
                  </TouchableOpacity>
                </View>
              </View>
            ))}
            {precos.length === 0 && (
              <View
                className="bg-white border border-zinc-100 rounded-2xl"
                style={{ padding: 24, alignItems: "center", gap: 12 }}
              >
                <Ionicons name="pricetag-outline" size={32} color="#a1a1aa" />
                <Text className="text-sm text-zinc-500 text-center">
                  Nenhum preço cadastrado.
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
              style={{ minWidth: 1000, flex: 1 }}
            >
              <View className="flex-row items-center border-b border-zinc-100 bg-zinc-50/50 p-6">
                <Text className="flex-[1.5] text-xs font-bold text-zinc-400">
                  Tipo de Serviço
                </Text>
                <Text className="flex-[1.5] text-xs font-bold text-zinc-400">
                  Idioma Origem
                </Text>
                <Text className="flex-[1.5] text-xs font-bold text-zinc-400">
                  Idioma Destino
                </Text>
                <Text className="flex-[1.5] text-xs font-bold text-zinc-400">
                  Unidade
                </Text>
                <Text className="flex-[1.5] text-xs font-bold text-zinc-400">
                  Valor Unitário
                </Text>
                <Text
                  className="text-xs font-bold text-zinc-400 text-right"
                  style={{ width: 168 }}
                >
                  Ações
                </Text>
              </View>
              {precos.map((preco, index) => (
                <View
                  key={preco.id}
                  className={`flex-row items-center p-6 ${index !== precos.length - 1 ? "border-b border-zinc-50" : ""}`}
                >
                  <View className="flex-[1.5]">
                    <View className="bg-[#8c5230]/10 px-3 py-1 rounded-full self-start">
                      <Text className="text-xs font-medium text-[#8c5230]">
                        {preco.tipoServico}
                      </Text>
                    </View>
                  </View>
                  <Text className="flex-[1.5] text-sm font-semibold text-zinc-600">
                    {preco.idiomaOrigem}
                  </Text>
                  <Text className="flex-[1.5] text-sm font-semibold text-zinc-600">
                    {preco.idiomaDestino}
                  </Text>
                  <Text className="flex-[1.5] text-sm text-zinc-500">
                    {preco.unidade}
                  </Text>
                  <Text className="flex-[1.5] text-sm font-bold text-zinc-900">
                    {formatCurrency(preco.valorUnitario)}
                  </Text>
                  <View
                    className="flex-row justify-end items-center gap-2"
                    style={{ width: 168 }}
                  >
                    <TouchableOpacity className="border border-zinc-200 px-3 py-1.5 rounded-lg hover:bg-zinc-50">
                      <Text className="text-xs font-medium text-zinc-500">
                        Editar
                      </Text>
                    </TouchableOpacity>
                    <TouchableOpacity
                      onPress={() => handleRemove(preco.id)}
                      className="border border-red-200 bg-red-50 px-3 py-1.5 rounded-lg hover:bg-red-100"
                    >
                      <Text className="text-xs font-medium text-red-500">
                        Remover
                      </Text>
                    </TouchableOpacity>
                  </View>
                </View>
              ))}
              {precos.length === 0 && (
                <View className="p-8 items-center justify-center">
                  <Text className="text-zinc-500 text-sm">
                    Nenhum preço cadastrado.
                  </Text>
                </View>
              )}
            </View>
          </ScrollView>
        )}
      </ScrollView>
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
                  Nova Entrada de Preço
                </Text>
                <TouchableOpacity
                  onPress={handleCloseModal}
                  accessibilityRole="button"
                  accessibilityLabel="Fechar nova entrada de preço"
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
                        <Picker.Item
                          key={tipo.id}
                          label={tipo.nome}
                          value={tipo.nome}
                        />
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
                            key={`origem-${idioma.id}`}
                            label={idioma.codigoIso}
                            value={idioma.codigoIso}
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
                            key={`destino-${idioma.id}`}
                            label={idioma.codigoIso}
                            value={idioma.codigoIso}
                          />
                        ))}
                      </Picker>
                    </View>
                  </View>
                </View>
                <View className="mb-5">
                  <Text className="text-sm font-semibold text-zinc-700 mb-2">
                    Unidade
                  </Text>
                  <View className="border border-zinc-200 rounded-xl bg-white overflow-hidden focus:border-[#a05a3c]">
                    <Picker
                      selectedValue={novaUnidade}
                      onValueChange={(itemValue) => setNovaUnidade(itemValue)}
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
                      <Picker.Item label="Por palavra" value="Por palavra" />
                      <Picker.Item label="Por hora" value="Por hora" />
                      <Picker.Item label="Por minuto" value="Por minuto" />
                      <Picker.Item label="Por página" value="Por página" />
                    </Picker>
                  </View>
                </View>
                <View className="mb-8">
                  <Text className="text-sm font-semibold text-zinc-700 mb-2">
                    Valor unitário (R$)
                  </Text>
                  <TextInput
                    value={novoValor}
                    onChangeText={setNovoValor}
                    placeholder="0,12"
                    placeholderTextColor="#a1a1aa"
                    keyboardType="decimal-pad"
                    accessibilityLabel="Valor unitário em reais"
                    style={{ minHeight: 48 }}
                    className="w-full border border-zinc-200 rounded-xl px-4 py-3 bg-white text-zinc-900 text-base focus:border-[#a05a3c]"
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
                    accessibilityRole="button"
                    style={{
                      flex: width < 360 ? undefined : 1,
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
                    onPress={handleSave}
                    accessibilityRole="button"
                    style={{
                      flex: width < 360 ? undefined : 1,
                      minHeight: 48,
                      justifyContent: "center",
                    }}
                    className="py-3.5 bg-[#a05a3c] rounded-xl items-center hover:bg-[#8c5230] transition-colors"
                  >
                    <Text className="text-white font-bold text-sm">Salvar</Text>
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
