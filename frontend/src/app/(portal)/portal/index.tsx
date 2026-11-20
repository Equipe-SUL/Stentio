import React, { useCallback, useEffect, useState } from "react";
import { ActivityIndicator, Pressable, ScrollView, Text, View } from "react-native";
import { isAxiosError } from "axios";
import * as DocumentPicker from "expo-document-picker";
import { Ionicons } from "@expo/vector-icons";
import { getApiErrorMessage } from "../../../lib/api";
import { aceitarEtapa, baixarEntrega, enviarEntrega, listarEtapasPortal, recusarEtapa, type DemandaRecurso, type EtapasPortalRecurso } from "../../../components/recursos/portalRecursoApi";

const LIMITE_ENTREGA_BYTES = 10 * 1024 * 1024;
const EXTENSOES_ENTREGA = new Set(["pdf", "doc", "docx", "odt", "rtf", "txt", "xls", "xlsx", "ppt", "pptx", "jpg", "jpeg", "png"]);

const STATUS: Record<string, { label: string; cor: string }> = {
  PENDENTE: { label: "Pendente", cor: "bg-amber-50 text-amber-800 border-amber-200" },
  AGUARDANDO_ETAPA_ANTERIOR: { label: "Aguardando etapa anterior", cor: "bg-neutral-100 text-neutral-700 border-neutral-200" },
  ALOCADO: { label: "Alocada", cor: "bg-emerald-50 text-emerald-800 border-emerald-200" },
  DISPARADA: { label: "Disponível", cor: "bg-blue-50 text-blue-800 border-blue-200" },
  ACEITA: { label: "Aceita", cor: "bg-emerald-50 text-emerald-800 border-emerald-200" },
  ENTREGUE: { label: "Entregue", cor: "bg-violet-50 text-violet-800 border-violet-200" },
};

function formatarPrazo(valor: string | null): string {
  if (!valor) return "Prazo não definido";
  const data = new Date(valor);
  if (Number.isNaN(data.getTime())) return "Prazo não definido";
  return new Intl.DateTimeFormat("pt-BR", { dateStyle: "medium", timeStyle: "short" }).format(data);
}

export default function MinhasDemandasScreen() {
  const [etapas, setEtapas] = useState<EtapasPortalRecurso>({ disponiveis: [], minhas: [] });
  const [carregando, setCarregando] = useState(true);
  const [erro, setErro] = useState("");
  const [mensagem, setMensagem] = useState("");
  const [etapaEmAcao, setEtapaEmAcao] = useState<string | null>(null);
  const [arquivo, setArquivo] = useState<DocumentPicker.DocumentPickerAsset | null>(null);
  const [etapaDoArquivo, setEtapaDoArquivo] = useState<string | null>(null);
  const [progresso, setProgresso] = useState<number | null>(null);

  const carregar = useCallback(async () => {
    setCarregando(true);
    setErro("");
    try {
      setEtapas(await listarEtapasPortal());
    } catch (error) {
      setErro(getApiErrorMessage(error));
    } finally {
      setCarregando(false);
    }
  }, []);

  useEffect(() => {
    let ativo = true;
    listarEtapasPortal()
      .then((lista) => { if (ativo) setEtapas(lista); })
      .catch((error) => { if (ativo) setErro(getApiErrorMessage(error)); })
      .finally(() => { if (ativo) setCarregando(false); });
    return () => { ativo = false; };
  }, []);

  const responderEtapa = async (etapa: DemandaRecurso, aceitar: boolean) => {
    try {
      setEtapaEmAcao(etapa.etapa_id);
      setErro("");
      if (aceitar) await aceitarEtapa(etapa.etapa_id);
      else await recusarEtapa(etapa.etapa_id);
      setMensagem(aceitar ? "Etapa aceita com sucesso." : "Alocação recusada. O gestor será notificado.");
      await carregar();
    } catch (error) {
      if (isAxiosError(error) && error.response?.status === 409) {
        await carregar();
        setMensagem("Outro recurso já assumiu esta etapa. A lista disponível foi atualizada.");
      } else {
        setErro(getApiErrorMessage(error));
      }
    } finally {
      setEtapaEmAcao(null);
    }
  };

  const selecionarEntrega = async (etapaId: string) => {
    try {
      const escolha = await DocumentPicker.getDocumentAsync({ type: "*/*", multiple: false, copyToCacheDirectory: true });
      if (escolha.canceled) return;
      const item = escolha.assets[0];
      const extensao = item.name.split(".").pop()?.toLowerCase() ?? "";
      if (!EXTENSOES_ENTREGA.has(extensao)) {
        setErro("Tipo de arquivo não permitido. Envie PDF, Office, texto ou imagem compatível.");
        return;
      }
      if (item.size !== undefined && item.size > LIMITE_ENTREGA_BYTES) {
        setErro("O arquivo ultrapassa o limite de 10 MB.");
        return;
      }
      setErro("");
      setArquivo(item);
      setEtapaDoArquivo(etapaId);
    } catch {
      setErro("Não foi possível abrir o seletor de arquivos. Tente novamente.");
    }
  };

  const enviarArquivo = async (etapaId: string) => {
    if (!arquivo || etapaDoArquivo !== etapaId) return;
    try {
      setEtapaEmAcao(etapaId);
      setProgresso(0);
      setErro("");
      await enviarEntrega(etapaId, { uri: arquivo.uri, nome: arquivo.name, tipo: arquivo.mimeType ?? "application/octet-stream", file: arquivo.file }, setProgresso);
      setMensagem("Entrega enviada com sucesso.");
      setArquivo(null);
      setEtapaDoArquivo(null);
      await carregar();
    } catch (error) {
      setErro(getApiErrorMessage(error));
    } finally {
      setEtapaEmAcao(null);
      setProgresso(null);
    }
  };

  const renderDemandas = (lista: DemandaRecurso[], disponiveis = false) => lista.map((demanda) => {
    const status = STATUS[demanda.situacao] ?? { label: demanda.situacao, cor: "bg-neutral-100 text-neutral-700 border-neutral-200" };
    return (
      <View key={demanda.etapa_id} className="gap-4 rounded-2xl border border-neutral-200 bg-white p-5 shadow-sm">
        <View className="flex-row flex-wrap items-start justify-between gap-3">
          <View className="flex-1 gap-1">
            <Text className="text-xs font-semibold uppercase tracking-wide text-neutral-500">OS {demanda.ordem_servico_codigo || demanda.ordem_servico_id}</Text>
            <Text className="text-base font-bold text-neutral-900">{demanda.nome || `Etapa ${demanda.ordem}`}</Text>
          </View>
          <View className={`rounded-full border px-3 py-1.5 ${status.cor}`}><Text className="text-xs font-semibold">{status.label}</Text></View>
        </View>
        <View className="flex-row flex-wrap gap-x-8 gap-y-3 border-t border-neutral-100 pt-4">
          <View className="min-w-[130px] flex-1 gap-1"><Text className="text-xs text-neutral-500">Tipo de serviço</Text><Text className="text-sm font-medium text-neutral-800">{demanda.tipo_servico}</Text></View>
          <View className="min-w-[170px] flex-1 gap-1"><Text className="text-xs text-neutral-500">Par de idiomas</Text><Text className="text-sm font-medium text-neutral-800">{demanda.idioma_origem} → {demanda.idioma_destino}</Text></View>
          <View className="min-w-[160px] flex-1 gap-1"><Text className="text-xs text-neutral-500">Prazo</Text><Text className="text-sm font-medium text-neutral-800">{formatarPrazo(demanda.data_limite_entrega)}</Text></View>
          <View className="min-w-[130px] flex-1 gap-1"><Text className="text-xs text-neutral-500">Estimativa</Text><Text className="text-sm font-medium text-neutral-800">{demanda.prazo_estimado_horas} h</Text></View>
        </View>
        {disponiveis || demanda.situacao === "ALOCADO" ? (
          <View className="flex-row flex-wrap gap-2">
            <Pressable disabled={etapaEmAcao === demanda.etapa_id} onPress={() => void responderEtapa(demanda, true)} className="flex-row items-center gap-2 rounded-xl bg-[#6f4f28] px-4 py-2.5">
              {etapaEmAcao === demanda.etapa_id ? <ActivityIndicator size="small" color="white" /> : <Ionicons name="checkmark-circle-outline" size={17} color="white" />}
              <Text className="text-sm font-semibold text-white">{disponiveis ? "Aceitar etapa" : "Confirmar alocação"}</Text>
            </Pressable>
            {!disponiveis ? (
              <Pressable disabled={etapaEmAcao === demanda.etapa_id} onPress={() => void responderEtapa(demanda, false)} className="rounded-xl border border-neutral-300 bg-white px-4 py-2.5">
                <Text className="text-sm font-semibold text-neutral-700">Recusar</Text>
              </Pressable>
            ) : null}
          </View>
        ) : null}
        {demanda.situacao === "ACEITA" ? (
          <View className="gap-2">
            {etapaDoArquivo === demanda.etapa_id && arquivo ? <Text className="text-xs text-neutral-600">Arquivo: {arquivo.name}</Text> : null}
            <View className="flex-row flex-wrap gap-2">
              <Pressable disabled={etapaEmAcao === demanda.etapa_id} onPress={() => void selecionarEntrega(demanda.etapa_id)} className="rounded-xl border border-neutral-300 bg-white px-4 py-2.5">
                <Text className="text-sm font-semibold text-neutral-700">{arquivo && etapaDoArquivo === demanda.etapa_id ? "Trocar arquivo" : "Selecionar arquivo"}</Text>
              </Pressable>
              {arquivo && etapaDoArquivo === demanda.etapa_id ? (
                <Pressable disabled={etapaEmAcao === demanda.etapa_id} onPress={() => void enviarArquivo(demanda.etapa_id)} className="rounded-xl bg-[#6f4f28] px-4 py-2.5">
                  <Text className="text-sm font-semibold text-white">{progresso === null ? "Enviar entrega" : `Enviando ${progresso}%`}</Text>
                </Pressable>
              ) : null}
            </View>
          </View>
        ) : null}
        {demanda.situacao === "ENTREGUE" && demanda.arquivo_entrega_url ? (
          <Pressable onPress={() => void baixarEntrega(demanda.arquivo_entrega_url!, demanda.arquivo_entrega_nome || "entrega")} className="self-start flex-row items-center gap-2 rounded-lg border border-violet-200 bg-violet-50 px-3 py-2">
            <Ionicons name="download-outline" size={17} color="#6d28d9" />
            <Text className="text-sm font-medium text-violet-800">{demanda.arquivo_entrega_nome || "Baixar arquivo entregue"}</Text>
          </Pressable>
        ) : null}
      </View>
    );
  });

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
              <Text className="text-xs font-semibold text-[#744127]">{etapas.minhas.length} {etapas.minhas.length === 1 ? "etapa" : "etapas"}</Text>
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
        ) : null}
        {mensagem ? <Text className="text-sm font-medium text-emerald-700">{mensagem}</Text> : null}
        {!carregando && !erro ? (
          <>
            <View className="gap-3">
              <View className="flex-row items-center justify-between"><Text className="text-lg font-bold text-neutral-900">Disponíveis</Text><Text className="text-xs text-neutral-500">{etapas.disponiveis.length}</Text></View>
              {etapas.disponiveis.length ? renderDemandas(etapas.disponiveis, true) : <Text className="rounded-xl bg-white p-4 text-sm text-neutral-500">Nenhuma etapa disponível no momento.</Text>}
            </View>
            <View className="gap-3">
              <View className="flex-row items-center justify-between"><Text className="text-lg font-bold text-neutral-900">Minhas</Text><Text className="text-xs text-neutral-500">{etapas.minhas.length}</Text></View>
              {etapas.minhas.length ? renderDemandas(etapas.minhas) : <Text className="rounded-xl bg-white p-4 text-sm text-neutral-500">Você ainda não tem etapas assumidas.</Text>}
            </View>
          </>
        ) : null}
      </View>
    </ScrollView>
  );
}
