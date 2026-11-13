import React from "react";
import { View, Text, Pressable, TextInput } from "react-native";
import { Ionicons } from "@expo/vector-icons";
import type { EtapaWorkflow, TipoServicoOption } from "./types";

interface EtapaContainerProps {
  etapa: EtapaWorkflow;
  index: number;
  totalEtapas: number;
  isFirst: boolean;
  isLast: boolean;
  bloqueadoPorRN3?: boolean;
  tiposServico: TipoServicoOption[];
  onSubir: () => void;
  onDescer: () => void;
  onRemover: () => void;
  onAlocarRecurso: () => void;
  onRemoverAlocacao: () => void;
  onAlternarConclusao: () => void;
  onAtualizarCampo: (campo: keyof EtapaWorkflow, valor: any) => void;
}

export function EtapaContainer({
  etapa,
  index,
  totalEtapas,
  isFirst,
  isLast,
  bloqueadoPorRN3 = false,
  tiposServico,
  onSubir,
  onDescer,
  onRemover,
  onAlocarRecurso,
  onRemoverAlocacao,
  onAlternarConclusao,
  onAtualizarCampo,
}: EtapaContainerProps) {
  const isConcluida = etapa.status === "CONCLUIDA";
  const isPendente = etapa.status === "PENDENTE";
  const isAguardando = etapa.status === "AGUARDANDO_ETAPA_ANTERIOR";

  // Obter nome legível do tipo de serviço
  const tipoEncontrado = tiposServico.find((t) => t.id === etapa.tipo_servico_id);
  const tipoNome = etapa.tipo_servico_nome || tipoEncontrado?.nome || "Tipo não definido";

  // Cores do nó indicador da timeline
  let badgeNoBg = "bg-amber-100 border-amber-300 text-amber-800";
  let statusRotulo = "Pronta / Pendente";
  let statusBadgeBg = "bg-amber-50 border-amber-200 text-amber-800";

  if (isConcluida) {
    badgeNoBg = "bg-emerald-500 border-emerald-600 text-white";
    statusRotulo = "Concluída";
    statusBadgeBg = "bg-emerald-50 border-emerald-200 text-emerald-800";
  } else if (isAguardando) {
    badgeNoBg = "bg-neutral-100 border-neutral-300 text-neutral-500";
    statusRotulo = "Aguardando anterior";
    statusBadgeBg = "bg-neutral-100 border-neutral-200 text-neutral-600";
  }

  return (
    <View className="flex-row items-stretch group">
      {/* Coluna da Linha da Timeline */}
      <View className="items-center mr-4 w-10 flex-col">
        {/* Linha superior conectora */}
        <View
          className={`w-0.5 flex-1 ${
            isFirst ? "bg-transparent" : isConcluida ? "bg-emerald-400" : "bg-neutral-300"
          }`}
          style={{ minHeight: 16 }}
        />

        {/* Círculo do Nó da Timeline com Ordem Sequencial */}
        <View
          className={`h-9 w-9 rounded-full border-2 items-center justify-center shadow-xs z-10 ${badgeNoBg}`}
        >
          {isConcluida ? (
            <Ionicons name="checkmark" size={18} color="#ffffff" />
          ) : (
            <Text className="text-xs font-bold font-mono">{etapa.ordem}</Text>
          )}
        </View>

        {/* Linha inferior conectora */}
        <View
          className={`w-0.5 flex-1 ${
            isLast ? "bg-transparent" : isConcluida ? "bg-emerald-400" : "bg-neutral-300"
          }`}
          style={{ minHeight: 16 }}
        />
      </View>

      {/* Cartão Container da Etapa */}
      <View
        className={`flex-1 mb-6 rounded-2xl border bg-white p-5 shadow-sm transition-all ${
          isConcluida
            ? "border-emerald-200 bg-emerald-50/15"
            : isPendente
            ? "border-amber-200 shadow-amber-50"
            : "border-neutral-200"
        }`}
      >
        {/* Linha 1: Cabeçalho do Container (Ordem, Nome da Etapa, Badges e Ações de Ordenação) */}
        <View className="flex-col md:flex-row md:items-center justify-between gap-3 border-b border-neutral-100 pb-3">
          <View className="flex-1 flex-row items-center gap-2.5 flex-wrap">
            <View className="flex-row items-center gap-1.5 rounded-lg bg-neutral-100 px-2.5 py-1">
              <Ionicons name="git-commit-outline" size={14} color="#737373" />
              <Text className="text-xs font-semibold text-neutral-700 font-mono">
                Etapa {etapa.ordem}
              </Text>
            </View>

            {/* Input para Nome da Etapa */}
            <TextInput
              value={etapa.nome}
              onChangeText={(txt) => onAtualizarCampo("nome", txt)}
              placeholder="Nome da etapa (ex: Tradução Principal)..."
              placeholderTextColor="#a3a3a3"
              className={`flex-1 min-w-[200px] text-base font-bold text-neutral-900 border-b border-transparent hover:border-neutral-300 focus:border-[#8c5230] py-0.5 px-1 rounded ${
                isConcluida ? "line-through text-neutral-500" : ""
              }`}
            />

            {/* Badges de Tipo de Serviço e Status */}
            <View className="flex-row items-center gap-2">
              <View className="rounded-full bg-[#f6efe9] border border-[#e8d7c8] px-2.5 py-0.5">
                <Text className="text-[11px] font-semibold text-[#8c5230]">{tipoNome}</Text>
              </View>

              <View className={`rounded-full border px-2.5 py-0.5 ${statusBadgeBg}`}>
                <Text className="text-[11px] font-semibold">{statusRotulo}</Text>
              </View>
            </View>
          </View>

          {/* Botões de Ação de Ordenação e Remoção */}
          <View className="flex-row items-center gap-1 self-end md:self-auto">
            {/* Botão Subir na Timeline */}
            <Pressable
              onPress={onSubir}
              disabled={isFirst || bloqueadoPorRN3}
              className={`h-8 w-8 items-center justify-center rounded-lg border ${
                isFirst || bloqueadoPorRN3
                  ? "border-neutral-200 bg-neutral-100 opacity-40"
                  : "border-neutral-300 bg-white hover:bg-neutral-50 active:bg-neutral-100"
              }`}
              accessibilityRole="button"
              accessibilityLabel={`Mover etapa ${etapa.ordem} para cima`}
            >
              <Ionicons name="arrow-up" size={16} color={isFirst ? "#a3a3a3" : "#525252"} />
            </Pressable>

            {/* Botão Descer na Timeline */}
            <Pressable
              onPress={onDescer}
              disabled={isLast || bloqueadoPorRN3}
              className={`h-8 w-8 items-center justify-center rounded-lg border ${
                isLast || bloqueadoPorRN3
                  ? "border-neutral-200 bg-neutral-100 opacity-40"
                  : "border-neutral-300 bg-white hover:bg-neutral-50 active:bg-neutral-100"
              }`}
              accessibilityRole="button"
              accessibilityLabel={`Mover etapa ${etapa.ordem} para baixo`}
            >
              <Ionicons name="arrow-down" size={16} color={isLast ? "#a3a3a3" : "#525252"} />
            </Pressable>

            {/* Botão Excluir Etapa */}
            <Pressable
              onPress={onRemover}
              disabled={bloqueadoPorRN3 || totalEtapas <= 1}
              className={`h-8 w-8 items-center justify-center rounded-lg border ${
                bloqueadoPorRN3 || totalEtapas <= 1
                  ? "border-neutral-200 bg-neutral-100 opacity-40"
                  : "border-red-200 bg-red-50 hover:bg-red-100 active:bg-red-200"
              }`}
              accessibilityRole="button"
              accessibilityLabel={`Remover etapa ${etapa.ordem}`}
            >
              <Ionicons name="trash-outline" size={15} color="#dc2626" />
            </Pressable>
          </View>
        </View>

        {/* Linha 2: Configurações da Etapa (Tipo de Serviço, Prazo Estimado, Alocação e Conclusão) */}
        <View className="mt-4 grid grid-cols-1 md:grid-cols-3 gap-4">
          {/* Seletor do Tipo de Serviço */}
          <View className="gap-1.5">
            <Text className="text-xs font-semibold text-neutral-600">Tipo de Serviço</Text>
            <View className="rounded-xl border border-neutral-300 bg-neutral-50/60 px-3 py-2">
              <select
                value={etapa.tipo_servico_id}
                onChange={(e) => onAtualizarCampo("tipo_servico_id", e.target.value)}
                disabled={bloqueadoPorRN3}
                style={{
                  width: "100%",
                  border: "none",
                  backgroundColor: "transparent",
                  outline: "none",
                  fontSize: 13,
                  fontWeight: 500,
                  color: "#262626",
                  cursor: bloqueadoPorRN3 ? "not-allowed" : "pointer",
                }}
              >
                <option value="">Selecione o serviço...</option>
                {tiposServico.map((tipo) => (
                  <option key={tipo.id} value={tipo.id}>
                    {tipo.nome}
                  </option>
                ))}
              </select>
            </View>
          </View>

          {/* Prazo Estimado em Horas (RN.2: > 0) */}
          <View className="gap-1.5">
            <View className="flex-row items-center justify-between">
              <Text className="text-xs font-semibold text-neutral-600">Prazo Estimado</Text>
              {etapa.prazo_estimado_horas <= 0 ? (
                <Text className="text-[10px] font-bold text-red-600">Obrigatório &gt; 0h</Text>
              ) : null}
            </View>
            <View
              className={`flex-row items-center rounded-xl border bg-neutral-50/60 px-3 py-2 ${
                etapa.prazo_estimado_horas <= 0 ? "border-red-400 bg-red-50/30" : "border-neutral-300"
              }`}
            >
              <Ionicons name="time-outline" size={16} color="#737373" className="mr-2" />
              <TextInput
                value={String(etapa.prazo_estimado_horas || "")}
                onChangeText={(val) => {
                  const num = parseInt(val.replace(/[^0-9]/g, ""), 10) || 0;
                  onAtualizarCampo("prazo_estimado_horas", num);
                }}
                keyboardType="numeric"
                placeholder="Ex: 24"
                placeholderTextColor="#a3a3a3"
                className="flex-1 py-0 text-sm font-semibold text-neutral-800"
              />
              <Text className="text-xs font-bold text-neutral-500 ml-1">horas</Text>
            </View>
          </View>

          {/* Bloco de Alocação de Recurso */}
          <View className="gap-1.5">
            <Text className="text-xs font-semibold text-neutral-600">Recurso Alocado</Text>
            {etapa.recurso_id ? (
              <View className="flex-row items-center justify-between rounded-xl border border-emerald-200 bg-emerald-50/40 p-2 px-3">
                <View className="flex-row items-center gap-2 flex-1 mr-2">
                  <View className="h-7 w-7 rounded-full bg-emerald-100 items-center justify-center">
                    <Ionicons name="person" size={14} color="#047857" />
                  </View>
                  <Text
                    className="text-xs font-bold text-emerald-950 truncate flex-1"
                    numberOfLines={1}
                  >
                    {etapa.recurso_nome || "Profissional Alocado"}
                  </Text>
                </View>

                <View className="flex-row items-center gap-1">
                  <Pressable
                    onPress={onAlocarRecurso}
                    className="rounded-md p-1 hover:bg-emerald-100"
                    accessibilityLabel="Alterar recurso"
                  >
                    <Ionicons name="swap-horizontal" size={16} color="#065f46" />
                  </Pressable>
                  <Pressable
                    onPress={onRemoverAlocacao}
                    className="rounded-md p-1 hover:bg-red-100"
                    accessibilityLabel="Remover alocação"
                  >
                    <Ionicons name="close" size={16} color="#dc2626" />
                  </Pressable>
                </View>
              </View>
            ) : (
              <Pressable
                onPress={onAlocarRecurso}
                className="flex-row items-center justify-center gap-1.5 rounded-xl border border-dashed border-neutral-300 bg-neutral-50/50 py-2.5 px-3 hover:bg-neutral-100 active:bg-neutral-200"
                accessibilityRole="button"
                accessibilityLabel="Alocar recurso para esta etapa"
              >
                <Ionicons name="person-add-outline" size={15} color="#6f4f28" />
                <Text className="text-xs font-semibold text-[#6f4f28]">+ Alocar Recurso</Text>
              </Pressable>
            )}
          </View>
        </View>

        {/* Linha 3: Barra inferior com Ação de Concluir Etapa */}
        <View className="mt-4 pt-3 border-t border-neutral-100 flex-row items-center justify-between flex-wrap gap-2">
          <View className="flex-row items-center gap-2">
            <Ionicons
              name={isConcluida ? "checkmark-circle" : "ellipse-outline"}
              size={16}
              color={isConcluida ? "#10b981" : "#a3a3a3"}
            />
            <Text className="text-xs text-neutral-500">
              {isConcluida
                ? "Esta etapa foi marcada como concluída"
                : isAguardando
                ? "Aguardando conclusão das etapas anteriores"
                : "Pronta para execução"}
            </Text>
          </View>

          <Pressable
            onPress={onAlternarConclusao}
            className={`flex-row items-center gap-1.5 rounded-lg px-3 py-1.5 border transition-colors ${
              isConcluida
                ? "border-neutral-300 bg-white hover:bg-neutral-50 text-neutral-700"
                : "border-emerald-600 bg-emerald-600 active:bg-emerald-700"
            }`}
            accessibilityRole="button"
          >
            <Ionicons
              name={isConcluida ? "arrow-undo-outline" : "checkmark-outline"}
              size={15}
              color={isConcluida ? "#525252" : "#ffffff"}
            />
            <Text
              className={`text-xs font-semibold ${
                isConcluida ? "text-neutral-700" : "text-white"
              }`}
            >
              {isConcluida ? "Reabrir Etapa" : "Marcar como Concluída"}
            </Text>
          </Pressable>
        </View>
      </View>
    </View>
  );
}
