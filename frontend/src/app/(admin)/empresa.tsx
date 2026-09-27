import React, { useState } from "react";
import {
  View,
  Text,
  ScrollView,
  TextInput,
  TouchableOpacity,
  ActivityIndicator,
  KeyboardAvoidingView,
  Platform,
  useWindowDimensions,
} from "react-native";
import { Ionicons } from "@expo/vector-icons";

export default function EmpresaScreen() {
  const { width } = useWindowDimensions();
  const isMobile = width < 768;
  const [contentWidth, setContentWidth] = useState(0);
  const sideBySide = contentWidth >= 1000;
  const [isLoading, setIsLoading] = useState(false);
  const [razaoSocial, setRazaoSocial] = useState("Stentio Traduções Ltda.");
  const [cnpj, setCnpj] = useState("12.345.678/0001-99");
  const [site, setSite] = useState("www.stentio.com.br");
  const [email, setEmail] = useState("contato@stentio.com.br");
  const [telefone, setTelefone] = useState("+55 (11) 3456-7890");
  const [logradouro, setLogradouro] = useState(
    "Av. Paulista, 1000 — Sala 1402",
  );
  const [cidade, setCidade] = useState("São Paulo");
  const [estado, setEstado] = useState("SP");
  const [cep, setCep] = useState("01310-100");
  const [rodape, setRodape] = useState(
    "Stentio Traduções Ltda. | CNPJ 12.345.678/0001-99 | contato@stentio.com.br",
  );

  const handleSave = async () => {
    setIsLoading(true);
    try {
      // TODO: Substituir por chamada real à API (ex: PUT /api/v1/empresa).
      await new Promise((resolve) => setTimeout(resolve, 800));
    } catch (error) {
      console.error(error);
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <KeyboardAvoidingView
      style={{ flex: 1, minWidth: 0, backgroundColor: "#f4f4f5" }}
      // O layout principal já reserva o cabeçalho e a barra inferior.
      behavior={Platform.OS === "ios" ? "padding" : undefined}
    >
      <ScrollView
        style={{ flex: 1 }}
        contentContainerStyle={{
          padding: isMobile ? 16 : 32,
          paddingBottom: 32,
        }}
        keyboardShouldPersistTaps="handled"
        keyboardDismissMode={Platform.OS === "ios" ? "interactive" : "on-drag"}
        showsVerticalScrollIndicator={false}
      >
        <View style={{ marginBottom: 24 }}>
          <Text
            className="font-bold text-zinc-900 mb-1"
            style={{ fontSize: isMobile ? 26 : 30 }}
          >
            Dados da Empresa
          </Text>
          <Text className="text-zinc-500 text-sm">
            Informações institucionais exibidas em documentos e orçamentos
          </Text>
        </View>
        <View
          onLayout={({ nativeEvent }) =>
            setContentWidth(nativeEvent.layout.width)
          }
          style={{ minWidth: 0 }}
        >
          <View
            style={{
              flexDirection: sideBySide ? "row" : "column",
              gap: isMobile ? 16 : 24,
              alignItems: "stretch",
            }}
          >
            <View
              style={{
                flex: sideBySide ? 2 : undefined,
                minWidth: 0,
                gap: isMobile ? 16 : 24,
              }}
            >
              <View
                className="bg-white rounded-2xl border border-zinc-100 shadow-sm"
                style={{ padding: isMobile ? 16 : 24 }}
              >
                <Text className="text-lg font-bold text-zinc-900 mb-5">
                  Identificação
                </Text>
                <View
                  style={{
                    flexDirection: isMobile ? "column" : "row",
                    gap: 16,
                    marginBottom: 16,
                  }}
                >
                  <View style={{ flex: isMobile ? undefined : 1, minWidth: 0 }}>
                    <Text className="text-sm font-semibold text-zinc-700 mb-2">
                      Razão Social / Nome da Empresa
                    </Text>
                    <TextInput
                      value={razaoSocial}
                      onChangeText={setRazaoSocial}
                      accessibilityLabel="Razão Social / Nome da Empresa"
                      style={{ minHeight: 48 }}
                      className="w-full border border-zinc-200 rounded-xl px-4 py-3 bg-white text-zinc-900 text-base"
                    />
                  </View>
                </View>
                <View
                  style={{
                    flexDirection: isMobile ? "column" : "row",
                    gap: 16,
                    marginBottom: 16,
                  }}
                >
                  <View style={{ flex: isMobile ? undefined : 1, minWidth: 0 }}>
                    <Text className="text-sm font-semibold text-zinc-700 mb-2">
                      CNPJ
                    </Text>
                    <TextInput
                      value={cnpj}
                      onChangeText={setCnpj}
                      accessibilityLabel="CNPJ"
                      autoCorrect={false}
                      style={{ minHeight: 48 }}
                      className="w-full border border-zinc-200 rounded-xl px-4 py-3 bg-white text-zinc-900 text-base"
                    />
                  </View>
                  <View style={{ flex: isMobile ? undefined : 1, minWidth: 0 }}>
                    <Text className="text-sm font-semibold text-zinc-700 mb-2">
                      Site
                    </Text>
                    <TextInput
                      value={site}
                      onChangeText={setSite}
                      accessibilityLabel="Site"
                      keyboardType="url"
                      autoCapitalize="none"
                      autoCorrect={false}
                      style={{ minHeight: 48 }}
                      className="w-full border border-zinc-200 rounded-xl px-4 py-3 bg-white text-zinc-900 text-base"
                    />
                  </View>
                </View>
                <View
                  style={{
                    flexDirection: isMobile ? "column" : "row",
                    gap: 16,
                    marginBottom: 0,
                  }}
                >
                  <View style={{ flex: isMobile ? undefined : 1, minWidth: 0 }}>
                    <Text className="text-sm font-semibold text-zinc-700 mb-2">
                      E-mail de contato
                    </Text>
                    <TextInput
                      value={email}
                      onChangeText={setEmail}
                      accessibilityLabel="E-mail de contato"
                      keyboardType="email-address"
                      autoCapitalize="none"
                      autoCorrect={false}
                      style={{ minHeight: 48 }}
                      className="w-full border border-zinc-200 rounded-xl px-4 py-3 bg-white text-zinc-900 text-base"
                    />
                  </View>
                  <View style={{ flex: isMobile ? undefined : 1, minWidth: 0 }}>
                    <Text className="text-sm font-semibold text-zinc-700 mb-2">
                      Telefone
                    </Text>
                    <TextInput
                      value={telefone}
                      onChangeText={setTelefone}
                      accessibilityLabel="Telefone"
                      keyboardType="phone-pad"
                      style={{ minHeight: 48 }}
                      className="w-full border border-zinc-200 rounded-xl px-4 py-3 bg-white text-zinc-900 text-base"
                    />
                  </View>
                </View>
              </View>
              <View
                className="bg-white rounded-2xl border border-zinc-100 shadow-sm"
                style={{ padding: isMobile ? 16 : 24 }}
              >
                <Text className="text-lg font-bold text-zinc-900 mb-5">
                  Endereço
                </Text>
                <View
                  style={{
                    flexDirection: isMobile ? "column" : "row",
                    gap: 16,
                    marginBottom: 16,
                  }}
                >
                  <View style={{ flex: isMobile ? undefined : 1, minWidth: 0 }}>
                    <Text className="text-sm font-semibold text-zinc-700 mb-2">
                      Logradouro
                    </Text>
                    <TextInput
                      value={logradouro}
                      onChangeText={setLogradouro}
                      accessibilityLabel="Logradouro"
                      style={{ minHeight: 48 }}
                      className="w-full border border-zinc-200 rounded-xl px-4 py-3 bg-white text-zinc-900 text-base"
                    />
                  </View>
                </View>
                <View
                  style={{
                    flexDirection: isMobile ? "column" : "row",
                    gap: 16,
                    marginBottom: 0,
                  }}
                >
                  <View style={{ flex: isMobile ? undefined : 2, minWidth: 0 }}>
                    <Text className="text-sm font-semibold text-zinc-700 mb-2">
                      Cidade
                    </Text>
                    <TextInput
                      value={cidade}
                      onChangeText={setCidade}
                      accessibilityLabel="Cidade"
                      style={{ minHeight: 48 }}
                      className="w-full border border-zinc-200 rounded-xl px-4 py-3 bg-white text-zinc-900 text-base"
                    />
                  </View>
                  <View style={{ flex: isMobile ? undefined : 1, minWidth: 0 }}>
                    <Text className="text-sm font-semibold text-zinc-700 mb-2">
                      Estado
                    </Text>
                    <TextInput
                      value={estado}
                      onChangeText={setEstado}
                      accessibilityLabel="Estado"
                      autoCapitalize="characters"
                      autoCorrect={false}
                      style={{ minHeight: 48 }}
                      className="w-full border border-zinc-200 rounded-xl px-4 py-3 bg-white text-zinc-900 text-base"
                    />
                  </View>
                  <View style={{ flex: isMobile ? undefined : 1, minWidth: 0 }}>
                    <Text className="text-sm font-semibold text-zinc-700 mb-2">
                      CEP
                    </Text>
                    <TextInput
                      value={cep}
                      onChangeText={setCep}
                      accessibilityLabel="CEP"
                      keyboardType="numeric"
                      style={{ minHeight: 48 }}
                      className="w-full border border-zinc-200 rounded-xl px-4 py-3 bg-white text-zinc-900 text-base"
                    />
                  </View>
                </View>
              </View>
            </View>
            <View
              style={{
                flex: sideBySide ? 1 : undefined,
                minWidth: 0,
                gap: isMobile ? 16 : 24,
              }}
            >
              <View
                className="bg-white rounded-2xl border border-zinc-100 shadow-sm"
                style={{ padding: isMobile ? 16 : 24 }}
              >
                <Text className="text-lg font-bold text-zinc-900 mb-5">
                  Logotipo da Empresa
                </Text>
                {/* O upload ainda não tem implementação no arquivo original. */}
                <TouchableOpacity
                  accessibilityRole="button"
                  accessibilityLabel="Enviar logotipo da empresa"
                  className="border-2 border-dashed border-[#8c5230]/30 bg-[#8c5230]/5 rounded-xl items-center justify-center hover:bg-[#8c5230]/10 transition-colors"
                  style={{ padding: isMobile ? 20 : 32, minHeight: 150 }}
                >
                  <View className="w-12 h-12 bg-[#8c5230]/10 rounded-xl items-center justify-center mb-3">
                    <Ionicons
                      name="cloud-upload-outline"
                      size={24}
                      color="#8c5230"
                    />
                  </View>
                  <Text className="text-[#8c5230] font-medium text-sm mb-1 text-center">
                    {isMobile ? "Toque para enviar" : "Clique para enviar"}
                  </Text>
                  <Text className="text-zinc-400 text-xs text-center">
                    PNG, JPG ou SVG — máx. 2MB
                  </Text>
                </TouchableOpacity>
              </View>
              <View
                className="bg-white rounded-2xl border border-zinc-100 shadow-sm"
                style={{ padding: isMobile ? 16 : 24 }}
              >
                <Text className="text-lg font-bold text-zinc-900 mb-2">
                  Rodapé do Documento
                </Text>
                <Text className="text-sm text-zinc-500 mb-5 leading-relaxed">
                  Texto exibido no rodapé de orçamentos e documentos gerados.
                </Text>
                <TextInput
                  value={rodape}
                  onChangeText={setRodape}
                  accessibilityLabel="Rodapé do documento"
                  multiline
                  numberOfLines={4}
                  textAlignVertical="top"
                  style={{ minHeight: 120 }}
                  className="w-full border border-zinc-200 rounded-xl px-4 py-3 bg-white text-zinc-700 text-base"
                />
              </View>
            </View>
          </View>
          <View
            style={{
              alignItems: isMobile ? "stretch" : "flex-end",
              marginTop: 24,
            }}
          >
            <TouchableOpacity
              onPress={handleSave}
              disabled={isLoading}
              accessibilityRole="button"
              accessibilityLabel={
                isLoading ? "Salvando alterações" : "Salvar alterações"
              }
              accessibilityState={{ disabled: isLoading, busy: isLoading }}
              style={{
                minHeight: 52,
                minWidth: isMobile ? undefined : 180,
                opacity: isLoading ? 0.7 : 1,
              }}
              className="bg-[#8c5230] px-6 py-3.5 rounded-xl flex-row items-center justify-center hover:bg-[#6f4f28] transition-colors shadow-sm"
            >
              {isLoading ? (
                <ActivityIndicator color="#ffffff" />
              ) : (
                <Text className="text-white font-bold text-sm tracking-wide">
                  Salvar Alterações
                </Text>
              )}
            </TouchableOpacity>
          </View>
        </View>
      </ScrollView>
    </KeyboardAvoidingView>
  );
}
