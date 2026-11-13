import React, { useState } from "react";
import { ActivityIndicator, Modal, Pressable, ScrollView, Text, View } from "react-native";
import { Ionicons } from "@expo/vector-icons";
import type { ModeloWorkflow } from "./types";

interface ModalCarregarModeloProps {
  visible: boolean;
  modelos: ModeloWorkflow[];
  carregando: boolean;
  onFechar: () => void;
  onAplicarModelo: (modeloId: string) => Promise<void>;
}

export function ModalCarregarModelo({
  visible,
  modelos,
  carregando,
  onFechar,
  onAplicarModelo,
}: ModalCarregarModeloProps) {
  const [modeloSelecionadoId, setModeloSelecionadoId] = useState<string | null>(null);
  const [aplicando, setAplicando] = useState(false);
  const [erro, setErro] = useState<string | null>(null);

  const modeloSelecionado = modelos.find((m) => m.id === modeloSelecionadoId);

  const handleAplicar = async () => {
    if (!modeloSelecionadoId) return;
    try {
      setAplicando(true);
      setErro(null);
      await onAplicarModelo(modeloSelecionadoId);
      onFechar();
    } catch (err: any) {
      setErro(err?.message || "Não foi possível aplicar o modelo.");
    } finally {
      setAplicando(false);
    }
  };

  if (!visible) return null;

  return (
    <Modal visible={visible} transparent animationType="fade" onRequestClose={onFechar}>
      <View className="flex-1 items-center justify-center bg-black/60 p-4">
        <View className="w-full max-w-2xl rounded-2xl bg-[#fbfaf8] border border-neutral-200 shadow-2xl overflow-hidden max-h-[90%] flex-col">
          {/* Cabeçalho */}
          <View className="flex-row items-center justify-between border-b border-neutral-200 bg-white px-6 py-4">
            <View className="flex-1 mr-4">
              <Text className="text-xs font-bold uppercase tracking-wider text-[#8c5230]">
                Modelos de Workflow Pré-definidos
              </Text>
              <Text className="text-lg font-bold text-neutral-900">
                Carregar Modelo de Workflow
              </Text>
              <Text className="text-xs text-neutral-500 mt-0.5">
                Selecione um molde de etapas padronizado para clonar diretamente nesta Ordem de Serviço.
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

          {/* Conteúdo com Lista de Modelos e Pré-visualização */}
          <View className="flex-1 p-6 flex-col md:flex-row gap-6 overflow-hidden">
            {/* Coluna da Esquerda: Lista de Modelos */}
            <View className="w-full md:w-1/2 flex-col">
              <Text className="text-xs font-bold text-neutral-500 uppercase tracking-wider mb-2">
                Modelos Disponíveis ({modelos.length})
              </Text>

              {carregando ? (
                <View className="py-12 items-center justify-center">
                  <ActivityIndicator size="small" color="#6f4f28" />
                  <Text className="text-xs text-neutral-500 mt-2">Buscando modelos...</Text>
                </View>
              ) : modelos.length === 0 ? (
                <View className="py-10 items-center justify-center rounded-xl border border-dashed border-neutral-300 p-4">
                  <Ionicons name="folder-open-outline" size={28} color="#a3a3a3" />
                  <Text className="text-xs text-neutral-600 font-semibold mt-2 text-center">
                    Nenhum modelo cadastrado
                  </Text>
                  <Text className="text-[11px] text-neutral-400 text-center mt-1">
                    Você pode salvar o workflow atual como modelo usando o botão &quot;Salvar Modelo&quot;.
                  </Text>
                </View>
              ) : (
                <ScrollView className="flex-1 pr-1" showsVerticalScrollIndicator={true}>
                  <View className="gap-2.5">
                    {modelos.map((m) => {
                      const isSel = m.id === modeloSelecionadoId;
                      return (
                        <Pressable
                          key={m.id}
                          onPress={() => setModeloSelecionadoId(m.id)}
                          className={`rounded-xl border p-3 transition-colors ${
                            isSel
                              ? "border-[#8c5230] bg-[#fdf8f4] shadow-xs"
                              : "border-neutral-200 bg-white hover:bg-neutral-50"
                          }`}
                        >
                          <View className="flex-row items-center justify-between">
                            <Text
                              className={`text-sm font-bold ${
                                isSel ? "text-[#8c5230]" : "text-neutral-900"
                              }`}
                            >
                              {m.nome}
                            </Text>
                            <View className="rounded-full bg-neutral-100 px-2 py-0.5 border border-neutral-200">
                              <Text className="text-[10px] font-semibold text-neutral-600">
                                {m.etapas?.length || 0} etapas
                              </Text>
                            </View>
                          </View>
                          {m.descricao ? (
                            <Text
                              className="text-xs text-neutral-500 mt-1 line-clamp-2"
                              numberOfLines={2}
                            >
                              {m.descricao}
                            </Text>
                          ) : null}
                        </Pressable>
                      );
                    })}
                  </View>
                </ScrollView>
              )}
            </View>

            {/* Coluna da Direita: Pré-visualização do Modelo Selecionado */}
            <View className="w-full md:w-1/2 flex-col rounded-xl border border-neutral-200 bg-white p-4">
              <Text className="text-xs font-bold text-neutral-500 uppercase tracking-wider mb-2">
                Pré-visualização do Molde
              </Text>

              {modeloSelecionado ? (
                <View className="flex-1 flex-col">
                  <View className="border-b border-neutral-100 pb-2 mb-3">
                    <Text className="text-base font-bold text-neutral-900">
                      {modeloSelecionado.nome}
                    </Text>
                    {modeloSelecionado.descricao ? (
                      <Text className="text-xs text-neutral-600 mt-0.5">
                        {modeloSelecionado.descricao}
                      </Text>
                    ) : null}
                  </View>

                  <ScrollView className="flex-1 pr-1">
                    <View className="gap-2">
                      {modeloSelecionado.etapas?.map((etapa) => (
                        <View
                          key={etapa.id || etapa.ordem}
                          className="flex-row items-center justify-between rounded-lg border border-neutral-100 bg-neutral-50/70 p-2.5"
                        >
                          <View className="flex-row items-center gap-2 flex-1 mr-2 min-w-0">
                            <View className="h-6 w-6 rounded-full bg-[#f6efe9] items-center justify-center">
                              <Text className="text-[11px] font-bold text-[#8c5230]">
                                {etapa.ordem}
                              </Text>
                            </View>
                            <View className="flex-1 min-w-0">
                              <Text
                                className="text-xs font-bold text-neutral-800 truncate"
                                numberOfLines={1}
                              >
                                {etapa.nome}
                              </Text>
                              <Text className="text-[10px] text-neutral-500 truncate">
                                {etapa.tipo_servico_nome || "Tipo de serviço"}
                              </Text>
                            </View>
                          </View>

                          <View className="rounded bg-neutral-200/80 px-2 py-0.5">
                            <Text className="text-[11px] font-mono font-semibold text-neutral-700">
                              {etapa.prazo_estimado_horas}h
                            </Text>
                          </View>
                        </View>
                      ))}
                    </View>
                  </ScrollView>
                </View>
              ) : (
                <View className="flex-1 items-center justify-center py-10">
                  <Ionicons name="layers-outline" size={32} color="#d4d4d4" />
                  <Text className="text-xs text-neutral-400 mt-2 text-center">
                    Selecione um modelo à esquerda para inspecionar as etapas antes de aplicar.
                  </Text>
                </View>
              )}
            </View>
          </View>

          {/* Rodapé com Ações */}
          <View className="border-t border-neutral-200 bg-white p-4 flex-row justify-between items-center">
            <Text className="text-xs text-neutral-400">
              * A aplicação do modelo substitui a sequência atual da OS.
            </Text>

            <View className="flex-row gap-3">
              <Pressable
                onPress={onFechar}
                disabled={aplicando}
                className="rounded-lg border border-neutral-300 bg-white px-4 py-2 hover:bg-neutral-50"
              >
                <Text className="text-xs font-semibold text-neutral-700">Cancelar</Text>
              </Pressable>

              <Pressable
                onPress={handleAplicar}
                disabled={!modeloSelecionadoId || aplicando}
                className={`flex-row items-center gap-2 rounded-lg px-4 py-2 ${
                  !modeloSelecionadoId || aplicando
                    ? "bg-neutral-300 opacity-60"
                    : "bg-[#6f4f28] active:bg-[#5b4020]"
                }`}
              >
                {aplicando ? (
                  <ActivityIndicator size="small" color="#ffffff" />
                ) : (
                  <Ionicons name="copy-outline" size={16} color="#ffffff" />
                )}
                <Text className="text-xs font-bold text-white">
                  {aplicando ? "Clonando Etapas..." : "Aplicar este Modelo"}
                </Text>
              </Pressable>
            </View>
          </View>
        </View>
      </View>
    </Modal>
  );
}
