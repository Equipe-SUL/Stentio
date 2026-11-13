import React from "react";
import { View, Text, Pressable } from "react-native";
import { Ionicons } from "@expo/vector-icons";
import { EtapaContainer } from "./EtapaContainer";
import type { EtapaWorkflow, TipoServicoOption } from "./types";

interface TimelineEtapasProps {
  etapas: EtapaWorkflow[];
  tiposServico: TipoServicoOption[];
  bloqueadoPorRN3: boolean;
  onAdicionarEtapa: () => void;
  onSubirEtapa: (index: number) => void;
  onDescerEtapa: (index: number) => void;
  onRemoverEtapa: (index: number) => void;
  onAbrirAlocacao: (etapa: EtapaWorkflow) => void;
  onRemoverAlocacao: (etapa: EtapaWorkflow) => void;
  onAlternarConclusao: (index: number) => void;
  onAtualizarCampoEtapa: (index: number, campo: keyof EtapaWorkflow, valor: any) => void;
  onAbrirCarregarModelo: () => void;
}

export function TimelineEtapas({
  etapas,
  tiposServico,
  bloqueadoPorRN3,
  onAdicionarEtapa,
  onSubirEtapa,
  onDescerEtapa,
  onRemoverEtapa,
  onAbrirAlocacao,
  onRemoverAlocacao,
  onAlternarConclusao,
  onAtualizarCampoEtapa,
  onAbrirCarregarModelo,
}: TimelineEtapasProps) {
  const totalHoras = etapas.reduce(
    (acc, e) => acc + (Number(e.prazo_estimado_horas) || 0),
    0,
  );
  const totalConcluidas = etapas.filter((e) => e.status === "CONCLUIDA").length;
  const porcentagem =
    etapas.length > 0 ? Math.round((totalConcluidas / etapas.length) * 100) : 0;

  if (etapas.length === 0) {
    return (
      <View className="rounded-2xl border border-neutral-200 bg-white p-8 shadow-sm">
        <View className="items-center text-center max-w-lg mx-auto py-6">
          <View className="h-16 w-16 rounded-2xl bg-[#f6efe9] items-center justify-center mb-4 border border-[#e8d7c8]">
            <Ionicons name="git-network-outline" size={32} color="#8c5230" />
          </View>
          <Text className="text-xl font-bold font-serif text-neutral-900">
            Nenhuma etapa cadastrada no Workflow
          </Text>
          <Text className="text-sm text-neutral-600 mt-2 text-center leading-relaxed">
            Esta ordem de serviço ainda não possui um fluxo de trabalho definido. Você pode
            iniciar clonando um modelo pré-definido ou estruturando etapas avulsas manualmente.
          </Text>

          {/* Opções Iniciais (CA.1) */}
          <View className="mt-8 flex-col sm:flex-row gap-4 w-full">
            <Pressable
              onPress={onAbrirCarregarModelo}
              className="flex-1 flex-col items-center justify-center p-5 rounded-2xl border border-[#8c5230]/40 bg-[#fbf8f5] hover:bg-[#f6efe9] active:bg-[#ede2d6] transition-colors"
            >
              <Ionicons name="copy-outline" size={24} color="#8c5230" />
              <Text className="text-sm font-bold text-[#8c5230] mt-2">
                Carregar Modelo de Workflow
              </Text>
              <Text className="text-xs text-neutral-500 text-center mt-1">
                Clona um conjunto padronizado de etapas para a OS
              </Text>
            </Pressable>

            <Pressable
              onPress={onAdicionarEtapa}
              className="flex-1 flex-col items-center justify-center p-5 rounded-2xl border border-neutral-300 bg-white hover:bg-neutral-50 active:bg-neutral-100 transition-colors"
            >
              <Ionicons name="add-circle-outline" size={24} color="#525252" />
              <Text className="text-sm font-bold text-neutral-800 mt-2">
                Adicionar Etapas Manualmente
              </Text>
              <Text className="text-xs text-neutral-500 text-center mt-1">
                Monte e configure a sequência sob demanda
              </Text>
            </Pressable>
          </View>
        </View>
      </View>
    );
  }

  return (
    <View className="gap-6">
      {/* Barra de Resumo e Métricas da Timeline */}
      <View className="rounded-2xl border border-neutral-200 bg-white p-5 shadow-sm">
        <View className="flex-col md:flex-row md:items-center justify-between gap-4">
          <View className="flex-row items-center gap-6 flex-wrap">
            <View>
              <Text className="text-xs font-semibold text-neutral-400 uppercase tracking-wider">
                Total de Etapas
              </Text>
              <Text className="text-xl font-bold font-mono text-neutral-900 mt-0.5">
                {etapas.length} {etapas.length === 1 ? "etapa" : "etapas"}
              </Text>
            </View>

            <View className="h-8 w-px bg-neutral-200" />

            <View>
              <Text className="text-xs font-semibold text-neutral-400 uppercase tracking-wider">
                Prazo Total Estimado
              </Text>
              <View className="flex-row items-center gap-1.5 mt-0.5">
                <Ionicons name="time-outline" size={18} color="#8c5230" />
                <Text className="text-xl font-bold font-mono text-neutral-900">
                  {totalHoras}h
                </Text>
              </View>
            </View>

            <View className="h-8 w-px bg-neutral-200" />

            <View>
              <Text className="text-xs font-semibold text-neutral-400 uppercase tracking-wider">
                Progresso Concluído
              </Text>
              <Text className="text-xl font-bold font-mono text-emerald-700 mt-0.5">
                {totalConcluidas}/{etapas.length} ({porcentagem}%)
              </Text>
            </View>
          </View>

          {/* Botão para Inserir Nova Etapa */}
          <Pressable
            onPress={onAdicionarEtapa}
            disabled={bloqueadoPorRN3}
            className={`flex-row items-center gap-2 rounded-xl border border-[#8c5230] bg-[#fbf8f5] px-4 py-2.5 active:bg-[#f6efe9] ${
              bloqueadoPorRN3 ? "opacity-40" : ""
            }`}
            accessibilityRole="button"
            accessibilityLabel="Adicionar nova etapa no final da sequência"
          >
            <Ionicons name="add-circle" size={18} color="#8c5230" />
            <Text className="text-xs font-bold text-[#8c5230]">+ Adicionar Etapa</Text>
          </Pressable>
        </View>

        {/* Barra de Progresso Visual */}
        <View className="mt-4 pt-4 border-t border-neutral-100">
          <View className="h-2 w-full rounded-full bg-neutral-100 overflow-hidden">
            <View
              className="h-full bg-emerald-500 rounded-full transition-all"
              style={{ width: `${porcentagem}%` }}
            />
          </View>
        </View>

        {/* Alerta RN.3 se a sequência estiver congelada */}
        {bloqueadoPorRN3 ? (
          <View className="mt-3 flex-row items-center gap-2 rounded-xl bg-amber-50 border border-amber-200 p-2.5 px-3">
            <Ionicons name="lock-closed" size={16} color="#b45309" />
            <Text className="text-xs font-medium text-amber-900 flex-1">
              <Text className="font-bold">Sequência congelada:</Text> Uma ou mais etapas já
              possuem recursos alocados. A reordenação e exclusão estrutural estão desabilitadas.
            </Text>
          </View>
        ) : null}
      </View>

      {/* Lista de Containers na Timeline */}
      <View className="pl-1">
        {etapas.map((etapa, idx) => (
          <EtapaContainer
            key={etapa.id || `etapa_temp_${idx}`}
            etapa={etapa}
            index={idx}
            totalEtapas={etapas.length}
            isFirst={idx === 0}
            isLast={idx === etapas.length - 1}
            bloqueadoPorRN3={bloqueadoPorRN3}
            tiposServico={tiposServico}
            onSubir={() => onSubirEtapa(idx)}
            onDescer={() => onDescerEtapa(idx)}
            onRemover={() => onRemoverEtapa(idx)}
            onAlocarRecurso={() => onAbrirAlocacao(etapa)}
            onRemoverAlocacao={() => onRemoverAlocacao(etapa)}
            onAlternarConclusao={() => onAlternarConclusao(idx)}
            onAtualizarCampo={(campo, valor) => onAtualizarCampoEtapa(idx, campo, valor)}
          />
        ))}
      </View>
    </View>
  );
}
