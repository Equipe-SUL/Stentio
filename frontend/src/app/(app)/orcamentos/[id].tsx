import { useCallback, useState } from "react";
import { ActivityIndicator, Pressable, ScrollView, Text, View } from "react-native";
import { useFocusEffect, useLocalSearchParams, useRouter } from "expo-router";
import { Ionicons } from "@expo/vector-icons";
import { coreApi, getApiErrorMessage } from "../../../lib/api";
import { useSession } from "../../../lib/session";
import { CabecalhoTela } from "../../../components/navegacao/CabecalhoTela";
import { buscarOrcamento, codigoOrcamento, enviarOrcamentoAoCliente, type OrcamentoDetalhe } from "../../../components/orcamentos/orcamentosApi";
import { buscarSolicitacao } from "../../../components/solicitacoes/solicitacoesApi";
import type { SolicitacaoResponse } from "../../../components/solicitacoes/types";
import React from "react";

const moeda = (v: number) => new Intl.NumberFormat("pt-BR", { style: "currency", currency: "BRL" }).format(v ?? 0);
const data = (v: string) => { const d = new Date(v); return Number.isNaN(d.getTime()) ? "—" : new Intl.DateTimeFormat("pt-BR", { dateStyle: "medium" }).format(d); };
const statusLabel: Record<string, string> = { EM_ELABORACAO: "Em elaboração", AGUARDANDO_APROVACAO: "Aguardando aprovação", APROVADO: "Aprovado", REJEITADO: "Rejeitado" };
interface IdiomaResumo { id: string; nome: string; }

export default function OrcamentoDetalheRoute() {
  const { id } = useLocalSearchParams<{ id: string }>(); const router = useRouter();
  const { usuario } = useSession();
  const [orcamento, setOrcamento] = useState<OrcamentoDetalhe | null>(null); const [erro, setErro] = useState(""); const [carregando, setCarregando] = useState(true);
  const [enviando, setEnviando] = useState(false);
  const [mensagemEnvio, setMensagemEnvio] = useState("");
  const [erroEnvio, setErroEnvio] = useState("");
  const [idCarregado, setIdCarregado] = useState("");
  const [solicitacao, setSolicitacao] = useState<SolicitacaoResponse | null>(null);
  const [erroSolicitacao, setErroSolicitacao] = useState("");
  const [nomesIdiomas, setNomesIdiomas] = useState<Record<string, string>>({});
  const aguardandoOrcamento = carregando || idCarregado !== id;
  useFocusEffect(useCallback(() => {
    if (!id) return;
    setCarregando(true);
    let ativo = true;
    buscarOrcamento(id)
      .then(async (dados) => {
        if (!ativo) return;
        setOrcamento(dados);
        setIdCarregado(id);
        setErro("");
        setSolicitacao(null);
        setErroSolicitacao("");
        setNomesIdiomas({});
        await Promise.all([
          buscarSolicitacao(dados.solicitacao_id)
            .then((dadosSolicitacao) => { if (ativo) setSolicitacao(dadosSolicitacao); })
            .catch((e) => { if (ativo) setErroSolicitacao(getApiErrorMessage(e)); }),
          coreApi.get<{ content: IdiomaResumo[] }>("/api/v1/idiomas", { params: { size: 100 } })
            .then(({ data: paginaIdiomas }) => {
              if (ativo) setNomesIdiomas(Object.fromEntries((paginaIdiomas.content ?? []).map((idioma) => [idioma.id, idioma.nome])));
            })
            .catch(() => { if (ativo) setNomesIdiomas({}); }),
        ]);
      })
      .catch((e) => { if (ativo) { setErro(getApiErrorMessage(e)); setIdCarregado(id); } })
      .finally(() => { if (ativo) setCarregando(false); });
    return () => { ativo = false; };
  }, [id]));
  const podeEditar = orcamento?.status === "EM_ELABORACAO";
  const temValores = Boolean(orcamento?.itens.length && orcamento.valor_total > 0);
  const podeEnviarAoCliente =
    (usuario?.role === "ADMIN" || usuario?.role === "ATENDENTE")
    && (orcamento?.status === "EM_ELABORACAO" || orcamento?.status === "REJEITADO");

  async function enviarAoCliente() {
    if (!id || enviando) return;
    setEnviando(true);
    setErroEnvio("");
    setMensagemEnvio("");
    try {
      const resposta = await enviarOrcamentoAoCliente(id);
      setOrcamento(await buscarOrcamento(id));
      setMensagemEnvio(resposta.mensagem);
    } catch (error) {
      setErroEnvio(getApiErrorMessage(error));
    } finally {
      setEnviando(false);
    }
  }

  return <ScrollView className="flex-1 bg-[#fbfaf8]" contentContainerStyle={{ flexGrow: 1, paddingBottom: 40 }}><View className="mx-auto w-full max-w-5xl gap-3 px-4 py-8 md:px-8">
    <CabecalhoTela contexto="Operacional" secao="Orçamentos" titulo="Detalhes do orçamento" descricao="Proposta e serviços cadastrados." rotaFallback="/orcamentos" compacto acoes={<Pressable onPress={() => router.push("/orcamentos")} className="flex-row items-center gap-2 rounded-lg border border-neutral-300 bg-white px-4 py-3"><Ionicons name="list-outline" size={18} color="#6f4f28" /><Text className="font-medium text-neutral-700">Listagem</Text></Pressable>} />
    {aguardandoOrcamento ? <View className="items-center rounded-xl bg-white p-10"><ActivityIndicator color="#6f4f28" /></View> : erro ? <View className="rounded-xl border border-red-200 bg-red-50 p-4"><Text className="text-sm text-red-700">{erro}</Text></View> : orcamento ? <>
      <View className="flex-row flex-wrap gap-3 rounded-2xl border border-neutral-200 bg-white p-5"><View className="w-full"><Text className="text-xs font-medium text-neutral-500">Código do orçamento</Text><Text selectable className="mt-1 text-2xl font-bold tracking-wider text-neutral-900">{codigoOrcamento(orcamento.id)}</Text></View><View className="min-w-[180px] flex-1"><Text className="text-xs text-neutral-500">Status</Text><Text className="mt-1 font-semibold text-[#6f4f28]">{statusLabel[orcamento.status] ?? orcamento.status}</Text></View><View className="min-w-[180px] flex-1"><Text className="text-xs text-neutral-500">Valor total</Text><Text className="mt-1 text-xl font-bold text-neutral-900">{moeda(orcamento.valor_total)}</Text></View><View className="min-w-[180px] flex-1"><Text className="text-xs text-neutral-500">Expira em</Text><Text className="mt-1 font-medium text-neutral-800">{data(orcamento.data_expiracao)}</Text></View><View className="min-w-[180px] flex-1"><Text className="text-xs text-neutral-500">Versão</Text><Text className="mt-1 font-medium text-neutral-800">{orcamento.versao}</Text></View></View>
      {podeEnviarAoCliente && (!temValores || erroEnvio || mensagemEnvio) ? <View className="gap-2 rounded-xl border border-neutral-200 bg-white p-4">
        {!temValores ? <Text className="text-sm text-amber-700">Preencha os itens e valores antes de enviar o orçamento ao cliente.</Text> : null}
        {erroEnvio ? <Text className="text-sm text-red-700">{erroEnvio}</Text> : null}
        {mensagemEnvio ? <Text className="text-sm text-green-700">{mensagemEnvio}</Text> : null}
      </View> : null}
      <View className="flex-row flex-wrap items-start gap-3">
        {podeEditar ? <Pressable onPress={() => router.push({ pathname: "/orcamentos/novo", params: { orcamentoId: orcamento.id } })} className="flex-row items-center gap-2 self-start rounded-lg border border-[#6f4f28] bg-white px-5 py-3"><Ionicons name="create-outline" size={18} color="#6f4f28" /><Text className="font-semibold text-[#6f4f28]">Editar itens e valores</Text></Pressable> : null}
        {podeEnviarAoCliente ? <Pressable
          accessibilityRole="button"
          disabled={enviando || !temValores}
          onPress={() => void enviarAoCliente()}
          className="flex-row items-center justify-center gap-2 self-start rounded-lg bg-[#6f4f28] px-5 py-3 disabled:opacity-50"
        >
          {enviando ? <ActivityIndicator color="white" /> : <Ionicons name="mail-outline" size={18} color="white" />}
          <Text className="font-semibold text-white">{enviando ? "Enviando..." : "Enviar orçamento ao cliente"}</Text>
        </Pressable> : null}
      </View>
      <View className="gap-4 rounded-2xl border border-neutral-200 bg-white p-5"><Text className="text-lg font-semibold text-neutral-900">Itens da proposta</Text>{orcamento.itens?.length ? orcamento.itens.map((item, i) => <View key={item.id ?? item.ordem} className="gap-3 rounded-xl border border-neutral-200 bg-neutral-50 p-4"><View className="flex-row items-center gap-2"><Ionicons name="swap-horizontal-outline" size={18} color="#6f4f28" /><Text className="font-semibold text-neutral-800">Item {i + 1}</Text><Text className="ml-auto font-semibold text-neutral-700">{moeda(item.subtotal_item ?? 0)}</Text></View><View className="gap-1"><Text className="text-xs text-neutral-600">Idioma de origem: {nomesIdiomas[item.idioma_origem_id] ?? "Idioma não disponível"}</Text><Text className="text-xs text-neutral-600">Idioma de destino: {nomesIdiomas[item.idioma_destino_id] ?? "Idioma não disponível"}</Text></View>{item.servicos.map((serv, j) => <View key={serv.id ?? j} className="flex-row flex-wrap items-center justify-between gap-2 rounded-lg bg-white p-3"><View><Text className="font-medium text-neutral-800">{serv.tipo_servico}</Text><Text className="text-xs text-neutral-500">{serv.quantidade} {serv.unidade} × {moeda(serv.valor_unitario)}</Text></View><Text className="font-semibold text-neutral-700">{moeda(serv.subtotal ?? serv.quantidade * serv.valor_unitario)}</Text></View>)}</View>) : <Text className="text-sm text-neutral-500">Este orçamento ainda não possui itens.</Text>}</View>
      <View className="gap-4 rounded-xl border border-neutral-200 bg-white p-5">
        <Text className="text-lg font-semibold text-neutral-900">Solicitação vinculada</Text>
        {erroSolicitacao ? <Text className="text-sm text-amber-700">Não foi possível carregar os dados da solicitação: {erroSolicitacao}</Text> : solicitacao ? <>
          <View className="flex-row flex-wrap gap-4">
            <View className="min-w-[180px] flex-1"><Text className="text-xs text-neutral-500">Cliente</Text><Text className="mt-1 font-medium text-neutral-800">{solicitacao.cliente.nomeEmpresa}</Text></View>
            <View className="min-w-[180px] flex-1"><Text className="text-xs text-neutral-500">Categoria</Text><Text className="mt-1 font-medium text-neutral-800">{solicitacao.categoriaProjeto.nome}</Text></View>
            <View className="min-w-[180px] flex-1"><Text className="text-xs text-neutral-500">Status da solicitação</Text><Text className="mt-1 font-medium text-neutral-800">{solicitacao.status}</Text></View>
            <View className="min-w-[180px] flex-1"><Text className="text-xs text-neutral-500">Recebida em</Text><Text className="mt-1 font-medium text-neutral-800">{data(solicitacao.dataRecebimento)}</Text></View>
          </View>
          <Text className="text-sm text-neutral-700">Idiomas: {solicitacao.idiomaOrigem.nome} ({solicitacao.idiomaOrigem.codigoIso.toUpperCase()}) → {solicitacao.idiomaDestino.nome} ({solicitacao.idiomaDestino.codigoIso.toUpperCase()})</Text>
          {solicitacao.descricao ? <View className="gap-1"><Text className="text-xs text-neutral-500">Descrição</Text><Text className="text-sm leading-5 text-neutral-700">{solicitacao.descricao}</Text></View> : null}
          <Text selectable className="text-xs text-neutral-400">Solicitação {solicitacao.id}</Text>
        </> : <ActivityIndicator color="#6f4f28" />}
      </View>
    </> : null}
  </View></ScrollView>;
}
