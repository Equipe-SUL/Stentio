import { useEffect, useState } from "react";
import { ActivityIndicator, Pressable, ScrollView, Text, View } from "react-native";
import { useLocalSearchParams, useRouter } from "expo-router";
import { Ionicons } from "@expo/vector-icons";
import { getApiErrorMessage } from "../../../lib/api";
import { CabecalhoTela } from "../../../components/navegacao/CabecalhoTela";
import { buscarOrcamento, type OrcamentoDetalhe } from "../../../components/orcamentos/orcamentosApi";

const moeda = (v: number) => new Intl.NumberFormat("pt-BR", { style: "currency", currency: "BRL" }).format(v ?? 0);
const data = (v: string) => { const d = new Date(v); return Number.isNaN(d.getTime()) ? "—" : new Intl.DateTimeFormat("pt-BR", { dateStyle: "medium" }).format(d); };
const statusLabel: Record<string, string> = { EM_ELABORACAO: "Em elaboração", AGUARDANDO_APROVACAO: "Aguardando aprovação", APROVADO: "Aprovado", REJEITADO: "Rejeitado" };

export default function OrcamentoDetalheRoute() {
  const { id } = useLocalSearchParams<{ id: string }>(); const router = useRouter();
  const [orcamento, setOrcamento] = useState<OrcamentoDetalhe | null>(null); const [erro, setErro] = useState(""); const [carregando, setCarregando] = useState(true);
  useEffect(() => { if (!id) return; buscarOrcamento(id).then(setOrcamento).catch((e) => setErro(getApiErrorMessage(e))).finally(() => setCarregando(false)); }, [id]);
  return <ScrollView className="flex-1 bg-[#fbfaf8]" contentContainerStyle={{ flexGrow: 1, paddingBottom: 40 }}><View className="mx-auto w-full max-w-5xl gap-5 px-4 py-8 md:px-8">
    <CabecalhoTela contexto="Operacional" secao="Orçamentos" titulo="Detalhes do orçamento" descricao="Proposta e serviços cadastrados." rotaFallback="/orcamentos" acoes={<Pressable onPress={() => router.push("/orcamentos")} className="flex-row items-center gap-2 rounded-lg border border-neutral-300 bg-white px-4 py-3"><Ionicons name="list-outline" size={18} color="#6f4f28"/><Text className="font-medium text-neutral-700">Listagem</Text></Pressable>} />
    {carregando ? <View className="items-center rounded-xl bg-white p-10"><ActivityIndicator color="#6f4f28"/></View> : erro ? <View className="rounded-xl border border-red-200 bg-red-50 p-4"><Text className="text-sm text-red-700">{erro}</Text></View> : orcamento ? <>
      <View className="flex-row flex-wrap gap-3 rounded-2xl border border-neutral-200 bg-white p-5"><View className="min-w-[180px] flex-1"><Text className="text-xs text-neutral-500">Status</Text><Text className="mt-1 font-semibold text-[#6f4f28]">{statusLabel[orcamento.status] ?? orcamento.status}</Text></View><View className="min-w-[180px] flex-1"><Text className="text-xs text-neutral-500">Valor total</Text><Text className="mt-1 text-xl font-bold text-neutral-900">{moeda(orcamento.valor_total)}</Text></View><View className="min-w-[180px] flex-1"><Text className="text-xs text-neutral-500">Expira em</Text><Text className="mt-1 font-medium text-neutral-800">{data(orcamento.data_expiracao)}</Text></View><View className="min-w-[180px] flex-1"><Text className="text-xs text-neutral-500">Versão</Text><Text className="mt-1 font-medium text-neutral-800">{orcamento.versao}</Text></View></View>
      <View className="gap-4 rounded-2xl border border-neutral-200 bg-white p-5"><Text className="text-lg font-semibold text-neutral-900">Itens da proposta</Text>{orcamento.itens?.length ? orcamento.itens.map((item, i) => <View key={item.id ?? item.ordem} className="gap-3 rounded-xl border border-neutral-200 bg-neutral-50 p-4"><View className="flex-row items-center gap-2"><Ionicons name="swap-horizontal-outline" size={18} color="#6f4f28"/><Text className="font-semibold text-neutral-800">Item {i + 1}</Text><Text className="ml-auto font-semibold text-neutral-700">{moeda(item.subtotal_item ?? 0)}</Text></View><Text className="text-xs text-neutral-500">Idioma de origem: {item.idioma_origem_id} · destino: {item.idioma_destino_id}</Text>{item.servicos.map((serv, j) => <View key={serv.id ?? j} className="flex-row flex-wrap items-center justify-between gap-2 rounded-lg bg-white p-3"><View><Text className="font-medium text-neutral-800">{serv.tipo_servico}</Text><Text className="text-xs text-neutral-500">{serv.quantidade} {serv.unidade} × {moeda(serv.valor_unitario)}</Text></View><Text className="font-semibold text-neutral-700">{moeda(serv.subtotal ?? serv.quantidade * serv.valor_unitario)}</Text></View>)}</View>) : <Text className="text-sm text-neutral-500">Este orçamento ainda não possui itens.</Text>}</View>
      <View className="rounded-xl border border-neutral-200 bg-white p-4"><Text className="text-xs text-neutral-500">Identificação</Text><Text selectable className="mt-1 text-xs text-neutral-700">Orçamento {orcamento.id} · Solicitação {orcamento.solicitacao_id}</Text></View>
    </> : null}
  </View></ScrollView>;
}
