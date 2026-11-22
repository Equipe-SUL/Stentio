import {
  useCallback,
  useEffect,
  useEffectEvent,
  useMemo,
  useRef,
  useState,
} from "react";
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
import { useRouter } from "expo-router";
import { Ionicons } from "@expo/vector-icons";
import { isAxiosError } from "axios";
import { CabecalhoTela } from "../navegacao/CabecalhoTela";
import { coreApi, getApiErrorMessage } from "../../lib/api";
import {
  aprovarEntregaFinal,
  baixarArquivoOrdemServico,
  buscarOrdemServicoPorId,
  buscarRevisaoEntregaFinal,
  reprovarEntregaFinal,
} from "./ordensApi";
import { OrdemServicoStatusBadge } from "./OrdemServicoStatusBadge";
import type {
  AprovarEntregaFinalResponse,
  EntregaFinalEtapa,
  EntregaFinalRevisaoResponse,
  ReprovarEntregaFinalResponse,
  StatusOrdemServico,
} from "./types";

interface EntregaFinalRevisaoScreenProps {
  ordemId: string;
}

const PERFIS_PERMITIDOS = new Set(["ADMIN", "GESTOR_PROJETO"]);

function obterRoleUsuarioAtual(): string | null {
  try {
    let token: string | null = null;

    const authHeader = coreApi.defaults.headers?.common?.Authorization;
    if (typeof authHeader === "string" && authHeader.startsWith("Bearer ")) {
      token = authHeader.slice(7).trim();
    }

    if (!token && Platform.OS === "web" && typeof document !== "undefined") {
      const match = document.cookie
        .split("; ")
        .find((row) => row.startsWith("stentio_token="));
      if (match) {
        token = decodeURIComponent(match.split("=")[1] ?? "");
      }
    }

    if (!token) return null;
    const partes = token.split(".");
    if (partes.length < 2) return null;

    const base64 = partes[1].replace(/-/g, "+").replace(/_/g, "/");
    const jsonPayload =
      typeof atob === "function"
        ? atob(base64)
        : Buffer.from(base64, "base64").toString("utf-8");

    const payload = JSON.parse(jsonPayload) as { role?: string; roles?: string[] };
    if (typeof payload.role === "string") return payload.role.toUpperCase();
    if (Array.isArray(payload.roles) && payload.roles.length > 0) {
      return String(payload.roles[0]).toUpperCase();
    }
    return null;
  } catch {
    return null;
  }
}

function formatarDataHora(isoString?: string | null): string {
  if (!isoString) return "—";
  try {
    const data = new Date(isoString);
    if (Number.isNaN(data.getTime())) return isoString;
    return new Intl.DateTimeFormat("pt-BR", {
      dateStyle: "medium",
      timeStyle: "short",
    }).format(data);
  } catch {
    return isoString;
  }
}

function formatarTamanhoBytes(bytes?: number | null): string {
  if (bytes == null || Number.isNaN(bytes) || bytes < 0) return "Tamanho desconhecido";
  if (bytes === 0) return "0 B";
  const unidades = ["B", "KB", "MB", "GB"];
  const indice = Math.min(
    Math.floor(Math.log(bytes) / Math.log(1024)),
    unidades.length - 1,
  );
  const valor = bytes / Math.pow(1024, indice);
  return `${valor.toFixed(indice === 0 ? 0 : 2).replace(".", ",")} ${unidades[indice]}`;
}

function mapearErroAcaoEntregaFinal(err: unknown, contexto: "aprovar" | "reprovar"): string {
  if (isAxiosError(err)) {
    const status = err.response?.status;
    const detalheApi = getApiErrorMessage(err);

    if (status === 409) {
      return contexto === "aprovar"
        ? "Conflito: já existe um envio em processamento na fila (outbox pendente) ou esta OS já foi encerrada/alterada."
        : "Não é possível reprovar: já existe um envio pendente na fila ou esta OS já foi encerrada.";
    }
    if (status === 413) {
      return "O arquivo da última etapa excede o tamanho máximo permitido para envio por e-mail (HTTP 413). Reduza o tamanho do arquivo ou solicite correção.";
    }
    if (status === 422) {
      return "O arquivo da última etapa não foi encontrado no servidor ou está ilegível (HTTP 422). Solicite uma nova entrega ao recurso.";
    }
    if (status === 403) {
      return "Você não possui permissão para executar esta ação.";
    }
    if (detalheApi) return detalheApi;
  }
  return getApiErrorMessage(err);
}

export function EntregaFinalRevisaoScreen({ ordemId }: EntregaFinalRevisaoScreenProps) {
  const router = useRouter();
  const roleUsuario = useMemo(() => obterRoleUsuarioAtual(), []);
  const acessoPermitido = !roleUsuario || PERFIS_PERMITIDOS.has(roleUsuario);

  const [revisao, setRevisao] = useState<EntregaFinalRevisaoResponse | null>(null);
  const [carregando, setCarregando] = useState(true);
  const [erro, setErro] = useState<string | null>(null);
  const [statusInvalidoConflito, setStatusInvalidoConflito] = useState(false);

  // Download por etapa
  const [baixandoEtapaId, setBaixandoEtapaId] = useState<string | null>(null);
  const [erroDownload, setErroDownload] = useState<string | null>(null);

  // Fluxo de Aprovação (Task 2)
  const [modalAprovarAberto, setModalAprovarAberto] = useState(false);
  const [mensagemCliente, setMensagemCliente] = useState("");
  const [aprovando, setAprovando] = useState(false);
  const [erroAprovar, setErroAprovar] = useState<string | null>(null);
  const [resultadoAprovacao, setResultadoAprovacao] =
    useState<AprovarEntregaFinalResponse | null>(null);
  const [statusMonitoradoOs, setStatusMonitoradoOs] =
    useState<StatusOrdemServico | null>(null);

  // Fluxo de Reprovação (Task 2)
  const [modalReprovarAberto, setModalReprovarAberto] = useState(false);
  const [motivoReprovacao, setMotivoReprovacao] = useState("");
  const [reprovando, setReprovando] = useState(false);
  const [erroReprovar, setErroReprovar] = useState<string | null>(null);
  const [resultadoReprovacao, setResultadoReprovacao] =
    useState<ReprovarEntregaFinalResponse | null>(null);

  // Trava síncrona adicional contra duplo clique
  const emRequisicaoRef = useRef(false);

  const carregarRevisao = useCallback(async () => {
    if (!ordemId) {
      setErro("Identificador da Ordem de Serviço não informado.");
      setCarregando(false);
      return;
    }

    setCarregando(true);
    setErro(null);
    setStatusInvalidoConflito(false);

    try {
      const dados = await buscarRevisaoEntregaFinal(ordemId);
      setRevisao(dados);
      setStatusMonitoradoOs(dados.status);
    } catch (err: unknown) {
      if (isAxiosError(err)) {
        const status = err.response?.status;
        if (status === 409) {
          setStatusInvalidoConflito(true);
          setErro(
            getApiErrorMessage(err) ||
              "Esta Ordem de Serviço não está em Aguardando Verificação e não pode ser revisada agora.",
          );
        } else if (status === 403) {
          setErro("Acesso restrito a Gestores de Projeto e Administradores.");
        } else if (status === 404) {
          setErro("Ordem de Serviço não encontrada.");
        } else {
          setErro(getApiErrorMessage(err));
        }
      } else {
        setErro(getApiErrorMessage(err));
      }
    } finally {
      setCarregando(false);
    }
  }, [ordemId]);

  const carregarRevisaoQuandoPermitido = useEffectEvent(() => {
    if (acessoPermitido) {
      void carregarRevisao();
    } else {
      setCarregando(false);
    }
  });

  useEffect(() => {
    queueMicrotask(carregarRevisaoQuandoPermitido);
  }, [acessoPermitido, ordemId]);

  // Polling leve após 202 Accepted até o broker confirmar ACK e a OS virar ENCERRADA
  useEffect(() => {
    if (!resultadoAprovacao || !ordemId) return;
    if (statusMonitoradoOs === "ENCERRADA") return;

    let ativo = true;
    const intervalo = setInterval(async () => {
      try {
        const osAtual = await buscarOrdemServicoPorId(ordemId);
        if (!ativo) return;
        if (osAtual?.status) {
          setStatusMonitoradoOs(osAtual.status);
          if (osAtual.status === "ENCERRADA") {
            clearInterval(intervalo);
          }
        }
      } catch {
        // Ignora falha transitória durante polling
      }
    }, 2500);

    return () => {
      ativo = false;
      clearInterval(intervalo);
    };
  }, [resultadoAprovacao, statusMonitoradoOs, ordemId]);

  const etapasRevisao = revisao?.etapas;
  const etapasOrdenadas = useMemo(() => {
    if (!etapasRevisao) return [];
    return [...etapasRevisao].sort((a, b) => a.ordem - b.ordem);
  }, [etapasRevisao]);

  async function handleBaixarArquivoEtapa(etapa: EntregaFinalEtapa) {
    if (!etapa.arquivo_url) return;
    setErroDownload(null);
    setBaixandoEtapaId(etapa.etapa_id);
    try {
      const nome = etapa.arquivo_nome || `etapa-${etapa.ordem}-entrega`;
      await baixarArquivoOrdemServico(etapa.arquivo_url, nome);
    } catch (err) {
      setErroDownload(getApiErrorMessage(err));
    } finally {
      setBaixandoEtapaId(null);
    }
  }

  function abrirModalAprovar() {
    if (!revisao?.pacote?.pode_enviar || aprovando || reprovando || emRequisicaoRef.current) {
      return;
    }
    setErroAprovar(null);
    setModalAprovarAberto(true);
  }

  async function handleConfirmarAprovacao() {
    if (!ordemId || emRequisicaoRef.current || aprovando) return;
    emRequisicaoRef.current = true;
    setAprovando(true);
    setErroAprovar(null);

    try {
      const resposta = await aprovarEntregaFinal(ordemId, {
        mensagem: mensagemCliente,
      });
      setResultadoAprovacao(resposta);
      setStatusMonitoradoOs(resposta.status || "AGUARDANDO_VERIFICACAO");
      setModalAprovarAberto(false);
    } catch (err) {
      setErroAprovar(mapearErroAcaoEntregaFinal(err, "aprovar"));
    } finally {
      setAprovando(false);
      emRequisicaoRef.current = false;
    }
  }

  function abrirModalReprovar() {
    if (aprovando || reprovando || emRequisicaoRef.current) return;
    setErroReprovar(null);
    setModalReprovarAberto(true);
  }

  async function handleConfirmarReprovacao() {
    if (!ordemId || emRequisicaoRef.current || reprovando) return;

    const motivoLimpo = motivoReprovacao.trim();
    if (!motivoLimpo) {
      setErroReprovar("Informe o motivo da reprovação para orientar o profissional na correção.");
      return;
    }

    emRequisicaoRef.current = true;
    setReprovando(true);
    setErroReprovar(null);

    try {
      const resposta = await reprovarEntregaFinal(ordemId, {
        motivo: motivoLimpo,
      });
      setResultadoReprovacao(resposta);
      setStatusMonitoradoOs(resposta.ordem_servico_status || "EM_EXECUCAO");
      setModalReprovarAberto(false);
    } catch (err) {
      setErroReprovar(mapearErroAcaoEntregaFinal(err, "reprovar"));
    } finally {
      setReprovando(false);
      emRequisicaoRef.current = false;
    }
  }

  const operacaoFinalizada = Boolean(resultadoAprovacao || resultadoReprovacao);
  const bloqueadoParaAcoes =
    operacaoFinalizada || aprovando || reprovando;

  return (
    <ScrollView
      className="flex-1 bg-[#fbfaf8]"
      contentContainerStyle={{ flexGrow: 1, paddingBottom: 48 }}
      keyboardShouldPersistTaps="handled"
    >
      <View className="w-full max-w-5xl mx-auto px-4 py-8 md:px-8 gap-6">
        <CabecalhoTela
          contexto="Operacional"
          secao="Ordens de Serviço"
          titulo="Revisão Final & Envio ao Cliente"
          descricao="Confira os arquivos entregues em cada etapa concluída e valide o pacote final antes de enviá-lo ao cliente."
          rotaFallback={ordemId ? `/ordens/${ordemId}` : "/ordens"}
        />

        {!acessoPermitido ? (
          <View className="items-center justify-center rounded-2xl border border-red-200 bg-red-50/70 p-8 gap-3">
            <View className="h-12 w-12 items-center justify-center rounded-full bg-red-100">
              <Ionicons name="lock-closed-outline" size={24} color="#b91c1c" />
            </View>
            <Text className="text-lg font-bold text-red-950">Acesso não permitido</Text>
            <Text className="max-w-md text-center text-sm text-red-800">
              Apenas usuários com perfil Gestor de Projeto ou Administrador podem revisar e
              aprovar a entrega final de uma Ordem de Serviço.
            </Text>
            <Pressable
              onPress={() => router.push("/ordens")}
              className="mt-2 rounded-xl bg-[#6f4f28] px-5 py-2.5 active:bg-[#5b4020]"
            >
              <Text className="text-xs font-bold text-white">Voltar para Ordens</Text>
            </Pressable>
          </View>
        ) : carregando ? (
          <View className="items-center justify-center gap-3 rounded-2xl border border-neutral-200 bg-white p-12">
            <ActivityIndicator size="large" color="#6f4f28" />
            <Text className="text-sm font-medium text-neutral-600">
              Carregando dados da revisão final...
            </Text>
          </View>
        ) : erro || !revisao ? (
          <View className="items-center justify-center rounded-2xl border border-amber-200 bg-amber-50/70 p-8 gap-3">
            <View className="h-12 w-12 items-center justify-center rounded-full bg-amber-100">
              <Ionicons
                name={statusInvalidoConflito ? "git-compare-outline" : "warning-outline"}
                size={26}
                color="#b45309"
              />
            </View>
            <Text className="text-lg font-bold text-neutral-900">
              {statusInvalidoConflito
                ? "Revisão final indisponível para o status atual"
                : "Não foi possível carregar a revisão final"}
            </Text>
            <Text className="max-w-lg text-center text-sm text-neutral-700">{erro}</Text>
            <View className="mt-3 flex-row flex-wrap justify-center gap-3">
              {ordemId ? (
                <Pressable
                  onPress={() => router.push(`/ordens/${ordemId}` as never)}
                  className="rounded-xl bg-[#6f4f28] px-5 py-2.5 active:bg-[#5b4020]"
                >
                  <Text className="text-xs font-bold text-white">
                    Ver detalhes da Ordem de Serviço
                  </Text>
                </Pressable>
              ) : null}
              <Pressable
                onPress={() => void carregarRevisao()}
                className="rounded-xl border border-neutral-300 bg-white px-4 py-2.5 active:bg-neutral-50"
              >
                <Text className="text-xs font-semibold text-neutral-700">
                  Tentar novamente
                </Text>
              </Pressable>
            </View>
          </View>
        ) : (
          <View className="gap-6">
            {/* Banner de status após Aprovação (202 Accepted -> Polling até ENCERRADA) */}
            {resultadoAprovacao ? (
              <View
                className={`rounded-2xl border p-5 shadow-sm gap-3 ${
                  statusMonitoradoOs === "ENCERRADA"
                    ? "border-emerald-300 bg-emerald-50"
                    : "border-sky-300 bg-sky-50"
                }`}
              >
                <View className="flex-row items-start gap-3">
                  <View
                    className={`h-10 w-10 items-center justify-center rounded-xl border ${
                      statusMonitoradoOs === "ENCERRADA"
                        ? "bg-emerald-100 border-emerald-200"
                        : "bg-sky-100 border-sky-200"
                    }`}
                  >
                    {statusMonitoradoOs === "ENCERRADA" ? (
                      <Ionicons name="checkmark-done-circle" size={22} color="#047857" />
                    ) : (
                      <ActivityIndicator size="small" color="#0369a1" />
                    )}
                  </View>

                  <View className="flex-1 gap-1">
                    <Text
                      className={`text-base font-bold ${
                        statusMonitoradoOs === "ENCERRADA"
                          ? "text-emerald-950"
                          : "text-sky-950"
                      }`}
                    >
                      {statusMonitoradoOs === "ENCERRADA"
                        ? "Publicado para envio e Ordem de Serviço encerrada"
                        : "Envio em processamento — publicado na fila de disparo"}
                    </Text>
                    <Text
                      className={`text-xs leading-5 ${
                        statusMonitoradoOs === "ENCERRADA"
                          ? "text-emerald-900"
                          : "text-sky-900"
                      }`}
                    >
                      {statusMonitoradoOs === "ENCERRADA"
                        ? `O e-mail com o arquivo "${revisao.pacote?.arquivo_nome || "final"}" foi publicado para envio a ${revisao.cliente?.email || "o representante do cliente"} e a OS passou para ENCERRADA.`
                        : "A solicitação foi registrada na fila de envio (outbox) e está aguardando confirmação de publicação para encerrar a Ordem de Serviço."}
                    </Text>
                    <Text className="text-[11px] font-mono text-neutral-600 pt-1">
                      Outbox ID: {resultadoAprovacao.outbox_id} · Chave:{" "}
                      {resultadoAprovacao.idempotency_key}
                    </Text>
                  </View>
                </View>

                <View className="flex-row flex-wrap items-center justify-end gap-2.5 pt-1">
                  <Pressable
                    onPress={() => router.push(`/ordens/${revisao.ordem_servico_id}` as never)}
                    className="rounded-xl bg-[#6f4f28] px-4 py-2 active:bg-[#5b4020]"
                  >
                    <Text className="text-xs font-bold text-white">
                      Voltar para detalhes da OS
                    </Text>
                  </Pressable>
                </View>
              </View>
            ) : null}

            {/* Banner de status após Reprovação (200 OK -> ACEITA / EM_EXECUCAO) */}
            {resultadoReprovacao ? (
              <View className="rounded-2xl border border-amber-300 bg-amber-50 p-5 shadow-sm gap-3">
                <View className="flex-row items-start gap-3">
                  <View className="h-10 w-10 items-center justify-center rounded-xl bg-amber-100 border border-amber-200">
                    <Ionicons name="return-up-back" size={22} color="#b45309" />
                  </View>
                  <View className="flex-1 gap-1">
                    <Text className="text-base font-bold text-amber-950">
                      Entrega reprovada e etapa reaberta para correção
                    </Text>
                    <Text className="text-xs leading-5 text-amber-900">
                      A última etapa retornou para o status{" "}
                      <Text className="font-bold">{resultadoReprovacao.etapa_status}</Text> com
                      o mesmo profissional e prazo alocados, e a Ordem de Serviço voltou para{" "}
                      <Text className="font-bold">
                        {resultadoReprovacao.ordem_servico_status}
                      </Text>
                      . Nenhum e-mail foi enviado ao cliente.
                    </Text>
                  </View>
                </View>

                <View className="flex-row flex-wrap items-center justify-end gap-2.5 pt-1">
                  <Pressable
                    onPress={() =>
                      router.push(`/ordens/${revisao.ordem_servico_id}/workflow` as never)
                    }
                    className="rounded-xl border border-[#8c5230]/40 bg-white px-4 py-2 active:bg-[#ede2d6]"
                  >
                    <Text className="text-xs font-bold text-[#8c5230]">
                      Ver workflow da OS
                    </Text>
                  </Pressable>
                  <Pressable
                    onPress={() => router.push(`/ordens/${revisao.ordem_servico_id}` as never)}
                    className="rounded-xl bg-[#6f4f28] px-4 py-2 active:bg-[#5b4020]"
                  >
                    <Text className="text-xs font-bold text-white">
                      Voltar para detalhes da OS
                    </Text>
                  </Pressable>
                </View>
              </View>
            ) : null}

            {/* Cabeçalho de Identificação da OS */}
            <View className="rounded-2xl border border-neutral-200 bg-white p-5 md:p-6 shadow-sm">
              <View className="flex-col md:flex-row md:items-center justify-between gap-4">
                <View className="gap-1.5">
                  <Text className="text-xs font-semibold uppercase tracking-wider text-neutral-400">
                    Ordem de Serviço em Revisão
                  </Text>
                  <View className="flex-row items-center gap-3 flex-wrap">
                    <Text className="text-2xl md:text-3xl font-serif font-bold text-neutral-900">
                      {revisao.codigo}
                    </Text>
                    <OrdemServicoStatusBadge status={statusMonitoradoOs || revisao.status} />
                  </View>
                </View>

                <View className="flex-row items-center gap-2.5 flex-wrap">
                  <Pressable
                    onPress={() => router.push(`/ordens/${revisao.ordem_servico_id}` as never)}
                    className="flex-row items-center gap-1.5 rounded-xl border border-neutral-300 bg-white px-3.5 py-2 active:bg-neutral-50"
                  >
                    <Ionicons name="document-text-outline" size={16} color="#525252" />
                    <Text className="text-xs font-semibold text-neutral-700">
                      Detalhes da OS
                    </Text>
                  </Pressable>
                  <Pressable
                    onPress={() =>
                      router.push(`/ordens/${revisao.ordem_servico_id}/workflow` as never)
                    }
                    className="flex-row items-center gap-1.5 rounded-xl border border-[#8c5230]/40 bg-[#fbf8f5] px-3.5 py-2 active:bg-[#ede2d6]"
                  >
                    <Ionicons name="git-network-outline" size={16} color="#8c5230" />
                    <Text className="text-xs font-bold text-[#8c5230]">Workflow</Text>
                  </Pressable>
                </View>
              </View>
            </View>

            {/* Motivo de reprovação anterior (quando a OS já foi reprovada antes) */}
            {revisao.motivo_revisao ? (
              <View className="flex-row items-start gap-3 rounded-2xl border border-amber-300 bg-amber-50 p-4 shadow-sm">
                <View className="h-9 w-9 items-center justify-center rounded-xl bg-amber-100 border border-amber-200">
                  <Ionicons name="alert-circle" size={20} color="#b45309" />
                </View>
                <View className="flex-1 gap-1">
                  <Text className="text-sm font-bold text-amber-950">
                    Histórico de reprovação anterior
                  </Text>
                  <Text className="text-xs leading-5 text-amber-900">
                    Esta OS havia sido devolvida para correção pelo seguinte motivo:
                  </Text>
                  <View className="mt-1 rounded-xl border border-amber-200 bg-white/80 px-3.5 py-2.5">
                    <Text className="text-xs font-medium text-neutral-800">
                      “{revisao.motivo_revisao}”
                    </Text>
                  </View>
                </View>
              </View>
            ) : null}

            {/* Lista de Etapas Entregues em Ordem (CA.3 / CA.4) */}
            <View className="rounded-2xl border border-neutral-200 bg-white p-5 md:p-6 shadow-sm gap-4">
              <View className="flex-row flex-wrap items-center justify-between gap-2 border-b border-neutral-100 pb-3">
                <View className="flex-row items-center gap-2.5">
                  <View className="h-9 w-9 items-center justify-center rounded-xl bg-emerald-50 border border-emerald-200">
                    <Ionicons name="layers-outline" size={20} color="#047857" />
                  </View>
                  <View>
                    <Text className="text-base font-bold text-neutral-900">
                      Arquivos Entregues por Etapa
                    </Text>
                    <Text className="text-xs text-neutral-500">
                      Conferência interna das etapas concluídas em ordem de execução.
                    </Text>
                  </View>
                </View>

                <View className="rounded-full bg-neutral-100 px-3 py-1 border border-neutral-200">
                  <Text className="text-xs font-semibold text-neutral-700">
                    {etapasOrdenadas.length}{" "}
                    {etapasOrdenadas.length === 1 ? "etapa entregue" : "etapas entregues"}
                  </Text>
                </View>
              </View>

              {erroDownload ? (
                <View className="flex-row items-center justify-between gap-2 rounded-xl border border-red-200 bg-red-50 px-3.5 py-2.5">
                  <Text className="flex-1 text-xs text-red-800">{erroDownload}</Text>
                  <Pressable onPress={() => setErroDownload(null)} className="p-1">
                    <Ionicons name="close" size={16} color="#991b1b" />
                  </Pressable>
                </View>
              ) : null}

              {etapasOrdenadas.length === 0 ? (
                <View className="items-center justify-center py-8 gap-2">
                  <Ionicons name="folder-open-outline" size={30} color="#a3a3a3" />
                  <Text className="text-sm text-neutral-500">
                    Nenhuma etapa entregue encontrada para esta Ordem de Serviço.
                  </Text>
                </View>
              ) : (
                <View className="gap-3">
                  {etapasOrdenadas.map((etapa, index) => {
                    const isUltimaEtapa =
                      etapa.etapa_id === revisao.pacote?.etapa_id ||
                      index === etapasOrdenadas.length - 1;
                    const baixando = baixandoEtapaId === etapa.etapa_id;

                    return (
                      <View
                        key={etapa.etapa_id}
                        className={`flex-col sm:flex-row sm:items-center justify-between gap-4 rounded-xl border p-4 ${
                          isUltimaEtapa
                            ? "border-[#8c5230]/40 bg-[#fdfaf8]"
                            : "border-neutral-200 bg-neutral-50/60"
                        }`}
                      >
                        <View className="flex-row items-start gap-3 flex-1 min-w-0">
                          <View
                            className={`h-8 w-8 items-center justify-center rounded-lg border ${
                              isUltimaEtapa
                                ? "bg-[#f5ede7] border-[#e2cbb8]"
                                : "bg-white border-neutral-200"
                            }`}
                          >
                            <Text
                              className={`text-xs font-bold ${
                                isUltimaEtapa ? "text-[#8c5230]" : "text-neutral-700"
                              }`}
                            >
                              {etapa.ordem}
                            </Text>
                          </View>

                          <View className="flex-1 min-w-0 gap-1">
                            <View className="flex-row items-center gap-2 flex-wrap">
                              <Text className="text-sm font-bold text-neutral-900">
                                {etapa.nome}
                              </Text>
                              {isUltimaEtapa ? (
                                <View className="rounded-full bg-[#8c5230] px-2.5 py-0.5">
                                  <Text className="text-[10px] font-bold uppercase tracking-wide text-white">
                                    Etapa Final · Pacote do Cliente
                                  </Text>
                                </View>
                              ) : null}
                            </View>

                            <View className="flex-row items-center gap-1.5 flex-wrap">
                              <Ionicons
                                name="document-attach-outline"
                                size={14}
                                color="#737373"
                              />
                              <Text
                                className="text-xs font-medium text-neutral-800"
                                numberOfLines={1}
                              >
                                {etapa.arquivo_nome || "Arquivo sem nome"}
                              </Text>
                            </View>

                            <Text className="text-[11px] text-neutral-500">
                              Entregue em {formatarDataHora(etapa.entregue_em)}
                            </Text>
                          </View>
                        </View>

                        {Platform.OS === "web" && etapa.arquivo_url ? (
                          <Pressable
                            onPress={() => void handleBaixarArquivoEtapa(etapa)}
                            disabled={baixando}
                            className="flex-row items-center justify-center gap-1.5 self-start sm:self-auto rounded-xl border border-neutral-300 bg-white px-3.5 py-2 active:bg-neutral-100"
                            accessibilityRole="button"
                            accessibilityLabel={`Baixar arquivo da etapa ${etapa.nome}`}
                          >
                            {baixando ? (
                              <ActivityIndicator size="small" color="#6f4f28" />
                            ) : (
                              <Ionicons
                                name="download-outline"
                                size={16}
                                color="#6f4f28"
                              />
                            )}
                            <Text className="text-xs font-semibold text-neutral-800">
                              Baixar arquivo
                            </Text>
                          </Pressable>
                        ) : null}
                      </View>
                    );
                  })}
                </View>
              )}
            </View>

            {/* Bloco "Pacote para o cliente" (RN.3 / RN.4) */}
            <View className="rounded-2xl border border-neutral-200 bg-white p-5 md:p-6 shadow-sm gap-5">
              <View className="flex-row items-center gap-2.5 border-b border-neutral-100 pb-3">
                <View className="h-9 w-9 items-center justify-center rounded-xl bg-purple-50 border border-purple-200">
                  <Ionicons name="mail-outline" size={20} color="#6b21a8" />
                </View>
                <View className="flex-1">
                  <Text className="text-base font-bold text-neutral-900">
                    Pacote para o cliente
                  </Text>
                  <Text className="text-xs text-neutral-500">
                    Apenas o arquivo da última etapa é anexado ao e-mail enviado ao representante.
                  </Text>
                </View>
              </View>

              <View className="grid grid-cols-1 md:grid-cols-2 gap-4">
                <View className="rounded-xl border border-neutral-200 bg-neutral-50/70 p-4 gap-1.5">
                  <Text className="text-xs font-semibold uppercase tracking-wide text-neutral-400">
                    Destinatário (Cliente)
                  </Text>
                  <Text className="text-sm font-bold text-neutral-900">
                    {revisao.cliente?.nome || "Cliente não informado"}
                  </Text>
                  <View className="flex-row items-center gap-1.5 pt-0.5">
                    <Ionicons name="at-outline" size={14} color="#6b21a8" />
                    <Text className="text-xs font-medium text-neutral-700">
                      {revisao.cliente?.email || "E-mail do representante não cadastrado"}
                    </Text>
                  </View>
                </View>

                <View className="rounded-xl border border-[#8c5230]/30 bg-[#fbf8f5] p-4 gap-1.5">
                  <Text className="text-xs font-semibold uppercase tracking-wide text-[#8c5230]">
                    Arquivo Anexo do Pacote Final
                  </Text>
                  <Text
                    className="text-sm font-bold text-neutral-900"
                    numberOfLines={1}
                  >
                    {revisao.pacote?.arquivo_nome || "Nenhum arquivo na última etapa"}
                  </Text>
                  <View className="flex-row items-center gap-3 flex-wrap pt-0.5">
                    <Text className="text-xs font-medium text-neutral-700">
                      Tamanho:{" "}
                      <Text className="font-bold">
                        {formatarTamanhoBytes(revisao.pacote?.tamanho_bytes)}
                      </Text>
                    </Text>
                    {revisao.pacote?.tipo_conteudo ? (
                      <Text
                        className="text-[11px] font-mono text-neutral-500"
                        numberOfLines={1}
                      >
                        {revisao.pacote.tipo_conteudo}
                      </Text>
                    ) : null}
                  </View>
                </View>
              </View>

              {/* Bloqueios quando pode_enviar === false */}
              {!revisao.pacote?.pode_enviar ? (
                <View className="rounded-xl border border-red-200 bg-red-50 p-4 gap-2">
                  <View className="flex-row items-center gap-2">
                    <Ionicons name="ban-outline" size={18} color="#b91c1c" />
                    <Text className="text-sm font-bold text-red-900">
                      Envio bloqueado para este pacote
                    </Text>
                  </View>
                  {revisao.pacote?.bloqueios?.length ? (
                    <View className="gap-1 pl-6">
                      {revisao.pacote.bloqueios.map((motivo, idx) => (
                        <Text key={idx} className="text-xs leading-5 text-red-800">
                          • {motivo}
                        </Text>
                      ))}
                    </View>
                  ) : (
                    <Text className="text-xs text-red-800 pl-6">
                      O pacote final não atende aos critérios obrigatórios para envio.
                    </Text>
                  )}
                </View>
              ) : (
                <View className="flex-row items-center gap-2 rounded-xl border border-emerald-200 bg-emerald-50/70 px-4 py-3">
                  <Ionicons name="checkmark-circle" size={18} color="#047857" />
                  <Text className="flex-1 text-xs font-medium text-emerald-900">
                    Pacote validado e pronto para publicação na fila de envio ao representante.
                  </Text>
                </View>
              )}

              {/* Ações de Decisão (Aprovar / Reprovar) */}
              <View className="flex-row flex-wrap items-center justify-end gap-3 border-t border-neutral-100 pt-4">
                <Pressable
                  onPress={abrirModalReprovar}
                  disabled={bloqueadoParaAcoes}
                  className={`flex-row items-center gap-2 rounded-xl border border-red-300 bg-white px-4 py-2.5 ${
                    bloqueadoParaAcoes ? "opacity-50" : "active:bg-red-50"
                  }`}
                >
                  <Ionicons name="close-circle-outline" size={17} color="#b91c1c" />
                  <Text className="text-xs font-bold text-red-700">
                    Reprovar e solicitar correção
                  </Text>
                </Pressable>

                <Pressable
                  onPress={abrirModalAprovar}
                  disabled={!revisao.pacote?.pode_enviar || bloqueadoParaAcoes}
                  className={`flex-row items-center gap-2 rounded-xl px-5 py-2.5 ${
                    revisao.pacote?.pode_enviar && !bloqueadoParaAcoes
                      ? "bg-[#6f4f28] active:bg-[#5b4020]"
                      : "bg-neutral-300 opacity-60"
                  }`}
                >
                  <Ionicons name="paper-plane-outline" size={16} color="#ffffff" />
                  <Text className="text-xs font-bold text-white">
                    Aprovar e publicar para envio
                  </Text>
                </Pressable>
              </View>
            </View>
          </View>
        )}
      </View>

      {/* Modal de Confirmação da Aprovação (POST .../entrega-final/aprovar) */}
      <Modal
        visible={modalAprovarAberto}
        transparent
        animationType="fade"
        onRequestClose={() => {
          if (!aprovando) setModalAprovarAberto(false);
        }}
      >
        <View className="flex-1 items-center justify-center bg-black/50 px-4">
          <View className="w-full max-w-lg rounded-2xl border border-neutral-200 bg-white p-6 shadow-xl gap-4">
            <View className="flex-row items-start justify-between gap-3">
              <View className="flex-row items-center gap-2.5 flex-1">
                <View className="h-10 w-10 items-center justify-center rounded-xl bg-emerald-50 border border-emerald-200">
                  <Ionicons name="paper-plane-outline" size={20} color="#047857" />
                </View>
                <View className="flex-1">
                  <Text className="text-base font-bold text-neutral-900">
                    Confirmar aprovação e publicação
                  </Text>
                  <Text className="text-xs text-neutral-500">
                    O arquivo final será publicado na fila de e-mail para o representante.
                  </Text>
                </View>
              </View>

              <Pressable
                onPress={() => setModalAprovarAberto(false)}
                disabled={aprovando}
                className="p-1"
              >
                <Ionicons name="close" size={20} color="#525252" />
              </Pressable>
            </View>

            <View className="rounded-xl border border-neutral-200 bg-neutral-50 p-3.5 gap-1">
              <Text className="text-xs text-neutral-600">
                <Text className="font-bold text-neutral-800">Destinatário:</Text>{" "}
                {revisao?.cliente?.email || "—"}
              </Text>
              <Text className="text-xs text-neutral-600" numberOfLines={1}>
                <Text className="font-bold text-neutral-800">Arquivo anexo:</Text>{" "}
                {revisao?.pacote?.arquivo_nome || "—"} (
                {formatarTamanhoBytes(revisao?.pacote?.tamanho_bytes)})
              </Text>
            </View>

            <View className="gap-1.5">
              <Text className="text-xs font-semibold text-neutral-800">
                Mensagem personalizada ao cliente (opcional)
              </Text>
              <TextInput
                value={mensagemCliente}
                onChangeText={setMensagemCliente}
                editable={!aprovando}
                multiline
                numberOfLines={3}
                placeholder="Ex.: Segue em anexo o documento final traduzido e revisado."
                placeholderTextColor="#9ca3af"
                className="min-h-[84px] rounded-xl border border-neutral-300 bg-white px-3.5 py-2.5 text-xs text-neutral-900"
                style={{ textAlignVertical: "top" }}
              />
            </View>

            {erroAprovar ? (
              <View className="flex-row items-start gap-2 rounded-xl border border-red-200 bg-red-50 p-3">
                <Ionicons name="alert-circle" size={16} color="#b91c1c" />
                <Text className="flex-1 text-xs leading-5 text-red-800">{erroAprovar}</Text>
              </View>
            ) : null}

            <View className="flex-row items-center justify-end gap-2.5 pt-2">
              <Pressable
                onPress={() => setModalAprovarAberto(false)}
                disabled={aprovando}
                className="rounded-xl border border-neutral-300 bg-white px-4 py-2.5 active:bg-neutral-50"
              >
                <Text className="text-xs font-semibold text-neutral-700">Cancelar</Text>
              </Pressable>

              <Pressable
                onPress={() => void handleConfirmarAprovacao()}
                disabled={aprovando}
                className={`flex-row items-center gap-2 rounded-xl px-5 py-2.5 ${
                  aprovando ? "bg-[#6f4f28]/70" : "bg-[#6f4f28] active:bg-[#5b4020]"
                }`}
              >
                {aprovando ? (
                  <ActivityIndicator size="small" color="#ffffff" />
                ) : (
                  <Ionicons name="checkmark-circle-outline" size={16} color="#ffffff" />
                )}
                <Text className="text-xs font-bold text-white">
                  {aprovando ? "Publicando..." : "Confirmar e publicar"}
                </Text>
              </Pressable>
            </View>
          </View>
        </View>
      </Modal>

      {/* Modal de Reprovação (POST .../entrega-final/reprovar) */}
      <Modal
        visible={modalReprovarAberto}
        transparent
        animationType="fade"
        onRequestClose={() => {
          if (!reprovando) setModalReprovarAberto(false);
        }}
      >
        <View className="flex-1 items-center justify-center bg-black/50 px-4">
          <View className="w-full max-w-lg rounded-2xl border border-neutral-200 bg-white p-6 shadow-xl gap-4">
            <View className="flex-row items-start justify-between gap-3">
              <View className="flex-row items-center gap-2.5 flex-1">
                <View className="h-10 w-10 items-center justify-center rounded-xl bg-red-50 border border-red-200">
                  <Ionicons name="return-up-back-outline" size={20} color="#b91c1c" />
                </View>
                <View className="flex-1">
                  <Text className="text-base font-bold text-neutral-900">
                    Reprovar entrega e solicitar correção
                  </Text>
                  <Text className="text-xs text-neutral-500">
                    A última etapa voltará para ACEITA com o mesmo profissional e prazo.
                  </Text>
                </View>
              </View>

              <Pressable
                onPress={() => setModalReprovarAberto(false)}
                disabled={reprovando}
                className="p-1"
              >
                <Ionicons name="close" size={20} color="#525252" />
              </Pressable>
            </View>

            <View className="gap-1.5">
              <Text className="text-xs font-semibold text-neutral-800">
                Motivo da reprovação <Text className="text-red-600">*</Text>
              </Text>
              <TextInput
                value={motivoReprovacao}
                onChangeText={(texto) => {
                  setMotivoReprovacao(texto);
                  if (erroReprovar) setErroReprovar(null);
                }}
                editable={!reprovando}
                multiline
                numberOfLines={4}
                placeholder="Descreva o que precisa ser corrigido no arquivo final (ex.: Falta a assinatura na última página)."
                placeholderTextColor="#9ca3af"
                className="min-h-[100px] rounded-xl border border-neutral-300 bg-white px-3.5 py-2.5 text-xs text-neutral-900"
                style={{ textAlignVertical: "top" }}
              />
            </View>

            {erroReprovar ? (
              <View className="flex-row items-start gap-2 rounded-xl border border-red-200 bg-red-50 p-3">
                <Ionicons name="alert-circle" size={16} color="#b91c1c" />
                <Text className="flex-1 text-xs leading-5 text-red-800">{erroReprovar}</Text>
              </View>
            ) : null}

            <View className="flex-row items-center justify-end gap-2.5 pt-2">
              <Pressable
                onPress={() => setModalReprovarAberto(false)}
                disabled={reprovando}
                className="rounded-xl border border-neutral-300 bg-white px-4 py-2.5 active:bg-neutral-50"
              >
                <Text className="text-xs font-semibold text-neutral-700">Cancelar</Text>
              </Pressable>

              <Pressable
                onPress={() => void handleConfirmarReprovacao()}
                disabled={reprovando || !motivoReprovacao.trim()}
                className={`flex-row items-center gap-2 rounded-xl px-5 py-2.5 ${
                  reprovando || !motivoReprovacao.trim()
                    ? "bg-red-300"
                    : "bg-red-700 active:bg-red-800"
                }`}
              >
                {reprovando ? (
                  <ActivityIndicator size="small" color="#ffffff" />
                ) : (
                  <Ionicons name="return-up-back" size={16} color="#ffffff" />
                )}
                <Text className="text-xs font-bold text-white">
                  {reprovando ? "Reprovando..." : "Confirmar reprovação"}
                </Text>
              </Pressable>
            </View>
          </View>
        </View>
      </Modal>
    </ScrollView>
  );
}