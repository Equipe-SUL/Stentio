import React, { useMemo, useState } from "react";
import { Modal, Pressable, ScrollView, Text, TextInput, View } from "react-native";
import { Ionicons } from "@expo/vector-icons";
import type { EtapaWorkflow, RecursoOption, TipoServicoOption } from "./types";

interface ModalAlocarRecursoProps {
  visible: boolean;
  etapa: EtapaWorkflow | null;
  recursos: RecursoOption[];
  tiposServico: TipoServicoOption[];
  onFechar: () => void;
  onConfirmarAlocacao: (recurso: RecursoOption) => void;
}

export function ModalAlocarRecurso({
  visible,
  etapa,
  recursos,
  tiposServico,
  onFechar,
  onConfirmarAlocacao,
}: ModalAlocarRecursoProps) {
  const [busca, setBusca] = useState("");

  const tipoServicoNome = useMemo(() => {
    if (!etapa) return "";
    const achado = tiposServico.find((t) => t.id === etapa.tipo_servico_id);
    return etapa.tipo_servico_nome || achado?.nome || "Serviço";
  }, [etapa, tiposServico]);

  const recursosFiltrados = useMemo(() => {
    const termo = busca.trim().toLowerCase();
    return recursos.filter((r) => {
      const matchTexto =
        !termo ||
        r.nome.toLowerCase().includes(termo) ||
        (r.email && r.email.toLowerCase().includes(termo));
      return matchTexto;
    });
  }, [recursos, busca]);

  if (!visible || !etapa) return null;

  return (
    <Modal visible={visible} transparent animationType="fade" onRequestClose={onFechar}>
      <View className="flex-1 items-center justify-center bg-black/60 p-4">
        <View className="w-full max-w-lg rounded-2xl bg-[#fbfaf8] border border-neutral-200 shadow-2xl overflow-hidden max-h-[85%] flex-col">
          {/* Cabeçalho */}
          <View className="flex-row items-center justify-between border-b border-neutral-200 bg-white px-6 py-4">
            <View className="flex-1 mr-3">
              <Text className="text-xs font-bold uppercase tracking-wider text-[#8c5230]">
                Alocação de Recurso
              </Text>
              <Text className="text-base font-bold text-neutral-900 truncate" numberOfLines={1}>
                Etapa {etapa.ordem}: {etapa.nome || "Sem nome"}
              </Text>
              <Text className="text-xs text-neutral-500 mt-0.5">
                Tipo necessário: <Text className="font-semibold text-neutral-800">{tipoServicoNome}</Text>
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

          {/* Campo de Busca */}
          <View className="p-4 bg-white border-b border-neutral-100">
            <View className="flex-row items-center gap-2 rounded-xl border border-neutral-300 bg-neutral-50 px-3 py-2">
              <Ionicons name="search" size={16} color="#a3a3a3" />
              <TextInput
                value={busca}
                onChangeText={setBusca}
                placeholder="Buscar por nome ou e-mail do recurso..."
                placeholderTextColor="#a3a3a3"
                className="flex-1 py-0 text-xs text-neutral-800"
              />
              {busca ? (
                <Pressable onPress={() => setBusca("")} hitSlop={6}>
                  <Ionicons name="close-circle" size={14} color="#a3a3a3" />
                </Pressable>
              ) : null}
            </View>
          </View>

          {/* Lista de Recursos */}
          <ScrollView className="flex-1 p-4" showsVerticalScrollIndicator={true}>
            {recursosFiltrados.length === 0 ? (
              <View className="items-center justify-center py-10">
                <Ionicons name="people-outline" size={32} color="#d4d4d4" />
                <Text className="text-sm font-medium text-neutral-500 mt-2">
                  Nenhum recurso encontrado
                </Text>
                <Text className="text-xs text-neutral-400 mt-0.5 text-center">
                  Verifique o termo digitado ou cadastre profissionais na aba Recursos.
                </Text>
              </View>
            ) : (
              <View className="gap-2.5">
                {recursosFiltrados.map((recurso) => {
                  const isAtual = etapa.recurso_id === recurso.id;
                  const temEspecialidade = recurso.tiposServico?.some(
                    (ts) => ts.id === etapa.tipo_servico_id || ts.nome === tipoServicoNome,
                  );

                  return (
                    <Pressable
                      key={recurso.id}
                      onPress={() => onConfirmarAlocacao(recurso)}
                      className={`flex-row items-center justify-between rounded-xl border p-3 transition-colors ${
                        isAtual
                          ? "border-emerald-400 bg-emerald-50/60"
                          : "border-neutral-200 bg-white hover:border-[#8c5230]/40 hover:bg-neutral-50"
                      }`}
                    >
                      <View className="flex-row items-center gap-3 flex-1 mr-3 min-w-0">
                        <View
                          className={`h-10 w-10 rounded-full items-center justify-center ${
                            isAtual ? "bg-emerald-200 text-emerald-800" : "bg-[#f6efe9]"
                          }`}
                        >
                          <Ionicons
                            name="person"
                            size={18}
                            color={isAtual ? "#047857" : "#8c5230"}
                          />
                        </View>
                        <View className="flex-1 min-w-0">
                          <View className="flex-row items-center gap-2 flex-wrap">
                            <Text className="text-sm font-bold text-neutral-900 truncate">
                              {recurso.nome}
                            </Text>
                            {temEspecialidade ? (
                              <View className="rounded-full bg-blue-50 border border-blue-200 px-2 py-0.2">
                                <Text className="text-[10px] font-semibold text-blue-700">
                                  Habilitado em {tipoServicoNome}
                                </Text>
                              </View>
                            ) : null}
                          </View>
                          {recurso.email ? (
                            <Text className="text-xs text-neutral-500 truncate mt-0.5">
                              {recurso.email}
                            </Text>
                          ) : null}
                        </View>
                      </View>

                      <View className="flex-row items-center gap-2">
                        {isAtual ? (
                          <View className="flex-row items-center gap-1 rounded-lg bg-emerald-100 px-2.5 py-1">
                            <Ionicons name="checkmark-circle" size={14} color="#047857" />
                            <Text className="text-xs font-bold text-emerald-800">Alocado</Text>
                          </View>
                        ) : (
                          <View className="rounded-lg border border-neutral-300 bg-neutral-100/70 px-3 py-1.5 hover:bg-[#6f4f28] hover:text-white">
                            <Text className="text-xs font-semibold text-neutral-700">
                              Selecionar
                            </Text>
                          </View>
                        )}
                      </View>
                    </Pressable>
                  );
                })}
              </View>
            )}
          </ScrollView>

          {/* Rodapé */}
          <View className="border-t border-neutral-200 bg-white p-4 flex-row justify-end gap-3">
            <Pressable
              onPress={onFechar}
              className="rounded-lg border border-neutral-300 bg-white px-4 py-2 hover:bg-neutral-50"
            >
              <Text className="text-xs font-semibold text-neutral-700">Fechar</Text>
            </Pressable>
          </View>
        </View>
      </View>
    </Modal>
  );
}
