import { useEffect, useMemo, useState } from "react";
import { ActivityIndicator, Pressable, ScrollView, Text, TextInput, View } from "react-native";
import { useLocalSearchParams, useRouter } from "expo-router";
import { Ionicons } from "@expo/vector-icons";
import { coreApi, getApiErrorMessage } from "../../lib/api";
import { CabecalhoTela } from "../navegacao/CabecalhoTela";
import { listarSolicitacoes } from "../solicitacoes/solicitacoesApi";
import type { SolicitacaoResponse } from "../solicitacoes/types";
import { criarCabecalho, salvarItens, type OrcamentoItemInput, type UnidadeCobranca } from "./orcamentosApi";

interface Idioma { id: string; nome: string; codigoIso: string; }
interface TipoServico { id: string; nome: string; ativo: boolean; }
interface ServicoForm { tipo: string; quantidade: string; unidade: UnidadeCobranca; valor: string; }
interface ItemForm { origem: string; destino: string; servicos: ServicoForm[]; }
const UNIDADES: UnidadeCobranca[] = ["PALAVRA", "PAGINA", "HORA"];
const novoServico = (): ServicoForm => ({ tipo: "", quantidade: "", unidade: "PALAVRA", valor: "" });
const novoItem = (origem = "", destino = ""): ItemForm => ({ origem, destino, servicos: [novoServico()] });
const hojeMaisSete = () => { const d = new Date(); d.setDate(d.getDate() + 7); return d.toISOString().slice(0, 10); };
const numerico = (valor: string) => Number(valor.replace(",", "."));
const positivo = (valor: string) => Number.isFinite(numerico(valor)) && numerico(valor) > 0;
const dinheiro = (valor: number) => new Intl.NumberFormat("pt-BR", { style: "currency", currency: "BRL" }).format(valor || 0);

function Campo({ label, value, onChangeText, placeholder, keyboardType = "default", error }: { label: string; value: string; onChangeText: (text: string) => void; placeholder?: string; keyboardType?: "default" | "numeric" | "decimal-pad"; error?: string }) {
  return <View className="min-w-0 flex-1 gap-1"><Text className="text-xs font-medium text-neutral-600">{label}</Text><TextInput value={value} onChangeText={onChangeText} placeholder={placeholder} keyboardType={keyboardType} className={`rounded-lg border bg-white px-3 py-2.5 text-sm text-neutral-800 ${error ? "border-red-500" : "border-neutral-300"}`} />{error ? <Text className="text-xs text-red-600">{error}</Text> : null}</View>;
}

export function OrcamentoNovoScreen() {
  const router = useRouter();
  const params = useLocalSearchParams<{ solicitacaoId?: string }>();
  const [solicitacoes, setSolicitacoes] = useState<SolicitacaoResponse[]>([]);
  const [solicitacaoId, setSolicitacaoId] = useState(params.solicitacaoId ?? "");
  const solicitacao = solicitacoes.find((s) => s.id === solicitacaoId);
  const [idiomas, setIdiomas] = useState<Idioma[]>([]);
  const [tipos, setTipos] = useState<TipoServico[]>([]);
  const [dataExpiracao, setDataExpiracao] = useState(hojeMaisSete());
  const [orcamentoId, setOrcamentoId] = useState("");
  const [itens, setItens] = useState<ItemForm[]>([novoItem()]);
  const [carregando, setCarregando] = useState(true);
  const [salvando, setSalvando] = useState(false);
  const [erro, setErro] = useState("");

  useEffect(() => {
    let ativo = true;
    Promise.all([
      listarSolicitacoes(),
      coreApi.get<{ content: Idioma[] }>("/api/v1/idiomas", { params: { ativo: true, size: 100 } }),
      coreApi.get<{ content: TipoServico[] }>("/api/v1/tipos-servico", { params: { ativo: true, size: 100 } }),
    ]).then(([sols, idi, serv]) => {
      if (!ativo) return;
      setSolicitacoes(sols);
      setIdiomas(idi.data.content ?? []);
      setTipos(serv.data.content ?? []);
      const escolha = params.solicitacaoId ? sols.find((s) => s.id === params.solicitacaoId) : sols.find((s) => s.status === "PENDENTE_ORCAMENTO") ?? sols[0];
      if (escolha) { setSolicitacaoId(escolha.id); setItens([novoItem(escolha.idiomaOrigem.id, escolha.idiomaDestino.id)]); }
    }).catch((e) => { if (ativo) setErro(getApiErrorMessage(e)); }).finally(() => { if (ativo) setCarregando(false); });
    return () => { ativo = false; };
  }, [params.solicitacaoId]);


  const total = useMemo(() => itens.reduce((soma, item) => soma + item.servicos.reduce((sub, serv) => sub + numerico(serv.quantidade) * numerico(serv.valor), 0), 0), [itens]);
  const idiomaLabel = (id: string) => { const idioma = idiomas.find((i) => i.id === id); return idioma ? `${idioma.nome} (${idioma.codigoIso.toUpperCase()})` : "Selecione"; };
  const mudarItem = (index: number, patch: Partial<ItemForm>) => setItens((atual) => atual.map((item, i) => i === index ? { ...item, ...patch } : item));
  const mudarServico = (itemIndex: number, servicoIndex: number, patch: Partial<ServicoForm>) => setItens((atual) => atual.map((item, i) => i === itemIndex ? { ...item, servicos: item.servicos.map((serv, j) => j === servicoIndex ? { ...serv, ...patch } : serv) } : item));

  const iniciar = async () => {
    if (!solicitacao) return;
    setErro(""); setSalvando(true);
    try {
      const expiracao = new Date(`${dataExpiracao}T23:59:59.000Z`);
      if (Number.isNaN(expiracao.getTime()) || expiracao.getTime() <= Date.now()) { setErro("A data de expiração deve ser futura."); return; }
      const criado = await criarCabecalho(solicitacao.id, solicitacao.cliente.id, expiracao.toISOString());
      setOrcamentoId(criado.id);
    } catch (e) { setErro(getApiErrorMessage(e)); }
    finally { setSalvando(false); }
  };

  const submeter = async () => {
    setErro("");
    const validos = itens.length > 0 && itens.every((item) => item.origem && item.destino && item.origem !== item.destino && item.servicos.length > 0 && item.servicos.every((s) => s.tipo && positivo(s.quantidade) && positivo(s.valor)));
    if (!validos) { setErro("Revise os idiomas e preencha todos os serviços com quantidade e valor maiores que zero."); return; }
    const payload: OrcamentoItemInput[] = itens.map((item, idx) => ({
      ordem: idx + 1, idioma_origem_id: item.origem, idioma_destino_id: item.destino,
      servicos: item.servicos.map((serv) => ({ tipo_servico: serv.tipo, quantidade: numerico(serv.quantidade), unidade: serv.unidade, valor_unitario: numerico(serv.valor) })),
    }));
    setSalvando(true);
    try { await salvarItens(orcamentoId, payload); router.replace(`/orcamentos/${orcamentoId}`); }
    catch (e) { setErro(getApiErrorMessage(e)); }
    finally { setSalvando(false); }
  };

  return <ScrollView className="flex-1 bg-[#fbfaf8]" contentContainerStyle={{ flexGrow: 1, paddingBottom: 40 }} keyboardShouldPersistTaps="handled">
    <View className="mx-auto w-full max-w-5xl gap-5 px-4 py-8 md:px-8">
      <CabecalhoTela contexto="Operacional" secao="Orçamentos" titulo={orcamentoId ? "Elaborar orçamento" : "Novo orçamento"} descricao="Selecione a solicitação e monte a proposta com os serviços e valores." rotaFallback="/orcamentos" />
      {carregando ? <View className="items-center rounded-xl bg-white p-10"><ActivityIndicator color="#6f4f28" /></View> : null}
      {erro ? <View className="rounded-lg border border-red-200 bg-red-50 p-3"><Text className="text-sm text-red-700">{erro}</Text></View> : null}
      {!orcamentoId ? <View className="gap-4 rounded-2xl border border-neutral-200 bg-white p-5">
        <Text className="text-lg font-semibold text-neutral-900">1. Cabeçalho da proposta</Text>
        <Text className="text-sm text-neutral-600">Escolha a solicitação que originou este orçamento.</Text>
        {solicitacoes.length === 0 && !carregando ? <Text className="text-sm text-neutral-500">Não há solicitações disponíveis.</Text> : null}
        <View className="gap-2">{solicitacoes.map((s) => <Pressable key={s.id} onPress={() => { setSolicitacaoId(s.id); setItens([novoItem(s.idiomaOrigem.id, s.idiomaDestino.id)]); }} className={`flex-row items-center justify-between rounded-xl border p-3 ${solicitacaoId === s.id ? "border-[#6f4f28] bg-[#f8f2ec]" : "border-neutral-200"}`}>
          <View className="flex-1"><Text className="text-sm font-semibold text-neutral-800">{s.cliente.nomeEmpresa}</Text><Text className="mt-1 text-xs text-neutral-500">{s.categoriaProjeto.nome} · {s.idiomaOrigem.nome} → {s.idiomaDestino.nome}</Text></View>
          <Ionicons name={solicitacaoId === s.id ? "checkmark-circle" : "ellipse-outline"} size={22} color="#6f4f28" />
        </Pressable>)}</View>
        <Campo label="Data de expiração" value={dataExpiracao} onChangeText={setDataExpiracao} placeholder="AAAA-MM-DD" />
        <Pressable disabled={!solicitacao || salvando || carregando} onPress={() => void iniciar()} className="flex-row items-center justify-center gap-2 rounded-lg bg-[#6f4f28] px-5 py-3 disabled:opacity-50">{salvando ? <ActivityIndicator color="white" /> : null}<Text className="font-semibold text-white">Criar cabeçalho e continuar</Text></Pressable>
      </View> : <>
        <View className="flex-row items-center gap-3 rounded-xl border border-green-200 bg-green-50 p-4"><Ionicons name="checkmark-circle" size={22} color="#15803d" /><View><Text className="text-sm font-semibold text-green-800">Cabeçalho criado</Text><Text className="text-xs text-green-700">ID {orcamentoId}</Text></View></View>
        <View className="gap-4 rounded-2xl border border-neutral-200 bg-white p-5">
          <View className="flex-row items-center justify-between"><View><Text className="text-lg font-semibold text-neutral-900">2. Itens e serviços</Text><Text className="text-sm text-neutral-500">Adicione pares de idiomas e precifique cada serviço.</Text></View></View>
          {itens.map((item, idx) => <View key={idx} className="gap-4 rounded-xl border border-neutral-200 bg-neutral-50 p-4">
            <View className="flex-row items-center justify-between"><Text className="font-semibold text-neutral-800">Par de idiomas {idx + 1}</Text>{itens.length > 1 ? <Pressable onPress={() => setItens((atual) => atual.filter((_, i) => i !== idx))}><Text className="text-xs text-red-600">Remover</Text></Pressable> : null}</View>
            <View className="flex-row flex-wrap gap-2"><View className="min-w-[140px] flex-1 gap-1"><Text className="text-xs text-neutral-600">Origem</Text><View className="flex-row flex-wrap gap-1">{idiomas.map((idioma) => <Pressable key={idioma.id} onPress={() => mudarItem(idx, { origem: idioma.id })} className={`rounded-md border px-2 py-1.5 ${item.origem === idioma.id ? "border-[#6f4f28] bg-[#f4e9df]" : "border-neutral-200 bg-white"}`}><Text className="text-xs text-neutral-700">{idiomaLabel(idioma.id)}</Text></Pressable>)}</View></View>
            <View className="min-w-[140px] flex-1 gap-1"><Text className="text-xs text-neutral-600">Destino</Text><View className="flex-row flex-wrap gap-1">{idiomas.map((idioma) => <Pressable key={idioma.id} onPress={() => mudarItem(idx, { destino: idioma.id })} className={`rounded-md border px-2 py-1.5 ${item.destino === idioma.id ? "border-[#6f4f28] bg-[#f4e9df]" : "border-neutral-200 bg-white"}`}><Text className="text-xs text-neutral-700">{idiomaLabel(idioma.id)}</Text></Pressable>)}</View></View></View>
            {item.origem && item.origem === item.destino ? <Text className="text-xs text-red-600">Origem e destino precisam ser diferentes.</Text> : null}
            {item.servicos.map((serv, si) => <View key={si} className="gap-2 rounded-lg border border-neutral-200 bg-white p-3"><View className="flex-row items-center justify-between"><Text className="text-sm font-medium text-neutral-700">Serviço {si + 1}</Text>{item.servicos.length > 1 ? <Pressable onPress={() => mudarItem(idx, { servicos: item.servicos.filter((_, j) => j !== si) })}><Text className="text-xs text-red-600">Remover</Text></Pressable> : null}</View>
              <View className="flex-row flex-wrap gap-2">{tipos.map((t) => <Pressable key={t.id} onPress={() => mudarServico(idx, si, { tipo: t.nome })} className={`rounded-full border px-3 py-1.5 ${serv.tipo === t.nome ? "border-[#6f4f28] bg-[#6f4f28]" : "border-neutral-300"}`}><Text className={`text-xs ${serv.tipo === t.nome ? "text-white" : "text-neutral-700"}`}>{t.nome}</Text></Pressable>)}</View>
              <View className="flex-row flex-wrap gap-2"><Campo label="Quantidade" value={serv.quantidade} onChangeText={(v) => mudarServico(idx, si, { quantidade: v })} keyboardType="decimal-pad" placeholder="Ex.: 1500" error={serv.quantidade && !positivo(serv.quantidade) ? "Deve ser maior que zero" : undefined} /><View className="min-w-[105px] flex-1 gap-1"><Text className="text-xs font-medium text-neutral-600">Unidade</Text><View className="flex-row flex-wrap gap-1">{UNIDADES.map((u) => <Pressable key={u} onPress={() => mudarServico(idx, si, { unidade: u })} className={`rounded-md border px-2 py-2 ${serv.unidade === u ? "border-[#6f4f28] bg-[#f4e9df]" : "border-neutral-300 bg-white"}`}><Text className="text-[10px] text-neutral-700">{u}</Text></Pressable>)}</View></View><Campo label="Valor unitário (R$)" value={serv.valor} onChangeText={(v) => mudarServico(idx, si, { valor: v })} keyboardType="decimal-pad" placeholder="0,00" error={serv.valor && !positivo(serv.valor) ? "Deve ser maior que zero" : undefined} /></View>
              <Text className="text-right text-xs text-neutral-500">Subtotal: {dinheiro(numerico(serv.quantidade) * numerico(serv.valor))}</Text>
            </View>)}
            <Pressable onPress={() => mudarItem(idx, { servicos: [...item.servicos, novoServico()] })} className="self-start rounded-lg border border-[#6f4f28] px-3 py-2"><Text className="text-xs font-medium text-[#6f4f28]">+ Adicionar serviço</Text></Pressable>
          </View>)}
          <Pressable onPress={() => setItens((atual) => [...atual, novoItem()])} className="self-start rounded-lg border border-neutral-300 bg-white px-4 py-2.5"><Text className="text-sm font-medium text-neutral-700">+ Adicionar par de idiomas</Text></Pressable>
          <View className="flex-row items-center justify-between rounded-xl bg-[#f4e9df] p-4"><Text className="font-semibold text-neutral-800">Total estimado</Text><Text className="text-lg font-bold text-[#6f4f28]">{dinheiro(total)}</Text></View>
          <Pressable disabled={salvando || itens.some((i) => !i.origem || i.origem === i.destino || !i.servicos.length || i.servicos.some((s) => !s.tipo || !positivo(s.quantidade) || !positivo(s.valor)))} onPress={() => void submeter()} className="flex-row items-center justify-center gap-2 rounded-lg bg-[#6f4f28] px-5 py-3 disabled:opacity-50">{salvando ? <ActivityIndicator color="white" /> : null}<Text className="font-semibold text-white">Salvar orçamento</Text></Pressable>
        </View>
      </>}
    </View>
  </ScrollView>;
}
