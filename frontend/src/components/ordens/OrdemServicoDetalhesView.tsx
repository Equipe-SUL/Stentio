import { useEffect, useState } from "react";
import {
  ActivityIndicator,
  Platform,
  Pressable,
  ScrollView,
  Text,
  View,
} from "react-native";
import { Ionicons } from "@expo/vector-icons";
import axios from "axios";
import { baixarArquivoOrdemServico, buscarOrdemServicoPorId } from "./ordensApi";
import { OrdemServicoStatusBadge } from "./OrdemServicoStatusBadge";
import type { OrdemServicoArquivo, OrdemServicoDetalhe } from "./types";
import { getApiErrorMessage } from "../../lib/api";

interface OrdemServicoDetalhesViewProps {
  id: string;
  onVoltar?: () => void;
  ordemInicial?: OrdemServicoDetalhe | null;
}

function formatarData(isoString?: string): string {
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

export function OrdemServicoDetalhesView({
  id,
  onVoltar,
  ordemInicial,
}: OrdemServicoDetalhesViewProps) {
  const [ordem, setOrdem] = useState<OrdemServicoDetalhe | null>(ordemInicial ?? null);
  const [carregando, setCarregando] = useState(!ordemInicial);
  const [naoEncontrada, setNaoEncontrada] = useState(false);
  const [erro, setErro] = useState<string | null>(null);
  const [baixandoId, setBaixandoId] = useState<string | null>(null);

  const carregarDetalhes = async () => {
    setCarregando(true);
    setNaoEncontrada(false);
    setErro(null);

    try {
      const dados = await buscarOrdemServicoPorId(id);
      setOrdem(dados);
    } catch (err: unknown) {
      if (axios.isAxiosError(err) && err.response?.status === 404) {
        // RN1: Caso a consulta interna retorne o erro HTTP 404,
        // a interface deve exibir uma mensagem informando que a Ordem de Serviço não foi encontrada.
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
    }
  }, [id]);

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

  // RN1: Mensagem de erro HTTP 404
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

  return (
    <ScrollView
      className="flex-1"
      contentContainerStyle={{ paddingBottom: 32 }}
      showsVerticalScrollIndicator={false}
    >
      <View className="gap-6">
        {/* Cartão de Resumo e Status (CA3) */}
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

            <View className="flex-row items-center gap-6 self-start md:self-auto">
              <View>
                <Text className="text-xs text-neutral-400">Data de criação</Text>
                <Text className="text-sm font-semibold text-neutral-800 mt-0.5">
                  {formatarData(ordem.data_criacao)}
                </Text>
              </View>
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
          </View>
        </View>

        {/* Informações Completas do Projeto (CA2) */}
        <View className="grid grid-cols-1 md:grid-cols-2 gap-6">
          {/* Dados do Cliente */}
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

          {/* Dados da Solicitação e Categoria */}
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

        {/* Arquivos Anexados (CA2) */}
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
