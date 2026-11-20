import React, { useCallback, useEffect, useState } from "react";
import { ActivityIndicator, Pressable, ScrollView, Text, View } from "react-native";
import { Ionicons } from "@expo/vector-icons";
import { getApiErrorMessage } from "../../../lib/api";
import { listarMinhasDemandas, type DemandaRecurso } from "../../../components/recursos/portalRecursoApi";

const STATUS: Record<string, { label: string; cor: string }> = {
  PENDENTE: { label: "Pendente", cor: "bg-amber-50 text-amber-800 border-amber-200" },
  AGUARDANDO_ETAPA_ANTERIOR: { label: "Aguardando etapa anterior", cor: "bg-neutral-100 text-neutral-700 border-neutral-200" },
  ALOCADO: { label: "Alocada", cor: "bg-emerald-50 text-emerald-800 border-emerald-200" },
};

function formatarPrazo(valor: string | null): string {
  if (!valor) return "Prazo não definido";
  const data = new Date(valor);
  if (Number.isNaN(data.getTime())) return "Prazo não definido";
  return new Intl.DateTimeFormat("pt-BR", { dateStyle: "medium", timeStyle: "short" }).format(data);
}

export default function MinhasDemandasScreen() {
  const [demandas, setDemandas] = useState<DemandaRecurso[]>([]);
  const [carregando, setCarregando] = useState(true);
  const [erro, setErro] = useState("");

  const carregar = useCallback(async () => {
    setCarregando(true);
    setErro("");
    try {
      setDemandas(await listarMinhasDemandas());
    } catch (error) {
      setErro(getApiErrorMessage(error));
    } finally {
      setCarregando(false);
    }
  }, []);

  useEffect(() => {
    let ativo = true;
    listarMinhasDemandas()
      .then((lista) => { if (ativo) setDemandas(lista); })
      .catch((error) => { if (ativo) setErro(getApiErrorMessage(error)); })
      .finally(() => { if (ativo) setCarregando(false); });
    return () => { ativo = false; };
  }, []);

  return (
    <ScrollView className="flex-1" contentContainerClassName="items-center p-5 py-8 md:p-8">
      <View className="w-full max-w-5xl gap-6">
        <View className="flex-row items-start justify-between gap-4">
          <View className="flex-1 gap-1">
            <Text className="text-2xl font-bold text-neutral-900">Minhas demandas</Text>
            <Text className="text-sm text-neutral-600">Etapas de serviço destinadas a você.</Text>
          </View>
          {!carregando && !erro ? (
            <View className="rounded-full bg-[#f5ede7] px-3 py-1.5">
              <Text className="text-xs font-semibold text-[#744127]">{demandas.length} {demandas.length === 1 ? "etapa" : "etapas"}</Text>
            </View>
          ) : null}
        </View>

        {carregando ? (
          <View className="items-center justify-center gap-3 rounded-2xl border border-neutral-200 bg-white p-12">
            <ActivityIndicator color="#8c5230" />
            <Text className="text-sm text-neutral-500">Carregando suas demandas...</Text>
          </View>
        ) : erro ? (
          <View className="items-center gap-3 rounded-2xl border border-red-200 bg-white p-8">
            <Ionicons name="cloud-offline-outline" size={30} color="#b91c1c" />
            <Text accessibilityRole="alert" className="text-center text-sm text-red-700">{erro}</Text>
            <Pressable onPress={() => void carregar()} className="rounded-lg border border-neutral-300 px-4 py-2">
              <Text className="font-medium text-neutral-700">Tentar novamente</Text>
            </Pressable>
          </View>
        ) : demandas.length === 0 ? (
          <View className="items-center gap-3 rounded-2xl border border-neutral-200 bg-white p-10">
            <View className="h-14 w-14 items-center justify-center rounded-full bg-neutral-100">
              <Ionicons name="briefcase-outline" size={25} color="#8c5230" />
            </View>
            <Text className="text-base font-semibold text-neutral-900">Nenhuma demanda destinada</Text>
            <Text className="max-w-md text-center text-sm leading-5 text-neutral-500">Quando uma etapa for atribuída a você, ela aparecerá nesta lista.</Text>
          </View>
        ) : (
          <View className="gap-3">
            {demandas.map((demanda) => {
              const status = STATUS[demanda.situacao] ?? { label: demanda.situacao, cor: "bg-neutral-100 text-neutral-700 border-neutral-200" };
              return (
                <View key={demanda.etapa_id} className="gap-4 rounded-2xl border border-neutral-200 bg-white p-5 shadow-sm">
                  <View className="flex-row flex-wrap items-start justify-between gap-3">
                    <View className="flex-1 gap-1">
                      <Text className="text-xs font-semibold uppercase tracking-wide text-neutral-500">OS {demanda.ordem_servico_codigo || demanda.ordem_servico_id}</Text>
                      <Text className="text-base font-bold text-neutral-900">{demanda.nome || `Etapa ${demanda.ordem}`}</Text>
                    </View>
                    <View className={`rounded-full border px-3 py-1.5 ${status.cor}`}>
                      <Text className="text-xs font-semibold">{status.label}</Text>
                    </View>
                  </View>
                  <View className="flex-row flex-wrap gap-x-8 gap-y-3 border-t border-neutral-100 pt-4">
                    <View className="min-w-[130px] flex-1 gap-1">
                      <Text className="text-xs text-neutral-500">Tipo de serviço</Text>
                      <Text className="text-sm font-medium text-neutral-800">{demanda.tipo_servico}</Text>
                    </View>
                    <View className="min-w-[170px] flex-1 gap-1">
                      <Text className="text-xs text-neutral-500">Par de idiomas</Text>
                      <Text className="text-sm font-medium text-neutral-800">{demanda.idioma_origem} → {demanda.idioma_destino}</Text>
                    </View>
                    <View className="min-w-[160px] flex-1 gap-1">
                      <Text className="text-xs text-neutral-500">Prazo</Text>
                      <Text className="text-sm font-medium text-neutral-800">{formatarPrazo(demanda.data_limite_entrega)}</Text>
                    </View>
                    <View className="min-w-[130px] flex-1 gap-1">
                      <Text className="text-xs text-neutral-500">Estimativa</Text>
                      <Text className="text-sm font-medium text-neutral-800">{demanda.prazo_estimado_horas} h</Text>
                    </View>
                  </View>
                </View>
              );
            })}
          </View>
        )}
      </View>
    </ScrollView>
  );
}
