import React, { useState } from "react";
import { ActivityIndicator, Modal, Pressable, ScrollView, Text, TextInput, View } from "react-native";
import { Ionicons } from "@expo/vector-icons";
import type { EtapaWorkflow, TipoServicoOption } from "./types";

interface ModalSalvarModeloProps {
  visible: boolean;
  etapas: EtapaWorkflow[];
  tiposServico: TipoServicoOption[];
  onFechar: () => void;
  onConfirmarSalvar: (dados: { nome: string; descricao?: string }) => Promise<void>;
}

export function ModalSalvarModelo({
  visible,
  etapas,
  tiposServico,
  onFechar,
  onConfirmarSalvar,
}: ModalSalvarModeloProps) {
  const [nome, setNome] = useState("");
  const [descricao, setDescricao] = useState("");
  const [salvando, setSalvando] = useState(false);
  const [erro, setErro] = useState<string | null>(null);

  const handleSalvar = async () => {
    if (!nome.trim()) {
      setErro("Informe o nome do modelo.");
      return;
    }
    try {
      setSalvando(true);
      setErro(null);
      await onConfirmarSalvar({ nome: nome.trim(), descricao: descricao.trim() || undefined });
      setNome("");
      setDescricao("");
      onFechar();
    } catch (err: any) {
      setErro(err?.message || "Erro ao salvar modelo de workflow.");
    } finally {
      setSalvando(false);
    }
  };

  if (!visible) return null;

  return (
    <Modal visible={visible} transparent animationType="fade" onRequestClose={onFechar}>
      <View className="flex-1 items-center justify-center bg-black/60 p-4">
        <View className="w-full max-w-lg rounded-2xl bg-[#fbfaf8] border border-neutral-200 shadow-2xl overflow-hidden max-h-[90%] flex-col">
          {/* Cabeçalho */}
          <View className="flex-row items-center justify-between border-b border-neutral-200 bg-white px-6 py-4">
            <View className="flex-1 mr-4">
              <Text className="text-xs font-bold uppercase tracking-wider text-[#8c5230]">
                Padronização de Processos
              </Text>
              <Text className="text-lg font-bold text-neutral-900">
                Salvar como Modelo de Workflow
              </Text>
              <Text className="text-xs text-neutral-500 mt-0.5">
                Crie um modelo a partir das etapas atuais para reaproveitar em outras Ordens de Serviço.
              </Text>
            </View>

            <Pressable
              onPress={onFechar}
              hitSlop={8}
              className="rounded-lg p-1.5 hover:bg-neutral-100"
            >
              <Ionicons name="close" size={22} color="#737373" />
            </Pressable>
          </View>

          {erro ? (
            <View className="mx-6 mt-4 flex-row items-center gap-2 rounded-xl bg-red-50 border border-red-200 p-3">
              <Ionicons name="alert-circle" size={18} color="#dc2626" />
              <Text className="text-xs font-medium text-red-800 flex-1">{erro}</Text>
            </View>
          ) : null}

          {/* Formulário */}
          <ScrollView className="flex-1 p-6" showsVerticalScrollIndicator={true}>
            <View className="gap-4">
              <View className="gap-1.5">
                <Text className="text-xs font-bold text-neutral-700">
                  Nome do Modelo <Text className="text-red-500">*</Text>
                </Text>
                <TextInput
                  value={nome}
                  onChangeText={(val) => {
                    setNome(val);
                    if (erro) setErro(null);
                  }}
                  placeholder="Ex: Tradução Jurídica Padrão..."
                  placeholderTextColor="#a3a3a3"
                  className="rounded-xl border border-neutral-300 bg-white p-3 text-sm text-neutral-900 focus:border-[#8c5230]"
                />
              </View>

              <View className="gap-1.5">
                <Text className="text-xs font-bold text-neutral-700">Descrição (Opcional)</Text>
                <TextInput
                  value={descricao}
                  onChangeText={setDescricao}
                  placeholder="Descreva quando este fluxo deve ser adotado..."
                  placeholderTextColor="#a3a3a3"
                  multiline
                  numberOfLines={3}
                  className="rounded-xl border border-neutral-300 bg-white p-3 text-sm text-neutral-900 focus:border-[#8c5230]"
                  style={{ minHeight: 70 }}
                />
              </View>

              {/* Etapas que serão incluídas */}
              <View className="gap-2 mt-2">
                <Text className="text-xs font-bold text-neutral-500 uppercase tracking-wider">
                  Etapas a serem salvas ({etapas.length})
                </Text>

                <View className="rounded-xl border border-neutral-200 bg-white p-3 gap-2">
                  {etapas.map((etapa) => {
                    const tipoEncontrado = tiposServico.find(
                      (t) => t.id === etapa.tipo_servico_id,
                    );
                    const tipoNome =
                      etapa.tipo_servico_nome || tipoEncontrado?.nome || "Tipo";

                    return (
                      <View
                        key={etapa.ordem}
                        className="flex-row items-center justify-between border-b border-neutral-100 pb-2 last:border-b-0 last:pb-0"
                      >
                        <View className="flex-row items-center gap-2 flex-1 mr-2 min-w-0">
                          <View className="h-6 w-6 rounded-full bg-[#f6efe9] items-center justify-center">
                            <Text className="text-[11px] font-bold text-[#8c5230]">
                              {etapa.ordem}
                            </Text>
                          </View>
                          <Text
                            className="text-xs font-semibold text-neutral-800 truncate"
                            numberOfLines={1}
                          >
                            {etapa.nome}
                          </Text>
                        </View>

                        <View className="flex-row items-center gap-2">
                          <Text className="text-[11px] text-neutral-500">{tipoNome}</Text>
                          <View className="rounded bg-neutral-100 px-2 py-0.5">
                            <Text className="text-[11px] font-mono font-semibold text-neutral-700">
                              {etapa.prazo_estimado_horas}h
                            </Text>
                          </View>
                        </View>
                      </View>
                    );
                  })}
                </View>
              </View>
            </View>
          </ScrollView>

          {/* Rodapé */}
          <View className="border-t border-neutral-200 bg-white p-4 flex-row justify-end gap-3">
            <Pressable
              onPress={onFechar}
              disabled={salvando}
              className="rounded-lg border border-neutral-300 bg-white px-4 py-2 hover:bg-neutral-50"
            >
              <Text className="text-xs font-semibold text-neutral-700">Cancelar</Text>
            </Pressable>

            <Pressable
              onPress={handleSalvar}
              disabled={salvando || !nome.trim()}
              className={`flex-row items-center gap-2 rounded-lg px-4 py-2 ${
                salvando || !nome.trim()
                  ? "bg-neutral-300 opacity-60"
                  : "bg-[#6f4f28] active:bg-[#5b4020]"
              }`}
            >
              {salvando ? (
                <ActivityIndicator size="small" color="#ffffff" />
              ) : (
                <Ionicons name="save-outline" size={16} color="#ffffff" />
              )}
              <Text className="text-xs font-bold text-white">
                {salvando ? "Salvando Modelo..." : "Confirmar e Salvar Modelo"}
              </Text>
            </Pressable>
          </View>
        </View>
      </View>
    </Modal>
  );
}
