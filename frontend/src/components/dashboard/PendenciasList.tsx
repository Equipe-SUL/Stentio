import { useMemo, useState } from "react";
import { Ionicons } from "@expo/vector-icons";
import { useRouter } from "expo-router";
import { ActivityIndicator, Pressable, Text, View } from "react-native";
import {
  ehAtrasada, filtrarPendencias, formatarData, ordenarPendencias,
  prazoVencido, rotaDeAcao,
} from "./dashboardUtils";
import type { FiltroPendencias } from "./dashboardUtils";
import type { DashboardPendencias, Pendencia } from "./types";

const CORES: Record<string, { fundo: string; cor: string; texto: string }> = {
  ATRASADO: { fundo: "#fce8e6", cor: "#bb3f3d", texto: "Atrasado" },
  EM_ELABORACAO: { fundo: "#e7efff", cor: "#335eaa", texto: "Em elaboração" },
  AGUARDANDO_APROVACAO: { fundo: "#fff1d6", cor: "#926322", texto: "Aguardando aprovação" },
  APROVADO: { fundo: "#e9f7ee", cor: "#287346", texto: "Aprovado" },
};

const FILTROS: { id: FiltroPendencias; label: string }[] = [
  { id: "TODAS", label: "Todas" },
  { id: "ATRASADAS", label: "Atrasadas" },
  { id: "ETAPA_OS", label: "Etapas" },
  { id: "ORCAMENTO", label: "Orçamentos" },
];

function TipoBadge({ tipo }: { tipo: Pendencia["tipo_entidade"] }) {
  const etapa = tipo === "ETAPA_OS";
  return (
    <View
      className="self-start rounded-md px-2.5 py-1"
      style={{ backgroundColor: etapa ? "#fce8e6" : "#e8effc" }}
    >
      <Text style={{ color: etapa ? "#a44343" : "#345fa7" }} className="text-[10px] font-bold">
        {etapa ? "ETAPA / OS" : "ORÇAMENTO"}
      </Text>
    </View>
  );
}

function StatusBadge({ status }: { status: string }) {
  const c = CORES[status] ?? {
    fundo: "#f2efeb", cor: "#655a50", texto: status.replace(/_/g, " "),
  };
  return (
    <View className="self-start rounded-full px-3 py-1.5" style={{ backgroundColor: c.fundo }}>
      <Text className="text-[11px] font-semibold" style={{ color: c.cor }}>
        {status === "ATRASADO" ? "● " : ""}{c.texto}
      </Text>
    </View>
  );
}

function PendenciaRow({ item, compacto }: { item: Pendencia; compacto: boolean }) {
  const router = useRouter();
  const rota = rotaDeAcao(item.link_acao);
  const atrasada = ehAtrasada(item);
  const venceu = prazoVencido(item.data_limite);

  function abrir() {
    if (rota) router.push(rota);
  }

  if (compacto) {
    return (
      <Pressable
        onPress={abrir}
        disabled={!rota}
        accessibilityRole="button"
        accessibilityLabel={`${item.titulo}. ${item.status}. ${rota ? "Abrir detalhes" : "Sem link disponível"}`}
        className="gap-3 border-t border-[#eeeae5] px-4 py-4 active:bg-[#faf6f1]"
      >
        <View className="flex-row items-center justify-between gap-2">
          <TipoBadge tipo={item.tipo_entidade} />
          <StatusBadge status={item.status} />
        </View>
        <Text className="text-sm font-semibold leading-5 text-[#332923]">{item.titulo}</Text>
        <View className="flex-row flex-wrap items-center justify-between gap-2">
          <View className="flex-row items-center gap-2">
            <Ionicons name="calendar-outline" size={14} color={venceu ? "#bb3f3d" : "#86796b"} />
            <Text className="text-xs" style={{ color: venceu ? "#ad3f3f" : "#73695f" }}>
              {formatarData(item.data_limite)}{venceu ? " · Vencido" : ""}
            </Text>
          </View>
          <Text className="text-xs font-semibold text-[#845a35]">
            {rota ? "Ver detalhes  →" : "Sem ação"}
          </Text>
        </View>
        {atrasada ? (
          <Text className="text-xs text-[#b24640]">Atenção: etapa sinalizada como atrasada.</Text>
        ) : null}
      </Pressable>
    );
  }

  return (
    <Pressable
      onPress={abrir}
      disabled={!rota}
      accessibilityRole="button"
      accessibilityLabel={`${item.titulo}. ${item.status}. ${rota ? "Abrir detalhes" : "Sem link disponível"}`}
      className="flex-row items-center gap-4 border-t border-[#eeeae5] px-5 py-5 active:bg-[#faf6f1]"
      style={{ minHeight: 88 }}
    >
      <View style={{ flex: 1.1 }}><TipoBadge tipo={item.tipo_entidade} /></View>
      <Text style={{ flex: 2.6 }} className="text-sm font-semibold leading-5 text-[#352a23]">
        {item.titulo}
      </Text>
      <View style={{ flex: 1.7 }}><StatusBadge status={item.status} /></View>
      <View style={{ flex: 1.55 }} className="gap-1">
        <Text className="text-xs font-medium" style={{ color: venceu ? "#ad3f3f" : "#665e56" }}>
          {formatarData(item.data_limite)}
        </Text>
        {venceu ? <Text className="text-[10px] text-[#ad3f3f]">Prazo vencido</Text> : null}
      </View>
      <View style={{ flex: 0.9 }} className="flex-row items-center justify-end gap-1">
        <Text className="text-xs font-semibold text-[#845a35]">
          {rota ? "Ver detalhes" : "—"}
        </Text>
        {rota ? <Ionicons name="arrow-forward" size={13} color="#845a35" /> : null}
      </View>
    </Pressable>
  );
}

interface PendenciasListProps {
  dados?: DashboardPendencias;
  carregando: boolean;
  erro?: string;
  onTentarNovamente: () => void;
}

export function PendenciasList({ dados, carregando, erro, onTentarNovamente }: PendenciasListProps) {
  const [filtro, setFiltro] = useState<FiltroPendencias>("TODAS");
  const [largura, setLargura] = useState(0);
  const compacto = largura < 730;
  const itens = useMemo(
    () => filtrarPendencias(ordenarPendencias(dados?.pendencias ?? []), filtro),
    [dados, filtro],
  );

  return (
    <View className="overflow-hidden rounded-2xl border border-[#e8e2db] bg-white shadow-sm"
      onLayout={e => setLargura(e.nativeEvent.layout.width)}>
      <View className="gap-4 px-5 py-6">
        <View className="flex-row flex-wrap items-start justify-between gap-3">
          <View className="flex-1 gap-1" style={{ minWidth: 180 }}>
            <View className="flex-row items-center gap-3">
              <Text className="text-xl font-semibold text-[#33271e]">Pendências operacionais</Text>
              {dados ? (
                <View className="rounded-full bg-[#f8ede0] px-2.5 py-1">
                  <Text className="text-xs font-semibold text-[#95643c]">{dados.total_itens}</Text>
                </View>
              ) : null}
            </View>
            <Text className="text-xs leading-5 text-[#887d72]">
              Itens que precisam de atenção, priorizados pelo prazo de entrega.
            </Text>
          </View>
          <View className="flex-row items-center gap-2">
            <Ionicons name="swap-vertical-outline" size={15} color="#8f8172" />
            <Text className="text-xs text-[#8f8172]">Prazo mais próximo primeiro</Text>
          </View>
        </View>
        <View className="flex-row flex-wrap gap-2">
          {FILTROS.map(opcao => (
            <Pressable
              key={opcao.id}
              accessibilityRole="button"
              accessibilityState={{ selected: filtro === opcao.id }}
              onPress={() => setFiltro(opcao.id)}
              className="rounded-full border px-3 py-2"
              style={{
                backgroundColor: filtro === opcao.id ? "#70502e" : "#faf9f7",
                borderColor: filtro === opcao.id ? "#70502e" : "#e9e5df",
              }}
            >
              <Text className="text-xs font-semibold" style={{ color: filtro === opcao.id ? "#ffffff" : "#6e645a" }}>
                {opcao.label}
              </Text>
            </Pressable>
          ))}
        </View>
      </View>

      {erro && !dados ? (
        <View className="items-center gap-3 border-t border-[#eeeae5] px-5 py-10">
          <Ionicons name="cloud-offline-outline" size={25} color="#ab6a4c" />
          <Text className="text-center text-sm text-[#a44539]">{erro}</Text>
          <Pressable onPress={onTentarNovamente} className="rounded-lg bg-[#755334] px-4 py-2.5">
            <Text className="text-xs font-semibold text-white">Tentar novamente</Text>
          </Pressable>
        </View>
      ) : carregando && !dados ? (
        <View className="items-center gap-3 border-t border-[#eeeae5] py-12">
          <ActivityIndicator color="#70502e" />
          <Text className="text-xs text-[#8a8075]">Buscando pendências...</Text>
        </View>
      ) : (
        <>
          {!compacto ? (
            <View className="flex-row gap-4 bg-[#faf8f5] px-5 py-3">
              <Text style={{ flex: 1.1 }} className="text-[10px] font-bold tracking-wider text-[#8a8177]">TIPO</Text>
              <Text style={{ flex: 2.6 }} className="text-[10px] font-bold tracking-wider text-[#8a8177]">TÍTULO / DETALHE</Text>
              <Text style={{ flex: 1.7 }} className="text-[10px] font-bold tracking-wider text-[#8a8177]">STATUS</Text>
              <Text style={{ flex: 1.55 }} className="text-[10px] font-bold tracking-wider text-[#8a8177]">DATA LIMITE</Text>
              <Text style={{ flex: 0.9 }} className="text-right text-[10px] font-bold tracking-wider text-[#8a8177]">AÇÃO</Text>
            </View>
          ) : null}
          {itens.length ? itens.map(item => (
            <PendenciaRow key={item.id} item={item} compacto={compacto} />
          )) : (
            <View className="items-center gap-2 border-t border-[#eeeae5] py-12">
              <Ionicons name="checkmark-circle-outline" size={30} color="#719778" />
              <Text className="text-sm font-semibold text-[#594e44]">Nenhuma pendência encontrada</Text>
              <Text className="text-xs text-[#96897c]">Experimente selecionar outro filtro.</Text>
            </View>
          )}
          <View className="flex-row flex-wrap items-center justify-between gap-2 border-t border-[#eeeae5] bg-[#faf9f7] px-5 py-4">
            <Text className="text-[11px] text-[#897d72]">
              Exibindo {itens.length} de {dados?.total_itens ?? 0} pendências
            </Text>
            <Text className="text-[11px] text-[#897d72]">Atualização conforme perfil autorizado</Text>
          </View>
        </>
      )}
    </View>
  );
}
