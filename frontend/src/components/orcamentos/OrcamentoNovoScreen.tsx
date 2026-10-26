import { createElement, useEffect, useMemo, useState } from "react";
import { ActivityIndicator, Platform, Pressable, ScrollView, Text, TextInput, View } from "react-native";
import { useLocalSearchParams, useRouter } from "expo-router";
import { Ionicons } from "@expo/vector-icons";
import { Picker } from "@react-native-picker/picker";
import { coreApi, getApiErrorMessage } from "../../lib/api";
import { CabecalhoTela } from "../navegacao/CabecalhoTela";
import { buscarSolicitacao, listarSolicitacoes } from "../solicitacoes/solicitacoesApi";
import type { SolicitacaoResponse } from "../solicitacoes/types";
import { SelectField } from "../solicitacoes/SelectField";
import { buscarOrcamento, codigoOrcamento, criarCabecalho, salvarItens, type OrcamentoItemInput, type UnidadeCobranca } from "./orcamentosApi";

interface Idioma { id: string; nome: string; codigoIso: string; }
interface TipoServico { id: string; nome: string; ativo: boolean; }
interface ServicoForm { tipo: string; quantidade: string; unidade: UnidadeCobranca; valor: string; }
interface ItemForm { origem: string; destino: string; servicos: ServicoForm[]; }
const UNIDADES: UnidadeCobranca[] = ["PALAVRA", "PAGINA", "HORA"];
const novoServico = (): ServicoForm => ({ tipo: "", quantidade: "", unidade: "PALAVRA", valor: "" });
const novoItem = (origem = "", destino = ""): ItemForm => ({ origem, destino, servicos: [novoServico()] });
const dataLocal = (data: Date) => `${data.getFullYear()}-${String(data.getMonth() + 1).padStart(2, "0")}-${String(data.getDate()).padStart(2, "0")}`;
const hojeMaisSete = () => { const d = new Date(); d.setDate(d.getDate() + 7); return dataLocal(d); };
const numerico = (valor: string) => Number(valor.replace(",", "."));
const positivo = (valor: string) => Number.isFinite(numerico(valor)) && numerico(valor) > 0;
const dinheiro = (valor: number) => new Intl.NumberFormat("pt-BR", { style: "currency", currency: "BRL" }).format(valor || 0);

function Campo({ label, value, onChangeText, placeholder, keyboardType = "default", error }: { label: string; value: string; onChangeText: (text: string) => void; placeholder?: string; keyboardType?: "default" | "numeric" | "decimal-pad"; error?: string }) {
  return <View className="min-w-0 flex-1 gap-1"><Text className="text-xs font-medium text-neutral-600">{label}</Text><TextInput value={value} onChangeText={onChangeText} placeholder={placeholder} keyboardType={keyboardType} className={`rounded-lg border bg-white px-3 py-2.5 text-sm text-neutral-800 ${error ? "border-red-500" : "border-neutral-300"}`} />{error ? <Text className="text-xs text-red-600">{error}</Text> : null}</View>;
}

function SeletorData({ value, onChange }: { value: string; onChange: (data: string) => void }) {
  const [ano, mes, dia] = value.split("-").map(Number);
  const hoje = dataLocal(new Date());
  const diasNoMes = new Date(ano, mes, 0).getDate();
  const anosDisponiveis = Array.from({ length: 6 }, (_, index) => new Date().getFullYear() + index);
  const atualizar = (novoAno: number, novoMes: number, novoDia: number) => {
    const diaValido = Math.min(novoDia, new Date(novoAno, novoMes, 0).getDate());
    const novaData = `${novoAno}-${String(novoMes).padStart(2, "0")}-${String(diaValido).padStart(2, "0")}`;
    if (novaData >= hoje) onChange(novaData);
  };

  return <View className="gap-1">
    <Text className="text-xs font-medium text-neutral-600">Data de expiração</Text>
    {Platform.OS === "web" ? createElement("input", {
      type: "date",
      value,
      min: hoje,
      onChange: (event) => onChange(event.currentTarget.value),
      "aria-label": "Data de expiração",
      style: { width: "100%", minHeight: 42, padding: "8px 12px", border: "1px solid #d4d4d4", borderRadius: 8, backgroundColor: "white", color: "#262626", fontSize: 14 },
    }) : <View className="flex-row gap-2">
      <View className="min-w-0 flex-[0.8] overflow-hidden rounded-lg border border-neutral-300 bg-white"><Picker selectedValue={dia} onValueChange={(novoDia) => atualizar(ano, mes, Number(novoDia))} style={{ color: "#171717" }} accessibilityLabel="Dia"><Picker.Item label="Dia" value={0} />{Array.from({ length: diasNoMes }, (_, index) => index + 1).map((opcao) => <Picker.Item key={opcao} label={String(opcao).padStart(2, "0")} value={opcao} />)}</Picker></View>
      <View className="min-w-0 flex-[1.4] overflow-hidden rounded-lg border border-neutral-300 bg-white"><Picker selectedValue={mes} onValueChange={(novoMes) => atualizar(ano, Number(novoMes), dia)} style={{ color: "#171717" }} accessibilityLabel="Mês">{Array.from({ length: 12 }, (_, index) => index + 1).map((opcao) => <Picker.Item key={opcao} label={new Intl.DateTimeFormat("pt-BR", { month: "long" }).format(new Date(2020, opcao - 1, 1))} value={opcao} />)}</Picker></View>
      <View className="min-w-0 flex-1 overflow-hidden rounded-lg border border-neutral-300 bg-white"><Picker selectedValue={ano} onValueChange={(novoAno) => atualizar(Number(novoAno), mes, dia)} style={{ color: "#171717" }} accessibilityLabel="Ano">{anosDisponiveis.map((opcao) => <Picker.Item key={opcao} label={String(opcao)} value={opcao} />)}</Picker></View>
    </View>}
  </View>;
}

export function OrcamentoNovoScreen() {
  const router = useRouter();
  const params = useLocalSearchParams<{ solicitacaoId?: string; orcamentoId?: string }>();
  const [solicitacoes, setSolicitacoes] = useState<SolicitacaoResponse[]>([]);
  const [solicitacaoId, setSolicitacaoId] = useState(params.solicitacaoId ?? "");
  const solicitacao = solicitacoes.find((s) => s.id === solicitacaoId);
  const [idiomas, setIdiomas] = useState<Idioma[]>([]);
  const [tipos, setTipos] = useState<TipoServico[]>([]);
  const [dataExpiracao, setDataExpiracao] = useState(hojeMaisSete());
  const [orcamentoId, setOrcamentoId] = useState("");
  const [etapaItens, setEtapaItens] = useState(false);
  const [itens, setItens] = useState<ItemForm[]>([novoItem()]);
  const [carregando, setCarregando] = useState(true);
  const [salvando, setSalvando] = useState(false);
  const [erro, setErro] = useState("");

  useEffect(() => {
    let ativo = true;
    setCarregando(true);
    setOrcamentoId("");
    setEtapaItens(false);
    setErro("");
    Promise.all([
      params.orcamentoId ? buscarOrcamento(params.orcamentoId) : Promise.resolve(null),
      params.orcamentoId ? Promise.resolve([]) : listarSolicitacoes(),
      coreApi.get<{ content: Idioma[] }>("/api/v1/idiomas", { params: { ativo: true, size: 100 } }),
      coreApi.get<{ content: TipoServico[] }>("/api/v1/tipos-servico", { params: { ativo: true, size: 100 } }),
    ]).then(async ([orcamento, sols, idi, serv]) => {
      if (orcamento && orcamento.status !== "EM_ELABORACAO") {
        if (ativo) setErro("Somente orçamentos em elaboração podem ser editados.");
        return;
      }
      const vinculada = orcamento ? await buscarSolicitacao(orcamento.solicitacao_id) : null;
      if (!ativo) return;
      const disponiveis = vinculada ? [vinculada] : sols.filter((s) => s.status === "PENDENTE_ORCAMENTO");
      setSolicitacoes(disponiveis);
      setIdiomas(idi.data.content ?? []);
      setTipos(serv.data.content ?? []);
      const escolha = vinculada ?? (params.solicitacaoId
        ? disponiveis.find((s) => s.id === params.solicitacaoId)
        : disponiveis[0]);
      setSolicitacaoId(escolha?.id ?? "");
      if (orcamento) {
        setOrcamentoId(orcamento.id);
        setEtapaItens(true);
      }
      if (orcamento?.itens.length) {
        setItens(orcamento.itens.map((item) => ({
          origem: item.idioma_origem_id, destino: item.idioma_destino_id,
          servicos: item.servicos.map((servico) => ({
            tipo: servico.tipo_servico, quantidade: String(servico.quantidade),
            unidade: servico.unidade, valor: String(servico.valor_unitario),
          })),
        })));
      } else if (escolha) setItens([novoItem(escolha.idiomaOrigem.id, escolha.idiomaDestino.id)]);
    }).catch((e) => { if (ativo) setErro(getApiErrorMessage(e)); }).finally(() => { if (ativo) setCarregando(false); });
    return () => { ativo = false; };
  }, [params.solicitacaoId, params.orcamentoId]);


  const total = useMemo(() => itens.reduce((soma, item) => soma + item.servicos.reduce((sub, serv) => sub + numerico(serv.quantidade) * numerico(serv.valor), 0), 0), [itens]);
  const itensValidos = itens.length > 0 && itens.every((item) => item.origem && item.destino && item.origem !== item.destino && item.servicos.length > 0 && item.servicos.every((s) => s.tipo.trim() && positivo(s.quantidade) && positivo(s.valor)));
  const opcoesSolicitacao = solicitacoes.map((s) => ({ value: s.id, label: `${s.cliente.nomeEmpresa} · ${s.categoriaProjeto.nome} · ${s.idiomaOrigem.codigoIso.toUpperCase()} → ${s.idiomaDestino.codigoIso.toUpperCase()}` }));
  const opcoesIdioma = idiomas.map((idioma) => ({ value: idioma.id, label: `${idioma.nome} (${idioma.codigoIso.toUpperCase()})` }));
  const opcoesTipoServico = tipos.map((tipo) => ({ value: tipo.nome, label: tipo.nome }));
  const mudarItem = (index: number, patch: Partial<ItemForm>) => setItens((atual) => atual.map((item, i) => i === index ? { ...item, ...patch } : item));
  const mudarServico = (itemIndex: number, servicoIndex: number, patch: Partial<ServicoForm>) => setItens((atual) => atual.map((item, i) => i === itemIndex ? { ...item, servicos: item.servicos.map((serv, j) => j === servicoIndex ? { ...serv, ...patch } : serv) } : item));

  const iniciar = () => {
    if (!solicitacao) return;
    setErro("");
    const expiracao = new Date(`${dataExpiracao}T23:59:59.000Z`);
    if (Number.isNaN(expiracao.getTime()) || expiracao.getTime() <= Date.now()) { setErro("A data de expiração deve ser futura."); return; }
    setEtapaItens(true);
  };

  const submeter = async () => {
    if (salvando || carregando || !solicitacao) return;
    setErro("");
    if (!itensValidos) { setErro("Revise os idiomas e preencha todos os serviços com quantidade e valor maiores que zero."); return; }
    const payload: OrcamentoItemInput[] = itens.map((item, idx) => ({
      ordem: idx + 1, idioma_origem_id: item.origem, idioma_destino_id: item.destino,
      servicos: item.servicos.map((serv) => ({ tipo_servico: serv.tipo, quantidade: numerico(serv.quantidade), unidade: serv.unidade, valor_unitario: numerico(serv.valor) })),
    }));
    setSalvando(true);
    try {
      let id = orcamentoId;
      if (!id) {
        const expiracao = new Date(`${dataExpiracao}T23:59:59.000Z`);
        if (Number.isNaN(expiracao.getTime()) || expiracao.getTime() <= Date.now()) { setErro("A data de expiração deve ser futura."); return; }
        const criado = await criarCabecalho(solicitacao.id, solicitacao.cliente.id, expiracao.toISOString());
        id = criado.id;
        setOrcamentoId(id);
      }
      await salvarItens(id, payload);
      router.replace(`/orcamentos/${id}`);
    }
    catch (e) { setErro(getApiErrorMessage(e)); }
    finally { setSalvando(false); }
  };

  return <ScrollView className="flex-1 bg-[#fbfaf8]" contentContainerStyle={{ flexGrow: 1, paddingBottom: 40 }} keyboardShouldPersistTaps="handled">
    <View className="mx-auto w-full max-w-5xl gap-4 px-4 py-8 md:px-8">
      <CabecalhoTela contexto="Operacional" secao="Orçamentos" titulo={orcamentoId ? "Elaborar orçamento" : "Novo orçamento"} descricao="Selecione a solicitação e monte a proposta com os serviços e valores." rotaFallback="/orcamentos" compacto />
      {carregando ? <View className="items-center rounded-xl bg-white p-10"><ActivityIndicator color="#6f4f28" /></View> : null}
      {erro ? <View className="rounded-lg border border-red-200 bg-red-50 p-3"><Text className="text-sm text-red-700">{erro}</Text></View> : null}
      {!carregando && (!params.orcamentoId || orcamentoId) ? !etapaItens ? <View className="gap-4 rounded-2xl border border-neutral-200 bg-white p-5">
        <Text className="text-lg font-semibold text-neutral-900">1. Cabeçalho da proposta</Text>
        <Text className="text-sm text-neutral-600">Escolha a solicitação que originou este orçamento.</Text>
        {solicitacoes.length === 0 && !carregando ? <Text className="text-sm text-neutral-500">Não há solicitações pendentes de orçamento.</Text> : null}
        <SelectField
          label="Solicitação"
          value={solicitacaoId}
          onChange={(id) => {
            const selecionada = solicitacoes.find((s) => s.id === id);
            setSolicitacaoId(id);
            if (selecionada) setItens([novoItem(selecionada.idiomaOrigem.id, selecionada.idiomaDestino.id)]);
          }}
          options={opcoesSolicitacao}
          placeholder="Selecione a solicitação..."
          disabled={carregando || solicitacoes.length === 0}
        />
        <SeletorData value={dataExpiracao} onChange={setDataExpiracao} />
        <Pressable disabled={!solicitacao || salvando || carregando} onPress={iniciar} className="flex-row items-center justify-center gap-2 rounded-lg bg-[#6f4f28] px-5 py-3 disabled:opacity-50"><Text className="font-semibold text-white">Continuar para itens e valores</Text></Pressable>
      </View> : <>
        <View className="w-full items-center overflow-hidden rounded-xl border border-green-200 bg-green-50 px-4 py-3">
          <Text className="mb-2 text-xs font-medium text-green-700">{orcamentoId ? "Orçamento em elaboração" : "Solicitação selecionada"}</Text>
          <ScrollView horizontal className="w-full" contentContainerStyle={{ flexGrow: 1, justifyContent: "center" }} showsHorizontalScrollIndicator>
            <Text selectable numberOfLines={1} style={{ flexShrink: 0 }} className="text-sm font-semibold text-green-800">{orcamentoId ? `${codigoOrcamento(orcamentoId)} - ` : ""}{solicitacao?.cliente.nomeEmpresa ?? "Solicitação"}</Text>
          </ScrollView>
        </View>
        <View className="gap-4 rounded-2xl border border-neutral-200 bg-white p-5">
          <View className="flex-row items-center justify-between"><View><Text className="text-lg font-semibold text-neutral-900">2. Itens e serviços</Text><Text className="text-sm text-neutral-500">Adicione pares de idiomas e precifique cada serviço.</Text></View></View>
          {itens.map((item, idx) => <View key={idx} className="gap-4 rounded-xl border border-neutral-200 bg-neutral-50 p-4">
            <View className="flex-row items-center justify-between"><Text className="font-semibold text-neutral-800">Par de idiomas {idx + 1}</Text>{itens.length > 1 ? <Pressable accessibilityRole="button" accessibilityLabel={`Remover par de idiomas ${idx + 1}`} onPress={() => setItens((atual) => atual.filter((_, i) => i !== idx))} className="rounded-md border border-red-200 bg-white p-2 active:bg-red-50"><Ionicons name="trash-outline" size={18} color="#dc2626" /></Pressable> : null}</View>
            <View className="flex-row flex-wrap gap-3">
              <SelectField label="Idioma de origem" value={item.origem} onChange={(origem) => mudarItem(idx, { origem })} options={opcoesIdioma} placeholder="Selecione a origem..." invalid={Boolean(item.origem && item.origem === item.destino)} />
              <SelectField label="Idioma de destino" value={item.destino} onChange={(destino) => mudarItem(idx, { destino })} options={opcoesIdioma} placeholder="Selecione o destino..." invalid={Boolean(item.destino && item.origem === item.destino)} />
            </View>
            {item.origem && item.origem === item.destino ? <Text className="text-xs text-red-600">Origem e destino precisam ser diferentes.</Text> : null}
            {item.servicos.map((serv, si) => <View key={si} className="gap-2 rounded-lg border border-neutral-200 bg-white p-3"><View className="flex-row items-center justify-between"><Text className="text-sm font-medium text-neutral-700">Serviço {si + 1}</Text>{item.servicos.length > 1 ? <Pressable accessibilityRole="button" accessibilityLabel={`Remover serviço ${si + 1} do par ${idx + 1}`} onPress={() => mudarItem(idx, { servicos: item.servicos.filter((_, j) => j !== si) })} className="rounded-md border border-red-200 bg-white p-2 active:bg-red-50"><Ionicons name="trash-outline" size={18} color="#dc2626" /></Pressable> : null}</View>
              <SelectField label="Tipo de serviço" value={serv.tipo} onChange={(tipo) => mudarServico(idx, si, { tipo })} options={opcoesTipoServico} placeholder="Selecione o serviço..." />
              <View className="flex-row flex-wrap gap-2"><Campo label="Quantidade" value={serv.quantidade} onChangeText={(v) => mudarServico(idx, si, { quantidade: v })} keyboardType="decimal-pad" placeholder="Ex.: 1500" error={serv.quantidade && !positivo(serv.quantidade) ? "Deve ser maior que zero" : undefined} /><View className="min-w-[105px] flex-1 gap-1"><Text className="text-xs font-medium text-neutral-600">Unidade</Text><View className="flex-row flex-wrap gap-1">{UNIDADES.map((u) => <Pressable key={u} onPress={() => mudarServico(idx, si, { unidade: u })} className={`rounded-md border px-2 py-2 ${serv.unidade === u ? "border-[#6f4f28] bg-[#f4e9df]" : "border-neutral-300 bg-white"}`}><Text className="text-[10px] text-neutral-700">{u}</Text></Pressable>)}</View></View><Campo label="Valor unitário (R$)" value={serv.valor} onChangeText={(v) => mudarServico(idx, si, { valor: v })} keyboardType="decimal-pad" placeholder="0,00" error={serv.valor && !positivo(serv.valor) ? "Deve ser maior que zero" : undefined} /></View>
              <Text className="text-right text-xs text-neutral-500">Subtotal: {dinheiro(numerico(serv.quantidade) * numerico(serv.valor))}</Text>
            </View>)}
            <Pressable onPress={() => mudarItem(idx, { servicos: [...item.servicos, novoServico()] })} className="self-start rounded-lg border border-[#6f4f28] px-3 py-2"><Text className="text-xs font-medium text-[#6f4f28]">+ Adicionar serviço</Text></Pressable>
          </View>)}
          <Pressable onPress={() => setItens((atual) => [...atual, novoItem()])} className="self-start rounded-lg border border-neutral-300 bg-white px-4 py-2.5"><Text className="text-sm font-medium text-neutral-700">+ Adicionar par de idiomas</Text></Pressable>
          <View className="flex-row items-center justify-between rounded-xl bg-[#f4e9df] p-4"><Text className="font-semibold text-neutral-800">Total estimado</Text><Text className="text-lg font-bold text-[#6f4f28]">{dinheiro(total)}</Text></View>
          <Pressable disabled={salvando || !itensValidos} onPress={() => void submeter()} className="flex-row items-center justify-center gap-2 rounded-lg bg-[#6f4f28] px-5 py-3 disabled:opacity-50">{salvando ? <ActivityIndicator color="white" /> : null}<Text className="font-semibold text-white">Salvar orçamento</Text></Pressable>
        </View>
      </> : null}
    </View>
  </ScrollView>;
}
