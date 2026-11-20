import React from "react";
import { ActivityIndicator, View, Text, Pressable, TextInput } from "react-native";
import { Ionicons } from "@expo/vector-icons";
import { Picker } from "@react-native-picker/picker";
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
  onDisparar: () => void;
  confirmandoDisparo: boolean;
  disparando: boolean;
  onConfirmarDisparo: () => void;
  onCancelarDisparo: () => void;
  onAtualizarCampo: (campo: keyof EtapaWorkflow, valor: any) => void;
}

function formatarData(iso?: string | null): string | null {
  if (!iso) return null;
  const data = new Date(iso);
  if (Number.isNaN(data.getTime())) return null;
  return data.toLocaleDateString("pt-BR");
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
  onDisparar,
  confirmandoDisparo,
  disparando,
  onConfirmarDisparo,
  onCancelarDisparo,
  onAtualizarCampo,
}: EtapaContainerProps) {
  const isAlocada = etapa.status === "ALOCADO";
  const isAguardando = etapa.status === "AGUARDANDO_ETAPA_ANTERIOR";

  // Obter nome legível do tipo de serviço
  const tipoEncontrado = tiposServico.find((t) => t.id === etapa.tipo_servico_id);
  const tipoNome = etapa.tipo_servico_nome || tipoEncontrado?.nome || "Tipo não definido";
  const dataLimite = formatarData(etapa.data_limite_entrega);

  // Cores do nó indicador da timeline
  let badgeNoBg = "bg-amber-100 border-amber-300 text-amber-800";
  let statusRotulo = "Pendente";
  let statusBadgeBg = "bg-amber-50 border-amber-200 text-amber-800";

  if (isAlocada) {
    badgeNoBg = "bg-emerald-500 border-emerald-600 text-white";
    statusRotulo = "Alocada";
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
            isFirst ? "bg-transparent" : isAlocada ? "bg-emerald-400" : "bg-neutral-300"
          }`}
          style={{ minHeight: 16 }}
        />

        {/* Círculo do Nó da Timeline com Ordem Sequencial */}
        <View
          className={`h-9 w-9 rounded-full border-2 items-center justify-center shadow-xs z-10 ${badgeNoBg}`}
        >
          {isAlocada ? (
            <Ionicons name="checkmark" size={18} color="#ffffff" />
          ) : (
            <Text className="text-xs font-bold font-mono">{etapa.ordem}</Text>
          )}
        </View>

        {/* Linha inferior conectora */}
        <View
          className={`w-0.5 flex-1 ${
            isLast ? "bg-transparent" : isAlocada ? "bg-emerald-400" : "bg-neutral-300"
          }`}
          style={{ minHeight: 16 }}
        />
      </View>

      {/* Cartão Container da Etapa */}
      <View
        className={`flex-1 mb-6 rounded-2xl border bg-white p-5 shadow-sm transition-all ${
          isAlocada
            ? "border-emerald-200 bg-emerald-50/15"
            : isAguardando
            ? "border-neutral-200"
            : "border-amber-200 shadow-amber-50"
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
              editable={!bloqueadoPorRN3}
              placeholder="Nome da etapa (ex: Tradução Principal)..."
              placeholderTextColor="#a3a3a3"
              className={`flex-1 min-w-[200px] text-base font-bold text-neutral-900 border-b border-transparent hover:border-neutral-300 focus:border-[#8c5230] py-0.5 px-1 rounded ${
                bloqueadoPorRN3 ? "text-neutral-500" : ""
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

        {/* Linha 2: Configurações da Etapa (Tipo de Serviço, Prazo Estimado e Alocação) */}
        <View className="mt-4 grid grid-cols-1 md:grid-cols-3 gap-4">
          {/* Seletor do Tipo de Serviço */}
          <View className="gap-1.5">
            <Text className="text-xs font-semibold text-neutral-600">Tipo de Serviço</Text>
            <View
              className={`rounded-xl border border-neutral-300 bg-neutral-50/60 px-2 ${
                bloqueadoPorRN3 ? "opacity-60" : ""
              }`}
            >
              <Picker
                selectedValue={etapa.tipo_servico_id}
                onValueChange={(valor) => onAtualizarCampo("tipo_servico_id", String(valor))}
                enabled={!bloqueadoPorRN3}
                style={{ width: "100%", fontSize: 13, color: "#262626" }}
                dropdownIconColor="#737373"
                accessibilityLabel="Tipo de Serviço"
              >
                <Picker.Item label="Selecione o serviço..." value="" />
                {tiposServico.map((tipo) => (
                  <Picker.Item key={tipo.id} label={tipo.nome} value={tipo.id} />
                ))}
              </Picker>
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
                editable={!bloqueadoPorRN3}
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
                  <View className="flex-1 min-w-0">
                    <Text
                      className="text-xs font-bold text-emerald-950 truncate"
                      numberOfLines={1}
                    >
                      {etapa.recurso_nome || "Profissional Alocado"}
                    </Text>
                    {dataLimite ? (
                      <Text className="text-[11px] text-emerald-800">
                        Entrega: {dataLimite}
                      </Text>
                    ) : null}
                  </View>
                </View>

                <Pressable
                  onPress={onAlocarRecurso}
                  className="rounded-md p-1 hover:bg-emerald-100"
                  accessibilityLabel="Alterar recurso alocado"
                >
                  <Ionicons name="swap-horizontal" size={16} color="#065f46" />
                </Pressable>
              </View>
            ) : (
              <>
                <Pressable
                  onPress={onAlocarRecurso}
                  disabled={!etapa.id}
                  className={`flex-row items-center justify-center gap-1.5 rounded-xl border border-dashed border-neutral-300 bg-neutral-50/50 py-2.5 px-3 hover:bg-neutral-100 active:bg-neutral-200 ${
                    !etapa.id ? "opacity-50" : ""
                  }`}
                  accessibilityRole="button"
                  accessibilityLabel="Alocar recurso para esta etapa"
                >
                  <Ionicons name="person-add-outline" size={15} color="#6f4f28" />
                  <Text className="text-xs font-semibold text-[#6f4f28]">
                    + Alocar Recurso
                  </Text>
                </Pressable>
                {!etapa.id ? (
                  <Text className="text-[10px] text-neutral-500">
                    Salve o workflow antes de alocar recursos.
                  </Text>
                ) : null}
              </>
            )}
            {etapa.status === "PENDENTE" && etapa.id ? (
              <>
                {disparando ? (
                  <View className="flex-row items-center justify-center gap-2 rounded-xl border border-blue-200 bg-blue-50 py-2.5 px-3">
                    <ActivityIndicator size="small" color="#1d4ed8" />
                    <Text className="text-xs font-semibold text-blue-800">Disparando...</Text>
                  </View>
                ) : confirmandoDisparo ? (
                  <View className="gap-2 rounded-xl border border-blue-200 bg-blue-50 p-3">
                    <Text className="text-xs text-blue-900">Disparar para todos os recursos elegíveis?</Text>
                    <View className="flex-row gap-2">
                      <Pressable onPress={onConfirmarDisparo} disabled={disparando} className="rounded-lg bg-blue-700 px-3 py-2">
                        <Text className="text-xs font-semibold text-white">Confirmar disparo</Text>
                      </Pressable>
                      <Pressable onPress={onCancelarDisparo} className="rounded-lg border border-neutral-300 bg-white px-3 py-2">
                        <Text className="text-xs font-semibold text-neutral-700">Cancelar</Text>
                      </Pressable>
                    </View>
                  </View>
                ) : (
                  <Pressable onPress={onDisparar} className="flex-row items-center justify-center gap-1.5 rounded-xl border border-blue-200 bg-blue-50 py-2.5 px-3">
                    <Ionicons name="send-outline" size={15} color="#1d4ed8" />
                    <Text className="text-xs font-semibold text-blue-800">Disparar para elegíveis</Text>
                  </Pressable>
                )}
              </>
            ) : null}
            {etapa.status === "ACEITA" && etapa.recurso_aceite_nome ? (
              <Text className="text-xs font-medium text-emerald-800">
                Aceita por {etapa.recurso_aceite_nome}{etapa.aceito_em ? ` em ${new Date(etapa.aceito_em).toLocaleString("pt-BR")}` : ""}
              </Text>
            ) : null}
          </View>
        </View>

        {/* Linha 3: Situação da etapa */}
        <View className="mt-4 pt-3 border-t border-neutral-100 flex-row items-center gap-2">
          <Ionicons
            name={
              isAlocada
                ? "checkmark-circle"
                : isAguardando
                ? "hourglass-outline"
                : "ellipse-outline"
            }
            size={16}
            color={isAlocada ? "#10b981" : isAguardando ? "#a3a3a3" : "#d97706"}
          />
          <Text className="text-xs text-neutral-500 flex-1">
            {isAlocada
              ? "Recurso alocado para a execução desta etapa"
              : isAguardando
              ? "Aguardando a etapa anterior para ser alocada"
              : "Pendente de alocação de recurso"}
          </Text>
        </View>
      </View>
    </View>
  );
}
