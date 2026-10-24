import { useCallback, useEffect, useState } from "react";
import { ActivityIndicator, Pressable, ScrollView, Text, View } from "react-native";
import { useRouter } from "expo-router";
import { Ionicons } from "@expo/vector-icons";
import { CabecalhoTela } from "../navegacao/CabecalhoTela";
import { DataTable } from "../usuarios/DataTable";
import type { ColumnDef } from "../usuarios/types";
import { getApiErrorMessage } from "../../lib/api";
import { codigoOrcamento, listarOrcamentos, type OrcamentoResumo, type StatusOrcamento } from "./orcamentosApi";

const FILTROS: { valor: StatusOrcamento | "TODOS"; rotulo: string }[] = [
  { valor: "TODOS", rotulo: "Todos" }, { valor: "EM_ELABORACAO", rotulo: "Em elaboração" },
  { valor: "AGUARDANDO_APROVACAO", rotulo: "Aguardando aprovação" }, { valor: "APROVADO", rotulo: "Aprovados" }, { valor: "REJEITADO", rotulo: "Rejeitados" },
];
const ROTULOS: Record<StatusOrcamento, string> = { EM_ELABORACAO: "Em elaboração", AGUARDANDO_APROVACAO: "Aguardando aprovação", APROVADO: "Aprovado", REJEITADO: "Rejeitado" };
const moeda = (v: number) => new Intl.NumberFormat("pt-BR", { style: "currency", currency: "BRL" }).format(v ?? 0);
const dataCurta = (v: string) => { const d = new Date(v); return Number.isNaN(d.getTime()) ? "—" : new Intl.DateTimeFormat("pt-BR").format(d); };

export function OrcamentosScreen() {
  const router = useRouter();
  const [dados, setDados] = useState<OrcamentoResumo[]>([]);
  const [filtro, setFiltro] = useState<StatusOrcamento | "TODOS">("TODOS");
  const [pagina, setPagina] = useState(1); const [total, setTotal] = useState(0);
  const [carregando, setCarregando] = useState(true); const [erro, setErro] = useState("");
  const tamanho = 10;
  const carregar = useCallback(async () => {
    try {
      const r = await listarOrcamentos(pagina, tamanho, filtro === "TODOS" ? undefined : filtro);
      setDados(r.conteudo ?? []);
      setTotal(r.total_elementos ?? 0);
      setErro("");
    } catch (e) {
      setErro(getApiErrorMessage(e));
    } finally {
      setCarregando(false);
    }
  }, [pagina, filtro]);
useEffect(() => {
    let ativo = true;
    listarOrcamentos(pagina, tamanho, filtro === "TODOS" ? undefined : filtro)
      .then((r) => { if (ativo) { setDados(r.conteudo ?? []); setTotal(r.total_elementos ?? 0); setErro(""); } })
      .catch((e) => { if (ativo) setErro(getApiErrorMessage(e)); })
      .finally(() => { if (ativo) setCarregando(false); });
    return () => { ativo = false; };
  }, [pagina, filtro]);
  const colunas: ColumnDef<OrcamentoResumo>[] = [
    { key: "id", header: "Código", render: (x) => <Text className="text-sm font-semibold tracking-wide text-neutral-800">{codigoOrcamento(x.id)}</Text> },
    { key: "status", header: "Status", render: (x) => <Text className="text-xs font-semibold text-[#6f4f28]">{ROTULOS[x.status] ?? x.status}</Text> },
    { key: "valor_total", header: "Valor", render: (x) => <Text className="text-sm font-semibold text-neutral-800">{moeda(x.valor_total)}</Text> },
    { key: "data_expiracao", header: "Expira em", render: (x) => <Text className="text-sm text-neutral-600">{dataCurta(x.data_expiracao)}</Text> },
    { key: "versao", header: "Versão", width: 70, render: (x) => <Text className="text-sm text-neutral-600">{x.versao}</Text> },
  ];
  return <ScrollView className="flex-1 bg-[#fbfaf8]" contentContainerStyle={{ flexGrow: 1, paddingBottom: 40 }}><View className="mx-auto w-full max-w-5xl gap-6 px-4 py-8 md:px-8">
    <CabecalhoTela contexto="Operacional" secao="Orçamentos" titulo="Orçamentos" descricao="Consulte, acompanhe e elabore propostas para seus clientes." rotaFallback="/admin" acoes={<Pressable onPress={() => router.push("/orcamentos/novo")} className="flex-row items-center gap-2 rounded-lg bg-[#6f4f28] px-5 py-3"><Ionicons name="add" size={18} color="white"/><Text className="font-medium text-white">Novo orçamento</Text></Pressable>} />
    <View className="flex-row flex-wrap gap-2">{FILTROS.map((x) => <Pressable key={x.valor} onPress={() => { setCarregando(true); setErro(""); setPagina(1); setFiltro(x.valor); }} className={`rounded-full border px-3 py-2 ${filtro === x.valor ? "border-[#6f4f28] bg-[#6f4f28]" : "border-neutral-300 bg-white"}`}><Text className={`text-xs font-medium ${filtro === x.valor ? "text-white" : "text-neutral-700"}`}>{x.rotulo}</Text></Pressable>)}</View>
    {carregando ? <View className="items-center rounded-2xl border border-neutral-200 bg-white p-12"><ActivityIndicator color="#6f4f28"/><Text className="mt-3 text-sm text-neutral-500">Carregando orçamentos...</Text></View> : erro ? <View className="items-center gap-3 rounded-2xl border border-neutral-200 bg-white p-10"><Text className="text-center text-sm text-red-600">{erro}</Text><Pressable onPress={() => { setCarregando(true); setErro(""); void carregar(); }} className="rounded-lg border border-neutral-300 px-5 py-2"><Text>Tentar novamente</Text></Pressable></View> : <DataTable data={dados} columns={colunas} keyExtractor={(x) => x.id} onRowPress={(x) => router.push(`/orcamentos/${x.id}`)} emptyMessage="Nenhum orçamento encontrado." scrollEnabled={false}/>}
    <View className="flex-row items-center justify-between rounded-xl border border-neutral-200 bg-white px-4 py-3"><Text className="text-xs text-neutral-500">{total} orçamento(s) · Página {pagina} de {Math.max(1, Math.ceil(total / tamanho))}</Text><View className="flex-row gap-2"><Pressable disabled={pagina <= 1 || carregando} onPress={() => { setCarregando(true); setPagina((p) => p - 1); }} className="rounded-lg border border-neutral-300 px-3 py-2 disabled:opacity-40"><Text className="text-sm">Anterior</Text></Pressable><Pressable disabled={pagina * tamanho >= total || carregando} onPress={() => { setCarregando(true); setPagina((p) => p + 1); }} className="rounded-lg border border-neutral-300 px-3 py-2 disabled:opacity-40"><Text className="text-sm">Próxima</Text></Pressable></View></View>
  </View></ScrollView>;
}
