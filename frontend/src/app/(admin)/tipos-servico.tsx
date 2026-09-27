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
interface TipoServico {
  id: string;
  nome: string;
  descricao: string;
  unidadeCobranca:
    "por palavra" | "por hora" | "por minuto" | "por página" | "por lauda";
  status: "Ativo" | "Inativo";
}
export default function TiposServicoScreen() {
  const { width } = useWindowDimensions();
  const insets = useSafeAreaInsets();
  const isMobile = width < 768;
  const [gridWidth, setGridWidth] = useState(0);
  const gridGap = isMobile ? 12 : 24;
  // Usa a largura real do conteúdo, descontando a sidebar e as margens.
  const columns = isMobile
    ? 1
    : gridWidth >= 1000
      ? 3
      : gridWidth >= 640
        ? 2
        : 1;
  const cardWidth =
    columns === 1
      ? undefined
      : Math.floor((gridWidth - gridGap * (columns - 1)) / columns);

  const [isLoading, setIsLoading] = useState(true);
  const [tiposServico, setTiposServico] = useState<TipoServico[]>([]);
  const [isModalVisible, setIsModalVisible] = useState(false);
  const [novoNome, setNovoNome] = useState("");
  const [novaDescricao, setNovaDescricao] = useState("");
  const [novaUnidade, setNovaUnidade] =
    useState<TipoServico["unidadeCobranca"]>("por palavra");
  const [novoStatusAtivo, setNovoStatusAtivo] = useState(true);
  useEffect(() => {
    const fetchTiposServico = async () => {
      try {
        await new Promise((resolve) => setTimeout(resolve, 600));
        const mockTipos: TipoServico[] = [
          {
            id: "1",
            nome: "Tradução",
            descricao: "Tradução de textos entre idiomas",
            unidadeCobranca: "por palavra",
            status: "Ativo",
          },
          {
            id: "2",
            nome: "Revisão",
            descricao: "Revisão e edição de traduções existentes",
            unidadeCobranca: "por palavra",
            status: "Ativo",
          },
          {
            id: "3",
            nome: "Localização",
            descricao: "Adaptação cultural e regional de conteúdo",
            unidadeCobranca: "por hora",
            status: "Ativo",
          },
          {
            id: "4",
            nome: "Interpretação",
            descricao: "Interpretação simultânea ou consecutiva",
            unidadeCobranca: "por hora",
            status: "Ativo",
          },
          {
            id: "5",
            nome: "Legendagem",
            descricao: "Criação e tradução de legendas audiovisuais",
            unidadeCobranca: "por minuto",
            status: "Inativo",
          },
        ];
        setTiposServico(mockTipos);
      } catch (error) {
        console.error(error);
      } finally {
        setIsLoading(false);
      }
    };
    fetchTiposServico();
  }, []);
  const handleCreateTipoServico = () => {
    if (!novoNome || !novaDescricao) return;
    const newTipo: TipoServico = {
      id: Math.random().toString(),
      nome: novoNome,
      descricao: novaDescricao,
      unidadeCobranca: novaUnidade,
      status: novoStatusAtivo ? "Ativo" : "Inativo",
    };
    setTiposServico([...tiposServico, newTipo]);
    handleCloseModal();
  };
  const handleCloseModal = () => {
    setIsModalVisible(false);
    setNovoNome("");
    setNovaDescricao("");
    setNovaUnidade("por palavra");
    setNovoStatusAtivo(true);
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
              Tipos de Serviço
            </Text>
            <Text className="text-zinc-500 text-sm">
              Cadastre os tipos de serviço oferecidos pela empresa
            </Text>
          </View>
          <TouchableOpacity
            onPress={() => setIsModalVisible(true)}
            accessibilityRole="button"
            accessibilityLabel="Novo tipo de serviço"
            style={{ minHeight: 48, justifyContent: "center" }}
            className="bg-[#a05a3c] px-5 py-3 rounded-lg flex-row items-center hover:bg-[#8c5230] transition-colors shadow-sm"
          >
            <Text className="text-white font-bold text-sm">+ Novo Tipo</Text>
          </TouchableOpacity>
        </View>
        <View
          onLayout={({ nativeEvent }) => setGridWidth(nativeEvent.layout.width)}
          style={{
            flexDirection: columns === 1 ? "column" : "row",
            flexWrap: "wrap",
            gap: gridGap,
            minWidth: 0,
          }}
        >
          {tiposServico.map((tipo) => {
            const isAtivo = tipo.status === "Ativo";
            return (
              <View
                key={tipo.id}
                className={`bg-white rounded-2xl border shadow-sm flex-col justify-between ${isAtivo ? "border-zinc-100" : "border-zinc-200/50 opacity-70"}`}
                style={{
                  width: columns === 1 ? "100%" : cardWidth,
                  minWidth: 0,
                  padding: isMobile ? 16 : 24,
                }}
              >
                <View>
                  <View
                    style={{
                      flexDirection: "row",
                      flexWrap: "wrap",
                      justifyContent: "space-between",
                      alignItems: "center",
                      gap: 8,
                      marginBottom: 16,
                    }}
                  >
                    <View
                      className={`w-10 h-10 rounded-xl items-center justify-center ${isAtivo ? "bg-[#8c5230]/10" : "bg-zinc-100"}`}
                    >
                      <Ionicons
                        name="layers-outline"
                        size={20}
                        color={isAtivo ? "#8c5230" : "#a1a1aa"}
                      />
                    </View>
                    <View
                      className={`${isAtivo ? "bg-green-50 text-green-700" : "bg-zinc-100 text-zinc-500"} px-3 py-1 rounded-full`}
                    >
                      <Text
                        className={`text-xs font-bold ${isAtivo ? "text-green-700" : "text-zinc-500"}`}
                      >
                        {tipo.status}
                      </Text>
                    </View>
                  </View>
                  <Text className="text-lg font-bold text-zinc-900 mb-1">
                    {tipo.nome}
                  </Text>
                  <Text className="text-sm text-zinc-500 mb-6 min-h-[40px]">
                    {tipo.descricao}
                  </Text>
                </View>
                <View
                  style={{
                    flexDirection: "row",
                    flexWrap: "wrap",
                    justifyContent: "space-between",
                    alignItems: "center",
                    gap: 8,
                  }}
                >
                  <View className="bg-[#8c5230]/5 px-3 py-1.5 rounded-lg border border-[#8c5230]/10">
                    <Text className="text-[#8c5230] text-xs font-medium">
                      {tipo.unidadeCobranca}
                    </Text>
                  </View>
                  <TouchableOpacity
                    accessibilityRole="button"
                    accessibilityLabel={`Editar tipo de serviço ${tipo.nome}`}
                    style={{
                      minHeight: 44,
                      minWidth: 64,
                      justifyContent: "center",
                      alignItems: "center",
                    }}
                    className="hover:opacity-70 transition-opacity"
                  >
                    <Text className="text-[#a05a3c] text-xs font-semibold">
                      Editar ➔
                    </Text>
                  </TouchableOpacity>
                </View>
              </View>
            );
          })}
        </View>
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
                maxWidth: 480,
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
                  Novo Tipo de Serviço
                </Text>
                <TouchableOpacity
                  onPress={handleCloseModal}
                  accessibilityRole="button"
                  accessibilityLabel="Fechar novo tipo de serviço"
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
                    Nome
                  </Text>
                  <TextInput
                    value={novoNome}
                    onChangeText={setNovoNome}
                    accessibilityLabel="Nome do tipo de serviço"
                    style={{ minHeight: 48 }}
                    placeholder="Ex: Tradução"
                    placeholderTextColor="#a1a1aa"
                    className="w-full border border-zinc-200 rounded-xl px-4 py-3 bg-white text-zinc-900 text-base focus:border-[#a05a3c]"
                  />
                </View>
                <View className="mb-5">
                  <Text className="text-sm font-semibold text-zinc-700 mb-2">
                    Descrição
                  </Text>
                  <TextInput
                    value={novaDescricao}
                    onChangeText={setNovaDescricao}
                    accessibilityLabel="Descrição do tipo de serviço"
                    multiline
                    textAlignVertical="top"
                    style={{ minHeight: 96 }}
                    placeholder="Breve descrição"
                    placeholderTextColor="#a1a1aa"
                    className="w-full border border-zinc-200 rounded-xl px-4 py-3 bg-white text-zinc-900 text-base focus:border-[#a05a3c]"
                  />
                </View>
                <View className="mb-6">
                  <Text className="text-sm font-semibold text-zinc-700 mb-2">
                    Unidade de cobrança
                  </Text>
                  <View className="border border-zinc-200 rounded-xl bg-white overflow-hidden">
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
                      <Picker.Item label="Por palavra" value="por palavra" />
                      <Picker.Item label="Por hora" value="por hora" />
                      <Picker.Item label="Por minuto" value="por minuto" />
                      <Picker.Item label="Por página" value="por página" />
                      <Picker.Item label="Por lauda" value="por lauda" />
                    </Picker>
                  </View>
                </View>
                <View className="mb-8">
                  <TouchableOpacity
                    onPress={() => setNovoStatusAtivo(!novoStatusAtivo)}
                    accessibilityRole="checkbox"
                    accessibilityState={{ checked: novoStatusAtivo }}
                    accessibilityLabel="Tipo ativo"
                    style={{ minHeight: 44 }}
                    className="flex-row items-center"
                  >
                    <View
                      className={`w-5 h-5 rounded border items-center justify-center mr-3 transition-colors ${novoStatusAtivo ? "bg-[#a05a3c] border-[#a05a3c]" : "bg-white border-zinc-300"}`}
                    >
                      {novoStatusAtivo && (
                        <Ionicons name="checkmark" size={14} color="#ffffff" />
                      )}
                    </View>
                    <Text className="text-sm text-zinc-700 font-medium">
                      Tipo ativo
                    </Text>
                  </TouchableOpacity>
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
                    onPress={handleCreateTipoServico}
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
