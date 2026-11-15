import React, { createElement, useMemo, useState } from "react";
import {
  ActivityIndicator,
  Modal,
  Platform,
  Pressable,
  ScrollView,
  Text,
  TextInput,
  View,
} from "react-native";
import { Ionicons } from "@expo/vector-icons";
import { Picker } from "@react-native-picker/picker";
import { getApiErrorMessage } from "../../../lib/api";
import type { EtapaWorkflow, RecursoOption, TipoServicoOption } from "./types";

interface ModalAlocarRecursoProps {
  visible: boolean;
  etapa: EtapaWorkflow | null;
  recursos: RecursoOption[];
  tiposServico: TipoServicoOption[];
  onFechar: () => void;
  onConfirmarAlocacao: (
    recurso: RecursoOption,
    dataLimiteEntrega: string,
  ) => Promise<void>;
}

const dataLocal = (data: Date) =>
  `${data.getFullYear()}-${String(data.getMonth() + 1).padStart(2, "0")}-${String(
    data.getDate(),
  ).padStart(2, "0")}`;

const hojeMaisSete = () => {
  const d = new Date();
  d.setDate(d.getDate() + 7);
  return dataLocal(d);
};

const dataDeIso = (iso?: string | null): string => {
  if (!iso) return hojeMaisSete();
  const data = new Date(iso);
  return Number.isNaN(data.getTime()) ? hojeMaisSete() : dataLocal(data);
};

const isoDeDataLocal = (data: string): string =>
  new Date(`${data}T23:59:59`).toISOString();

function SeletorData({
  value,
  onChange,
  label,
}: {
  value: string;
  onChange: (data: string) => void;
  label: string;
}) {
  const [ano, mes, dia] = value.split("-").map(Number);
  const hoje = dataLocal(new Date());
  const diasNoMes = new Date(ano, mes, 0).getDate();
  const anosDisponiveis = Array.from(
    { length: 6 },
    (_, index) => new Date().getFullYear() + index,
  );
  const atualizar = (novoAno: number, novoMes: number, novoDia: number) => {
    const diaValido = Math.min(novoDia, new Date(novoAno, novoMes, 0).getDate());
    const novaData = `${novoAno}-${String(novoMes).padStart(2, "0")}-${String(
      diaValido,
    ).padStart(2, "0")}`;
    if (novaData >= hoje) onChange(novaData);
  };

  return (
    <View className="gap-1">
      <Text className="text-xs font-medium text-neutral-600">{label}</Text>
      {Platform.OS === "web" ? (
        createElement("input", {
          type: "date",
          value,
          min: hoje,
          onChange: (event: React.ChangeEvent<HTMLInputElement>) =>
            onChange(event.currentTarget.value),
          "aria-label": label,
          style: {
            width: "100%",
            minHeight: 42,
            padding: "8px 12px",
            border: "1px solid #d4d4d4",
            borderRadius: 8,
            backgroundColor: "white",
            color: "#262626",
            fontSize: 14,
          },
        })
      ) : (
        <View className="flex-row gap-2">
          <View className="min-w-0 flex-[0.8] overflow-hidden rounded-lg border border-neutral-300 bg-white">
            <Picker
              selectedValue={dia}
              onValueChange={(novoDia) => atualizar(ano, mes, Number(novoDia))}
              style={{ color: "#171717" }}
              accessibilityLabel="Dia"
            >
              <Picker.Item label="Dia" value={0} />
              {Array.from({ length: diasNoMes }, (_, index) => index + 1).map((opcao) => (
                <Picker.Item
                  key={opcao}
                  label={String(opcao).padStart(2, "0")}
                  value={opcao}
                />
              ))}
            </Picker>
          </View>
          <View className="min-w-0 flex-[1.4] overflow-hidden rounded-lg border border-neutral-300 bg-white">
            <Picker
              selectedValue={mes}
              onValueChange={(novoMes) => atualizar(ano, Number(novoMes), dia)}
              style={{ color: "#171717" }}
              accessibilityLabel="Mês"
            >
              {Array.from({ length: 12 }, (_, index) => index + 1).map((opcao) => (
                <Picker.Item
                  key={opcao}
                  label={new Intl.DateTimeFormat("pt-BR", { month: "long" }).format(
                    new Date(2020, opcao - 1, 1),
                  )}
                  value={opcao}
                />
              ))}
            </Picker>
          </View>
          <View className="min-w-0 flex-1 overflow-hidden rounded-lg border border-neutral-300 bg-white">
            <Picker
              selectedValue={ano}
              onValueChange={(novoAno) => atualizar(Number(novoAno), mes, dia)}
              style={{ color: "#171717" }}
              accessibilityLabel="Ano"
            >
              {anosDisponiveis.map((opcao) => (
                <Picker.Item key={opcao} label={String(opcao)} value={opcao} />
              ))}
            </Picker>
          </View>
        </View>
      )}
    </View>
  );
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
  const [recursoSelecionadoId, setRecursoSelecionadoId] = useState<string | null>(
    etapa?.recurso_id ?? null,
  );
  const [dataLimite, setDataLimite] = useState(
    dataDeIso(etapa?.data_limite_entrega),
  );
  const [erro, setErro] = useState<string | null>(null);
  const [salvando, setSalvando] = useState(false);

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

  const hoje = dataLocal(new Date());
  const dataValida = /^\d{4}-\d{2}-\d{2}$/.test(dataLimite) && dataLimite >= hoje;

  if (!visible || !etapa) return null;

  const handleConfirmar = async () => {
    if (!recursoSelecionadoId) {
      setErro("Selecione um recurso para alocar.");
      return;
    }
    if (!dataValida) {
      setErro("Informe uma data limite de entrega válida (hoje ou no futuro).");
      return;
    }
    const recurso = recursos.find((r) => r.id === recursoSelecionadoId);
    if (!recurso) {
      setErro("Recurso selecionado não encontrado.");
      return;
    }

    setSalvando(true);
    setErro(null);
    try {
      await onConfirmarAlocacao(recurso, isoDeDataLocal(dataLimite));
    } catch (err) {
      setErro(getApiErrorMessage(err));
    } finally {
      setSalvando(false);
    }
  };

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

          {/* Data limite e busca */}
          <View className="p-4 bg-white border-b border-neutral-100 gap-3">
            <SeletorData
              value={dataLimite}
              onChange={setDataLimite}
              label="Data limite de entrega"
            />

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
                  const selecionado = recursoSelecionadoId === recurso.id;
                  const temEspecialidade = recurso.tiposServico?.some(
                    (ts) => ts.id === etapa.tipo_servico_id || ts.nome === tipoServicoNome,
                  );

                  return (
                    <Pressable
                      key={recurso.id}
                      onPress={() => setRecursoSelecionadoId(recurso.id)}
                      className={`flex-row items-center justify-between rounded-xl border p-3 transition-colors ${
                        selecionado
                          ? "border-emerald-400 bg-emerald-50/60"
                          : "border-neutral-200 bg-white hover:border-[#8c5230]/40 hover:bg-neutral-50"
                      }`}
                      accessibilityRole="button"
                      accessibilityState={{ selected: selecionado }}
                    >
                      <View className="flex-row items-center gap-3 flex-1 mr-3 min-w-0">
                        <View
                          className={`h-10 w-10 rounded-full items-center justify-center ${
                            selecionado ? "bg-emerald-200 text-emerald-800" : "bg-[#f6efe9]"
                          }`}
                        >
                          <Ionicons
                            name="person"
                            size={18}
                            color={selecionado ? "#047857" : "#8c5230"}
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
                        {selecionado ? (
                          <View className="flex-row items-center gap-1 rounded-lg bg-emerald-100 px-2.5 py-1">
                            <Ionicons name="checkmark-circle" size={14} color="#047857" />
                            <Text className="text-xs font-bold text-emerald-800">
                              Selecionado
                            </Text>
                          </View>
                        ) : isAtual ? (
                          <View className="rounded-lg border border-emerald-300 bg-emerald-50 px-2.5 py-1">
                            <Text className="text-xs font-semibold text-emerald-800">
                              Alocado
                            </Text>
                          </View>
                        ) : (
                          <View className="rounded-lg border border-neutral-300 bg-neutral-100/70 px-3 py-1.5">
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
          <View className="border-t border-neutral-200 bg-white p-4 gap-3">
            {erro ? (
              <View className="flex-row items-center gap-2 rounded-lg bg-red-50 border border-red-200 px-3 py-2">
                <Ionicons name="alert-circle" size={15} color="#dc2626" />
                <Text className="text-xs font-medium text-red-800 flex-1">{erro}</Text>
              </View>
            ) : null}

            <View className="flex-row justify-end gap-3">
              <Pressable
                onPress={onFechar}
                disabled={salvando}
                className="rounded-lg border border-neutral-300 bg-white px-4 py-2 hover:bg-neutral-50 disabled:opacity-50"
              >
                <Text className="text-xs font-semibold text-neutral-700">Cancelar</Text>
              </Pressable>

              <Pressable
                onPress={handleConfirmar}
                disabled={salvando || !recursoSelecionadoId || !dataValida}
                className={`flex-row items-center gap-2 rounded-lg px-4 py-2 ${
                  salvando || !recursoSelecionadoId || !dataValida
                    ? "bg-neutral-300 opacity-60"
                    : "bg-[#6f4f28] active:bg-[#5b4020]"
                }`}
                accessibilityRole="button"
                accessibilityLabel="Confirmar alocação do recurso na etapa"
              >
                {salvando ? (
                  <ActivityIndicator size="small" color="#ffffff" />
                ) : (
                  <Ionicons name="checkmark" size={15} color="#ffffff" />
                )}
                <Text className="text-xs font-bold text-white">
                  {salvando ? "Alocando..." : "Confirmar Alocação"}
                </Text>
              </Pressable>
            </View>
          </View>
        </View>
      </View>
    </Modal>
  );
}
