import { useEffect, useMemo, useState } from "react";
import {
  ActivityIndicator,
  Platform,
  Pressable,
  ScrollView,
  Text,
  View,
} from "react-native";
import { useRouter } from "expo-router";
import { Ionicons } from "@expo/vector-icons";
import axios from "axios";
import {
  baixarArquivoOrdemServico,
  buscarOrdemServicoPorId,
  listarEtapasOrdemServico,
} from "./ordensApi";
import { OrdemServicoStatusBadge } from "./OrdemServicoStatusBadge";
import type {
  OrdemServicoArquivo,
  OrdemServicoDetalhe,
  OrdemServicoEtapaResumo,
} from "./types";
import { getApiErrorMessage } from "../../lib/api";

interface OrdemServicoDetalhesViewProps {
  id: string;
  onVoltar?: () => void;
  ordemInicial?: OrdemServicoDetalhe | null;
}

function formatarData(isoString?: string | null): string {
  if (!isoString) return "—";
  try {
    const data = new Date(isoString);
    return new Intl.DateTimeFormat("pt-BR", {
      dateStyle: "medium",
      timeStyle: "short",
    }).format(data);
  } catch {
    return isoString;
  }
}

function descreverDestinoAvanco(
  etapaAtual: OrdemServicoEtapaResumo,
  todasEtapas: OrdemServicoEtapaResumo[],
): string {
  const proxima = todasEtapas.find((e) => e.ordem === etapaAtual.ordem + 1);

  if (!proxima) {
    return "Última etapa concluída — produção avançou para verificação final da OS";
  }

  const nomeProxima = `Etapa ${proxima.ordem}${proxima.nome ? ` (${proxima.nome})` : ""}`;

  switch (proxima.status) {
    case "DISPARADA":
      return `Avançou para ${nomeProxima} — convite automático disparado`;
    case "ALOCADO":
      return `Avançou para ${nomeProxima} — liberada para o recurso alocado${proxima.recurso_nome ? ` (${proxima.recurso_nome})` : ""}`;
    case "ACEITA":
      return `Avançou para ${nomeProxima} — em execução${proxima.recurso_nome ? ` por ${proxima.recurso_nome}` : ""}`;
    case "ENTREGUE":
      return `Avançou para ${nomeProxima} — já concluída`;
    case "PENDENTE":
      return `Avançou para ${nomeProxima} — liberada como pendente (aguardando alocação/disparo)`;
    default:
      return `Avançou para ${nomeProxima}`;
  }
}

export function OrdemServicoDetalhesView({
  id,
  onVoltar,
  ordemInicial,
}: OrdemServicoDetalhesViewProps) {
  const router = useRouter();
  const [ordem, setOrdem] = useState<OrdemServicoDetalhe | null>(ordemInicial ?? null);
  const [etapas, setEtapas] = useState<OrdemServicoEtapaResumo[]>(
    ordemInicial?.etapas ?? [],
  );
  const [carregando, setCarregando] = useState(!ordemInicial);
  const [naoEncontrada, setNaoEncontrada] = useState(false);
  const [erro, setErro] = useState<string | null>(null);
  const [baixandoId, setBaixandoId] = useState<string | null>(null);

  const carregarDetalhes = async () => {
    setCarregando(true);
    setNaoEncontrada(false);
    setErro(null);

    try {
      const [dados, listaEtapas] = await Promise.all([
        buscarOrdemServicoPorId(id),
        listarEtapasOrdemServico(id),
      ]);
      setOrdem(dados);
      setEtapas(dados.etapas?.length ? dados.etapas : listaEtapas);
    } catch (err: unknown) {
      if (axios.isAxiosError(err) && err.response?.status === 404) {
        setNaoEncontrada(true);
      } else {
        setErro(getApiErrorMessage(err));
      }
    } finally {
      setCarregando(false);
    }
  };

  useEffect(() => {
    if (!ordemInicial || ordemInicial.id !== id) {
      carregarDetalhes();
    } else if (!ordemInicial.etapas?.length) {
      listarEtapasOrdemServico(id).then(setEtapas);
    }
  }, [id]);

  const etapasOrdenadas = useMemo(
    () => [...etapas].sort((a, b) => a.ordem - b.ordem),
    [etapas],
  );

  const etapasEntregues = useMemo(
    () => etapasOrdenadas.filter((e) => e.status === "ENTREGUE"),
    [etapasOrdenadas],
  );

  async function handleBaixar(url: string, nome: string, arquivoId: string) {
    try {
      setBaixandoId(arquivoId);
      await baixarArquivoOrdemServico(url, nome);
    } catch (e) {
      console.error("Falha ao baixar arquivo da OS", e);
    } finally {
      setBaixandoId(null);
    }
  }

  if (carregando) {
    return (
      <View className="items-center justify-center gap-3 rounded-2xl bg-white p-12 border border-neutral-200">
        <ActivityIndicator size="large" color="#6f4f28" />
        <Text className="text-sm font-medium text-neutral-600">
          Carregando informações da Ordem de Serviço...
        </Text>
      </View>
    );
  }

  if (naoEncontrada) {
    return (
      <View className="items-center justify-center rounded-2xl border border-amber-200 bg-amber-50/60 p-8 text-center">
        <View className="mb-3 h-14 w-14 items-center justify-center rounded-full bg-amber-100">
          <Ionicons name="alert-circle-outline" size={32} color="#b45309" />
        </View>
        <Text className="text-xl font-bold text-neutral-900">
          Ordem de Serviço não encontrada
        </Text>
        <Text className="mt-2 max-w-md text-center text-sm text-neutral-600">
          A Ordem de Serviço solicitada não foi encontrada no sistema. Verifique o
          identificador e tente novamente.
        </Text>
        <View className="mt-6 flex-row gap-3">
          {onVoltar ? (
            <Pressable
              onPress={onVoltar}
              className="rounded-lg bg-[#6f4f28] px-5 py-2.5 active:bg-[#5b4020]"
            >
              <Text className="font-medium text-white">Voltar para a listagem</Text>
            </Pressable>
          ) : null}
          <Pressable
            onPress={carregarDetalhes}
            className="rounded-lg border border-neutral-300 bg-white px-4 py-2.5 active:bg-neutral-50"
          >
            <Text className="font-medium text-neutral-700">Tentar novamente</Text>
          </Pressable>
        </View>
      </View>
    );
  }

  if (erro || !ordem) {
    return (
      <View className="items-center justify-center rounded-2xl border border-red-200 bg-red-50/60 p-8">
        <Ionicons name="warning-outline" size={32} color="#dc2626" />
        <Text className="mt-2 text-base font-bold text-red-900">
          Não foi possível carregar a Ordem de Serviço
        </Text>
        <Text className="mt-1 text-center text-sm text-red-700">{erro}</Text>
        <View className="mt-6 flex-row gap-3">
          {onVoltar ? (
            <Pressable
              onPress={onVoltar}
              className="rounded-lg border border-neutral-300 bg-white px-4 py-2.5 active:bg-neutral-50"
            >
              <Text className="font-medium text-neutral-700">Voltar</Text>
            </Pressable>
          ) : null}
          <Pressable
            onPress={carregarDetalhes}
            className="rounded-lg bg-[#6f4f28] px-5 py-2.5 active:bg-[#5b4020]"
          >
            <Text className="font-medium text-white">Tentar novamente</Text>
          </Pressable>
        </View>
      </View>
    );
  }

  const { solicitacao, cliente } = ordem;
  const arquivos = solicitacao?.arquivos ?? [];
  const aguardandoVerificacao = ordem.status === "AGUARDANDO_VERIFICACAO";
  const encerrada = ordem.status === "ENCERRADA";

  return (
    <ScrollView
      className="flex-1"
      contentContainerStyle={{ paddingBottom: 32 }}
      showsVerticalScrollIndicator={false}
    >
      <View className="gap-6">
        {/* Banner de atalho para revisão final (US-015) quando AGUARDANDO_VERIFICACAO */}
        {aguardandoVerificacao ? (
          <View className="flex-col md:flex-row md:items-center justify-between gap-4 rounded-2xl border border-purple-200 bg-purple-50 p-5 shadow-sm">
            <View className="flex-row items-start gap-3 flex-1">
              <View className="h-10 w-10 items-center justify-center rounded-xl bg-purple-100 border border-purple-200">
                <Ionicons name="checkmark-done-circle" size={22} color="#6b21a8" />
              </View>
              <View className="flex-1 gap-1">
                <Text className="text-sm font-bold text-purple-950">
                  Pronta para verificação final (US-015)
                </Text>
                <Text className="text-xs leading-5 text-purple-800">
                  Todas as etapas de produção foram entregues. Revise o pacote final antes do
                  envio ao cliente.
                </Text>
              </View>
            </View>

            <Pressable
              onPress={() => router.push(`/ordens/${ordem.id}/verificacao` as never)}
              className="flex-row items-center justify-center gap-2 self-start md:self-auto rounded-xl bg-purple-700 px-4 py-2.5 active:bg-purple-800 shadow-sm"
              accessibilityRole="button"
              accessibilityLabel="Ir para revisão final da OS"
            >
              <Ionicons name="shield-checkmark-outline" size={17} color="#ffffff" />
              <Text className="text-xs font-bold text-white tracking-wide">
                Revisar pacote final
              </Text>
            </Pressable>
          </View>
        ) : null}

        {/* Banner de OS ENCERRADA com data/autor do envio (US-015 Task 3) */}
        {encerrada || ordem.entregue_em || ordem.entregue_por ? (
          <View className="flex-row items-start gap-3 rounded-2xl border border-emerald-200 bg-emerald-50/80 p-5 shadow-sm">
            <View className="h-10 w-10 items-center justify-center rounded-xl bg-emerald-100 border border-emerald-200">
              <Ionicons name="checkmark-done-circle" size={22} color="#047857" />
            </View>
            <View className="flex-1 gap-1">
              <Text className="text-sm font-bold text-emerald-950">
                Ordem de Serviço Encerrada · Entrega Final Publicada
              </Text>
              <Text className="text-xs leading-5 text-emerald-900">
                {ordem.entregue_em
                  ? `Aprovada e publicada para envio ao cliente em ${formatarData(ordem.entregue_em)}`
                  : "Aprovada e publicada para envio ao cliente"}
                {ordem.entregue_por ? ` por ${ordem.entregue_por}.` : "."}
              </Text>
            </View>
          </View>
        ) : null}

        {/* Banner de OS reaberta com motivo_revisao para o Gestor (CA.11) */}
        {ordem.motivo_revisao ? (
          <View className="flex-row items-start gap-3 rounded-2xl border border-amber-300 bg-amber-50 p-5 shadow-sm">
            <View className="h-10 w-10 items-center justify-center rounded-xl bg-amber-100 border border-amber-200">
              <Ionicons name="return-up-back" size={20} color="#b45309" />
            </View>
            <View className="flex-1 gap-1">
              <Text className="text-sm font-bold text-amber-950">
                Ordem de Serviço reaberta para correção
              </Text>
              <Text className="text-xs text-amber-900">
                Motivo registrado na revisão final:
              </Text>
              <View className="mt-1 rounded-xl border border-amber-200 bg-white/85 px-3.5 py-2">
                <Text className="text-xs font-medium text-neutral-800">
                  “{ordem.motivo_revisao}”
                </Text>
              </View>
            </View>
          </View>
        ) : null}

        {/* Cartão de Resumo e Status */}
        <View className="rounded-2xl border border-neutral-200 bg-white p-5 md:p-6 shadow-sm">
          <View className="flex-col md:flex-row md:items-center justify-between gap-4 pb-4 border-b border-neutral-100">
            <View className="gap-1">
              <Text className="text-xs font-semibold uppercase tracking-wider text-neutral-400">
                Identificador da Ordem
              </Text>
              <View className="flex-row items-center gap-3 flex-wrap">
                <Text className="text-2xl md:text-3xl font-serif font-bold text-neutral-900">
                  {ordem.codigo}
                </Text>
                <OrdemServicoStatusBadge status={ordem.status} />
              </View>
            </View>

            <View className="flex-row items-center gap-4 self-start md:self-auto flex-wrap">
              <View>
                <Text className="text-xs text-neutral-400">Data de criação</Text>
                <Text className="text-sm font-semibold text-neutral-800 mt-0.5">
                  {formatarData(ordem.data_criacao)}
                </Text>
              </View>

              {ordem.entregue_em ? (
                <View>
                  <Text className="text-xs text-neutral-400">Entrega final</Text>
                  <Text className="text-sm font-semibold text-emerald-800 mt-0.5">
                    {formatarData(ordem.entregue_em)}
                  </Text>
                </View>
              ) : null}

              <Pressable
                onPress={() => router.push(`/ordens/${ordem.id}/workflow`)}
                className="flex-row items-center gap-2 rounded-xl bg-[#6f4f28] px-4 py-2.5 active:bg-[#5b4020] shadow-sm hover:opacity-95"
                accessibilityRole="button"
                accessibilityLabel="Acessar Workflow e etapas da OS"
              >
                <Ionicons name="git-network-outline" size={17} color="#ffffff" />
                <Text className="text-xs font-bold text-white tracking-wide">
                  Workflow
                </Text>
              </Pressable>
            </View>
          </View>

          {/* Vínculos complementares */}
          <View className="flex-row flex-wrap gap-4 pt-4 text-xs">
            <View className="flex-row items-center gap-1.5 rounded-lg bg-neutral-50 px-3 py-1.5 border border-neutral-200">
              <Ionicons name="receipt-outline" size={14} color="#737373" />
              <Text className="text-xs text-neutral-500">Orçamento de Origem:</Text>
              <Text className="text-xs font-mono font-medium text-neutral-800">
                {ordem.orcamento_id ? ordem.orcamento_id.substring(0, 8) + "..." : "—"}
              </Text>
            </View>

            <View className="flex-row items-center gap-1.5 rounded-lg bg-neutral-50 px-3 py-1.5 border border-neutral-200">
              <Ionicons name="document-text-outline" size={14} color="#737373" />
              <Text className="text-xs text-neutral-500">Solicitação:</Text>
              <Text className="text-xs font-mono font-medium text-neutral-800">
                {solicitacao?.id ? solicitacao.id.substring(0, 8) + "..." : "—"}
              </Text>
            </View>

            {ordem.entregue_por ? (
              <View className="flex-row items-center gap-1.5 rounded-lg bg-emerald-50/70 px-3 py-1.5 border border-emerald-200">
                <Ionicons name="person-check-outline" size={14} color="#047857" />
                <Text className="text-xs text-emerald-700">Enviada por:</Text>
                <Text className="text-xs font-semibold text-emerald-900">
                  {ordem.entregue_por}
                </Text>
              </View>
            ) : null}
          </View>
        </View>

        {/* Histórico de Etapas Entregues e Avanço da Produção */}
        {etapasEntregues.length > 0 ? (
          <View className="rounded-2xl border border-neutral-200 bg-white p-5 md:p-6 shadow-sm gap-4">
            <View className="flex-row items-center justify-between border-b border-neutral-100 pb-3">
              <View className="flex-row items-center gap-2.5">
                <View className="h-9 w-9 rounded-xl bg-emerald-50 items-center justify-center border border-emerald-200">
                  <Ionicons name="checkmark-done-outline" size={20} color="#047857" />
                </View>
                <View>
                  <Text className="text-base font-bold text-neutral-900">
                    Entregas Registradas & Avanço da Produção
                  </Text>
                  <Text className="text-xs text-neutral-500">
                    Acompanhe quando cada etapa foi entregue e o destino automático do fluxo.
                  </Text>
                </View>
              </View>

              <View className="rounded-full bg-emerald-50 px-2.5 py-0.5 border border-emerald-200">
                <Text className="text-xs font-semibold text-emerald-800">
                  {etapasEntregues.length}/{etapasOrdenadas.length} concluídas
                </Text>
              </View>
            </View>

            <View className="gap-3">
              {etapasEntregues.map((etapa) => (
                <View
                  key={etapa.id}
                  className="gap-2 rounded-xl border border-emerald-200/80 bg-emerald-50/40 p-4"
                >
                  <View className="flex-row flex-wrap items-center justify-between gap-2">
                    <Text className="text-sm font-bold text-neutral-900">
                      Etapa {etapa.ordem} · {etapa.nome}
                    </Text>
                    <View className="flex-row items-center gap-1.5 rounded-full bg-white px-2.5 py-1 border border-emerald-200">
                      <Ionicons name="time-outline" size={13} color="#047857" />
                      <Text className="text-xs font-medium text-emerald-800">
                        Entregue em {formatarData(etapa.entregue_em)}
                      </Text>
                    </View>
                  </View>

                  <View className="flex-row items-center gap-2 pt-1">
                    <Ionicons name="arrow-forward-circle" size={16} color="#8c5230" />
                    <Text className="flex-1 text-xs font-semibold text-[#744127]">
                      {encerrada
                        ? "Etapa concluída — OS encerrada após envio ao cliente"
                        : descreverDestinoAvanco(etapa, etapasOrdenadas)}
                    </Text>
                  </View>

                  {etapa.arquivo_nome ? (
                    <Text className="text-xs text-neutral-500">
                      Arquivo entregue:{" "}
                      <Text className="font-medium text-neutral-700">{etapa.arquivo_nome}</Text>
                    </Text>
                  ) : null}
                </View>
              ))}
            </View>
          </View>
        ) : null}

        {/* Card de Gestão de Workflow e Etapas */}
        <View className="rounded-2xl border border-neutral-200 bg-white p-5 md:p-6 shadow-sm gap-4">
          <View className="flex-col md:flex-row md:items-center justify-between gap-3 border-b border-neutral-100 pb-3">
            <View className="flex-row items-center gap-2.5">
              <View className="h-9 w-9 rounded-xl bg-[#f6efe9] items-center justify-center border border-[#e8d7c8]">
                <Ionicons name="git-network-outline" size={20} color="#8c5230" />
              </View>
              <View>
                <Text className="text-base font-bold text-neutral-900">
                  Workflow & Etapas do Projeto
                </Text>
                <Text className="text-xs text-neutral-500">
                  Defina o fluxo de conclusão do projeto, prazos e alocação de recursos.
                </Text>
              </View>
            </View>

            <Pressable
              onPress={() => router.push(`/ordens/${ordem.id}/workflow`)}
              className="flex-row items-center gap-1.5 self-start md:self-auto rounded-xl border border-[#8c5230]/40 bg-[#fbf8f5] px-3.5 py-2 hover:bg-[#f6efe9] active:bg-[#ede2d6]"
            >
              <Ionicons name="open-outline" size={16} color="#8c5230" />
              <Text className="text-xs font-bold text-[#8c5230]">Abrir Tela do Workflow</Text>
            </Pressable>
          </View>

          <View className="grid grid-cols-1 md:grid-cols-2 gap-4 pt-1">
            <Pressable
              onPress={() => router.push(`/ordens/${ordem.id}/workflow`)}
              className="rounded-xl border border-neutral-200 bg-neutral-50/70 p-4 hover:border-[#8c5230]/40 hover:bg-[#fdfaf8] transition-colors gap-2"
            >
              <View className="flex-row items-center gap-2">
                <Ionicons name="copy-outline" size={18} color="#8c5230" />
                <Text className="text-sm font-bold text-neutral-900">
                  Carregar Modelo de Workflow
                </Text>
              </View>
              <Text className="text-xs text-neutral-600 leading-relaxed">
                Clona um molde de etapas padronizado (ex: Tradução e Revisão) vinculando à OS em suas respectivas ordens numéricas.
              </Text>
            </Pressable>

            <Pressable
              onPress={() => router.push(`/ordens/${ordem.id}/workflow`)}
              className="rounded-xl border border-neutral-200 bg-neutral-50/70 p-4 hover:border-[#8c5230]/40 hover:bg-[#fdfaf8] transition-colors gap-2"
            >
              <View className="flex-row items-center gap-2">
                <Ionicons name="create-outline" size={18} color="#8c5230" />
                <Text className="text-sm font-bold text-neutral-900">
                  Adicionar Etapas Manualmente
                </Text>
              </View>
              <Text className="text-xs text-neutral-600 leading-relaxed">
                Adicione, remova e reordene as etapas dinamicamente em linha do tempo com cálculo de prazos e alocação de recursos.
              </Text>
            </Pressable>
          </View>
        </View>

        {/* Informações Completas do Projeto */}
        <View className="grid grid-cols-1 md:grid-cols-2 gap-6">
          <View className="rounded-2xl border border-neutral-200 bg-white p-5 md:p-6 shadow-sm gap-4">
            <View className="flex-row items-center gap-2 border-b border-neutral-100 pb-3">
              <Ionicons name="people-outline" size={20} color="#6f4f28" />
              <Text className="text-base font-bold text-neutral-900">
                Dados do Cliente
              </Text>
            </View>

            <View className="gap-3">
              <View className="rounded-xl border border-neutral-200 bg-neutral-50/70 p-3.5">
                <Text className="text-xs font-medium text-neutral-500">
                  Nome / Razão Social
                </Text>
                <Text className="text-base font-semibold text-neutral-900 mt-1">
                  {cliente?.nome || "Não informado"}
                </Text>
              </View>

              <View className="rounded-xl border border-neutral-200 bg-neutral-50/70 p-3.5">
                <Text className="text-xs font-medium text-neutral-500">
                  Identificador do Cliente
                </Text>
                <Text className="text-xs font-mono text-neutral-700 mt-1">
                  {cliente?.id || "—"}
                </Text>
              </View>
            </View>
          </View>

          <View className="rounded-2xl border border-neutral-200 bg-white p-5 md:p-6 shadow-sm gap-4">
            <View className="flex-row items-center gap-2 border-b border-neutral-100 pb-3">
              <Ionicons name="layers-outline" size={20} color="#6f4f28" />
              <Text className="text-base font-bold text-neutral-900">
                Categoria & Idiomas
              </Text>
            </View>

            <View className="gap-3">
              <View className="rounded-xl border border-neutral-200 bg-neutral-50/70 p-3.5">
                <Text className="text-xs font-medium text-neutral-500">
                  Categoria do Projeto
                </Text>
                <Text className="text-base font-semibold text-neutral-900 mt-1">
                  {solicitacao?.categoria || "Não definida"}
                </Text>
              </View>

              <View className="rounded-xl border border-neutral-200 bg-neutral-50/70 p-3.5">
                <Text className="text-xs font-medium text-neutral-500 mb-1.5">
                  Par de Idiomas
                </Text>
                <View className="flex-row items-center gap-2 flex-wrap">
                  <View className="rounded-lg bg-neutral-200/80 px-2.5 py-1">
                    <Text className="text-xs font-semibold text-neutral-800">
                      {solicitacao?.idioma_origem || "Origem"}
                    </Text>
                  </View>
                  <Ionicons name="arrow-forward" size={16} color="#737373" />
                  <View className="rounded-lg bg-neutral-200/80 px-2.5 py-1">
                    <Text className="text-xs font-semibold text-neutral-800">
                      {solicitacao?.idioma_destino || "Destino"}
                    </Text>
                  </View>
                </View>
              </View>
            </View>
          </View>
        </View>

        {/* Descrição do Projeto / Instruções */}
        {solicitacao?.descricao ? (
          <View className="rounded-2xl border border-neutral-200 bg-white p-5 md:p-6 shadow-sm gap-3">
            <View className="flex-row items-center gap-2 border-b border-neutral-100 pb-3">
              <Ionicons name="information-circle-outline" size={20} color="#6f4f28" />
              <Text className="text-base font-bold text-neutral-900">
                Descrição e Instruções da Solicitação
              </Text>
            </View>
            <View className="rounded-xl border border-neutral-200 bg-neutral-50/70 p-4">
              <Text className="text-sm leading-relaxed text-neutral-800 whitespace-pre-wrap">
                {solicitacao.descricao}
              </Text>
            </View>
          </View>
        ) : null}

        {/* Arquivos Anexados */}
        <View className="rounded-2xl border border-neutral-200 bg-white p-5 md:p-6 shadow-sm gap-4">
          <View className="flex-row items-center justify-between border-b border-neutral-100 pb-3">
            <View className="flex-row items-center gap-2">
              <Ionicons name="attach-outline" size={20} color="#6f4f28" />
              <Text className="text-base font-bold text-neutral-900">
                Arquivos Anexados
              </Text>
            </View>
            <View className="rounded-full bg-neutral-100 px-2.5 py-0.5 border border-neutral-200">
              <Text className="text-xs font-semibold text-neutral-700">
                {arquivos.length} {arquivos.length === 1 ? "arquivo" : "arquivos"}
              </Text>
            </View>
          </View>

          {arquivos.length === 0 ? (
            <View className="items-center justify-center py-6">
              <Ionicons name="folder-open-outline" size={32} color="#a3a3a3" />
              <Text className="mt-2 text-sm text-neutral-400 italic">
                Nenhum arquivo anexado nesta Ordem de Serviço.
              </Text>
            </View>
          ) : (
            <View className="gap-2.5">
              {arquivos.map((arq: OrdemServicoArquivo) => (
                <View
                  key={arq.id}
                  className="flex-row items-center justify-between rounded-xl border border-neutral-200 bg-neutral-50/50 p-3.5 hover:bg-neutral-50 transition-colors"
                >
                  <View className="flex-row items-center gap-3 flex-1 mr-3 min-w-0">
                    <View className="h-10 w-10 items-center justify-center rounded-lg bg-amber-50 border border-amber-200/60">
                      <Ionicons name="document-text-outline" size={20} color="#8c5230" />
                    </View>
                    <View className="flex-1 min-w-0">
                      <Text
                        className="text-sm font-medium text-neutral-900 truncate"
                        numberOfLines={1}
                      >
                        {arq.nome}
                      </Text>
                      <Text className="text-xs text-neutral-400 font-mono">
                        ID: {arq.id.substring(0, 8)}...
                      </Text>
                    </View>
                  </View>

                  {Platform.OS === "web" ? (
                    <Pressable
                      onPress={() => handleBaixar(arq.url, arq.nome, arq.id)}
                      disabled={baixandoId === arq.id}
                      className="flex-row items-center gap-1.5 rounded-lg border border-neutral-300 bg-white px-3 py-1.5 active:bg-neutral-100"
                      accessibilityRole="button"
                      accessibilityLabel={`Baixar arquivo ${arq.nome}`}
                    >
                      {baixandoId === arq.id ? (
                        <ActivityIndicator size="small" color="#6f4f28" />
                      ) : (
                        <Ionicons name="download-outline" size={16} color="#6f4f28" />
                      )}
                      <Text className="text-xs font-medium text-neutral-700">Baixar</Text>
                    </Pressable>
                  ) : null}
                </View>
              ))}
            </View>
          )}
        </View>
      </View>
    </ScrollView>
  );
}