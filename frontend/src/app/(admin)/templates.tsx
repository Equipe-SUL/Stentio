import React, { useState, useEffect } from "react";
import {
  View,
  Text,
  ScrollView,
  TouchableOpacity,
  Modal,
  TextInput,
  Pressable,
  ActivityIndicator,
  useWindowDimensions,
  KeyboardAvoidingView,
  Platform,
} from "react-native";
import { Ionicons } from "@expo/vector-icons";
interface EmailTemplate {
  id: string;
  nome: string;
  gatilho: string;
  assunto: string;
  corpo: string;
}
export default function TemplatesScreen() {
  const { width } = useWindowDimensions();
  const isMobile = width < 768;
  const [contentWidth, setContentWidth] = useState(0);
  const sideBySide = contentWidth >= 900;

  const [isLoading, setIsLoading] = useState(true);
  const [templates, setTemplates] = useState<EmailTemplate[]>([]);
  const [selectedTemplateId, setSelectedTemplateId] = useState<string | null>(
    null,
  );
  const [isModalVisible, setIsModalVisible] = useState(false);
  const [novoNome, setNovoNome] = useState("");
  const [novoGatilho, setNovoGatilho] = useState("");
  const [novoAssunto, setNovoAssunto] = useState("");
  useEffect(() => {
    const fetchTemplates = async () => {
      try {
        await new Promise((resolve) => setTimeout(resolve, 600));
        const mockTemplates: EmailTemplate[] = [
          {
            id: "1",
            nome: "Orçamento Enviado",
            gatilho: "Criação de orçamento",
            assunto: "Seu orçamento está pronto — [[EMPRESA]]",
            corpo:
              "Olá, [[NOME_CLIENTE]],\nPreparamos um orçamento para a sua solicitação de [[TIPO_SERVICO]] (par: [[PAR_IDIOMAS]]).\nAcesse o link abaixo para visualizar, aprovar ou recusar:\n➔ Ver Orçamento\nO link é válido até [[DATA_EXPIRACAO]].\nAtenciosamente,\nEquipe [[EMPRESA]]",
          },
          {
            id: "2",
            nome: "Projeto Aprovado",
            gatilho: "Aprovação de orçamento",
            assunto: "Projeto aprovado — iniciando trabalho",
            corpo:
              "Olá, [[NOME_CLIENTE]],\nÓtima notícia! Recebemos sua aprovação e a equipe já está iniciando o trabalho.\nPrazo estimado de entrega: [[PRAZO_ENTREGA]].\nAtenciosamente,\nEquipe [[EMPRESA]]",
          },
          {
            id: "3",
            nome: "Entrega Concluída",
            gatilho: "Conclusão de projeto",
            assunto: "Entrega concluída — [[NOME_PROJETO]]",
            corpo:
              "Olá, [[NOME_CLIENTE]],\nTemos o prazer de informar que o projeto [[NOME_PROJETO]] foi concluído.\nOs arquivos estão disponíveis para download em: Acessar entrega",
          },
        ];
        setTemplates(mockTemplates);
        if (mockTemplates.length > 0) {
          setSelectedTemplateId(mockTemplates[0].id);
        }
      } catch (error) {
        console.error(error);
      } finally {
        setIsLoading(false);
      }
    };
    fetchTemplates();
  }, []);
  const handleCreateTemplate = async () => {
    if (!novoNome || !novoGatilho || !novoAssunto) return;
    const newTemplate: EmailTemplate = {
      id: Math.random().toString(),
      nome: novoNome,
      gatilho: novoGatilho,
      assunto: novoAssunto,
      corpo: "",
    };
    setTemplates([...templates, newTemplate]);
    setSelectedTemplateId(newTemplate.id);
    handleCloseModal();
  };
  const handleCloseModal = () => {
    setIsModalVisible(false);
    setNovoNome("");
    setNovoGatilho("");
    setNovoAssunto("");
  };
  if (isLoading) {
    return (
      <View className="flex-1 items-center justify-center bg-[#f4f4f5]">
        <ActivityIndicator size="large" color="#8c5230" />
      </View>
    );
  }
  const selectedTemplate = templates.find((t) => t.id === selectedTemplateId);
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
              Templates de E-mail
            </Text>
            <Text className="text-zinc-500 text-sm">
              Gerencie modelos de e-mail com placeholders dinâmicos
            </Text>
          </View>
          <TouchableOpacity
            onPress={() => setIsModalVisible(true)}
            accessibilityRole="button"
            accessibilityLabel="Novo template de e-mail"
            style={{ minHeight: 48, justifyContent: "center" }}
            className="bg-[#8c5230] px-5 py-3 rounded-lg flex-row items-center hover:bg-[#6f4f28] transition-colors"
          >
            <Text className="text-white font-bold text-sm">
              + Novo Template
            </Text>
          </TouchableOpacity>
        </View>
        <View
          onLayout={({ nativeEvent }) =>
            setContentWidth(nativeEvent.layout.width)
          }
          style={{
            flexDirection: sideBySide ? "row" : "column",
            gap: isMobile ? 16 : 24,
            minWidth: 0,
          }}
        >
          <View
            style={{
              width: sideBySide ? 300 : "100%",
              minWidth: 0,
              gap: 12,
              display: isMobile && selectedTemplate ? "none" : "flex",
            }}
          >
            {templates.map((template) => {
              const isActive = selectedTemplateId === template.id;
              return (
                <TouchableOpacity
                  key={template.id}
                  onPress={() => setSelectedTemplateId(template.id)}
                  accessibilityRole="button"
                  accessibilityLabel={`Abrir template ${template.nome}`}
                  accessibilityState={{ selected: isActive }}
                  style={{ minHeight: 72 }}
                  className={`p-5 rounded-2xl border transition-colors ${
                    isActive
                      ? "bg-[#8c5230] border-[#8c5230] shadow-md shadow-orange-900/10"
                      : "bg-white border-zinc-200 hover:border-[#8c5230]/40"
                  }`}
                >
                  <Text
                    className={`text-base font-bold mb-1 ${isActive ? "text-white" : "text-zinc-800"}`}
                  >
                    {template.nome}
                  </Text>
                  <Text
                    className={`text-xs ${isActive ? "text-white/80" : "text-zinc-400"}`}
                  >
                    {template.gatilho}
                  </Text>
                </TouchableOpacity>
              );
            })}
          </View>
          <View
            style={{
              flex: 1,
              minWidth: 0,
              display: isMobile && !selectedTemplate ? "none" : "flex",
            }}
          >
            {isMobile && selectedTemplate && (
              <TouchableOpacity
                onPress={() => setSelectedTemplateId(null)}
                accessibilityRole="button"
                accessibilityLabel="Voltar à lista de templates"
                style={{
                  flexDirection: "row",
                  alignItems: "center",
                  gap: 8,
                  minHeight: 44,
                  marginBottom: 8,
                }}
              >
                <Ionicons name="arrow-back" size={20} color="#8c5230" />
                <Text className="text-sm font-semibold text-[#8c5230]">
                  Todos os templates
                </Text>
              </TouchableOpacity>
            )}
            {selectedTemplate ? (
              <View
                className="bg-white rounded-3xl border border-zinc-200 shadow-sm"
                style={{ padding: isMobile ? 16 : 24 }}
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
                  <View>
                    <Text
                      className="font-bold text-zinc-900 mb-1"
                      style={{ fontSize: isMobile ? 20 : 24 }}
                    >
                      {selectedTemplate.nome}
                    </Text>
                    <Text className="text-sm text-zinc-400">
                      Gatilho: {selectedTemplate.gatilho}
                    </Text>
                  </View>
                  <TouchableOpacity
                    accessibilityRole="button"
                    style={{ minHeight: 44, justifyContent: "center" }}
                    className="border border-zinc-300 px-4 py-2 rounded-lg hover:bg-zinc-50 transition-colors"
                  >
                    <Text className="text-zinc-600 text-sm font-medium">
                      Editar
                    </Text>
                  </TouchableOpacity>
                </View>
                <View className="mb-6">
                  <Text className="text-xs font-bold text-zinc-400 tracking-wider mb-2">
                    ASSUNTO
                  </Text>
                  <View className="bg-zinc-50 border border-zinc-100 p-4 rounded-xl">
                    <Text className="text-zinc-800 text-sm">
                      {selectedTemplate.assunto}
                    </Text>
                  </View>
                </View>
                <View>
                  <Text className="text-xs font-bold text-zinc-400 tracking-wider mb-2">
                    CORPO DO E-MAIL (HTML)
                  </Text>
                  <View
                    className="bg-zinc-50 border border-zinc-100 rounded-xl"
                    style={{
                      padding: isMobile ? 14 : 20,
                      minHeight: isMobile ? 180 : 220,
                    }}
                  >
                    <Text
                      selectable
                      className="text-zinc-700 text-sm leading-6"
                    >
                      {selectedTemplate.corpo}
                    </Text>
                  </View>
                </View>
              </View>
            ) : (
              <View
                className="bg-white rounded-3xl border border-zinc-200 shadow-sm items-center justify-center"
                style={{ padding: 24, minHeight: 260 }}
              >
                <Text className="text-zinc-400">
                  Selecione um template para visualizar
                </Text>
              </View>
            )}
          </View>
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
              padding: 16,
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
                  Novo Template de E-mail
                </Text>
                <TouchableOpacity
                  onPress={handleCloseModal}
                  accessibilityRole="button"
                  accessibilityLabel="Fechar novo template"
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
                    Nome do template
                  </Text>
                  <TextInput
                    value={novoNome}
                    onChangeText={setNovoNome}
                    placeholder="Ex: Confirmação de Recebimento"
                    placeholderTextColor="#a1a1aa"
                    style={{ minHeight: 48 }}
                    className="w-full border border-zinc-300 rounded-xl px-4 py-3 bg-white text-zinc-900 text-base"
                  />
                </View>
                <View className="mb-5">
                  <Text className="text-sm font-semibold text-zinc-700 mb-2">
                    Gatilho / Evento
                  </Text>
                  <TextInput
                    value={novoGatilho}
                    onChangeText={setNovoGatilho}
                    placeholder="Ex: Recebimento de solicitação"
                    placeholderTextColor="#a1a1aa"
                    style={{ minHeight: 48 }}
                    className="w-full border border-zinc-300 rounded-xl px-4 py-3 bg-white text-zinc-900 text-base"
                  />
                </View>
                <View className="mb-8">
                  <Text className="text-sm font-semibold text-zinc-700 mb-2">
                    Assunto padrão
                  </Text>
                  <TextInput
                    value={novoAssunto}
                    onChangeText={setNovoAssunto}
                    placeholder="Ex: Recebemos sua solicitação"
                    placeholderTextColor="#a1a1aa"
                    style={{ minHeight: 48 }}
                    className="w-full border border-zinc-300 rounded-xl px-4 py-3 bg-white text-zinc-900 text-base"
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
                    className="py-3.5 border border-zinc-300 rounded-xl items-center"
                  >
                    <Text className="text-zinc-600 font-semibold text-sm">
                      Cancelar
                    </Text>
                  </TouchableOpacity>
                  <TouchableOpacity
                    onPress={handleCreateTemplate}
                    accessibilityRole="button"
                    style={{
                      flex: width < 360 ? undefined : 1,
                      minHeight: 48,
                      justifyContent: "center",
                    }}
                    className="py-3.5 bg-[#8c5230] rounded-xl items-center"
                  >
                    <Text className="text-white font-bold text-sm">
                      Criar e Editar
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
