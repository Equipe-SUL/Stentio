import { useState } from "react";
import {
  ActivityIndicator,
  Modal,
  Platform,
  Pressable,
  ScrollView,
  Text,
  View,
} from "react-native";
import { Ionicons } from "@expo/vector-icons";
import type { SolicitacaoResponse } from "./types";
import { SolicitacaoStatusBadge } from "./SolicitacaoStatusBadge";
import { baixarArquivoSolicitacao } from "./solicitacoesApi";

interface SolicitacaoDetalhesModalProps {
  solicitacao: SolicitacaoResponse | null;
  onClose: () => void;
}

function formatarData(isoString?: string): string {
  if (!isoString) return "—";
  try {
    const data = new Date(isoString);
    return new Intl.DateTimeFormat("pt-BR", {
      dateStyle: "short",
      timeStyle: "short",
    }).format(data);
  } catch {
    return isoString;
  }
}

function formatarTamanho(bytes?: number): string {
  if (bytes === undefined) return "";
  if (bytes < 1024) return `${bytes} B`;
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(0)} KB`;
  return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
}

export function SolicitacaoDetalhesModal({
  solicitacao,
  onClose,
}: SolicitacaoDetalhesModalProps) {
  const [baixandoId, setBaixandoId] = useState<string | null>(null);

  if (!solicitacao) return null;

  async function handleBaixar(url: string, nome: string, id: string) {
    try {
      setBaixandoId(id);
      await baixarArquivoSolicitacao(url, nome);
    } catch (e) {
      console.error("Falha ao baixar arquivo", e);
    } finally {
      setBaixandoId(null);
    }
  }

  const arquivos = solicitacao.arquivos ?? [];

  return (
    <Modal
      visible={true}
      transparent
      animationType="fade"
      onRequestClose={onClose}
    >
      <View className="flex-1 items-center justify-center bg-black/50 p-4">
        <View className="w-full max-w-2xl max-h-[90%] rounded-2xl bg-white shadow-xl overflow-hidden flex-col">
          {/* Cabeçalho do modal */}
          <View className="flex-row items-center justify-between border-b border-neutral-200 px-6 py-4 bg-neutral-50">
            <View className="flex-1 mr-4">
              <Text className="text-xs uppercase font-bold text-neutral-400 tracking-wider">
                Detalhes da Solicitação
              </Text>
              <Text className="text-lg font-bold text-neutral-900 truncate" numberOfLines={1}>
                {solicitacao.cliente.nomeEmpresa}
              </Text>
            </View>
            <Pressable
              onPress={onClose}
              hitSlop={8}
              accessibilityRole="button"
              accessibilityLabel="Fechar modal"
              className="rounded-lg p-1 text-neutral-400 hover:bg-neutral-200 active:bg-neutral-300"
            >
              <Ionicons name="close" size={24} color="#737373" />
            </Pressable>
          </View>

          {/* Conteúdo rolável */}
          <ScrollView className="flex-1 px-6 py-4">
            <View className="gap-6 pb-4">
              {/* Status e protocolo */}
              <View className="flex-row items-center justify-between rounded-xl bg-neutral-50 p-4 border border-neutral-200">
                <View>
                  <Text className="text-xs text-neutral-500">Status atual</Text>
                  <View className="mt-1">
                    <SolicitacaoStatusBadge status={solicitacao.status} />
                  </View>
                </View>
                <View className="items-end">
                  <Text className="text-xs text-neutral-500">Data de recebimento</Text>
                  <Text className="text-sm font-semibold text-neutral-800 mt-1">
                    {formatarData(solicitacao.dataRecebimento)}
                  </Text>
                </View>
              </View>

              {/* Informações principais */}
              <View className="gap-3">
                <Text className="text-sm font-bold text-neutral-900">
                  Dados do Projeto
                </Text>

                <View className="grid grid-cols-1 md:grid-cols-2 gap-3">
                  <View className="rounded-xl border border-neutral-200 p-3 bg-white">
                    <Text className="text-xs text-neutral-400">Cliente</Text>
                    <Text className="text-sm font-medium text-neutral-900 mt-0.5">
                      {solicitacao.cliente.nomeEmpresa}
                    </Text>
                  </View>

                  <View className="rounded-xl border border-neutral-200 p-3 bg-white">
                    <Text className="text-xs text-neutral-400">Categoria do projeto</Text>
                    <Text className="text-sm font-medium text-neutral-900 mt-0.5">
                      {solicitacao.categoriaProjeto.nome}
                    </Text>
                  </View>
                </View>

                <View className="rounded-xl border border-neutral-200 p-3 bg-white">
                  <Text className="text-xs text-neutral-400">Pares de idiomas</Text>
                  <View className="flex-row items-center gap-2 mt-1">
                    <Text className="text-sm font-medium text-neutral-800">
                      {solicitacao.idiomaOrigem.nome} ({solicitacao.idiomaOrigem.codigoIso.toUpperCase()})
                    </Text>
                    <Ionicons name="arrow-forward" size={16} color="#737373" />
                    <Text className="text-sm font-medium text-neutral-800">
                      {solicitacao.idiomaDestino.nome} ({solicitacao.idiomaDestino.codigoIso.toUpperCase()})
                    </Text>
                  </View>
                </View>
              </View>

              {/* Descrição */}
              {solicitacao.descricao ? (
                <View className="gap-2">
                  <Text className="text-sm font-bold text-neutral-900">
                    Descrição / Instruções
                  </Text>
                  <View className="rounded-xl border border-neutral-200 bg-neutral-50 p-4">
                    <Text className="text-sm text-neutral-700 leading-relaxed">
                      {solicitacao.descricao}
                    </Text>
                  </View>
                </View>
              ) : null}

              {/* Arquivos */}
              <View className="gap-2">
                <Text className="text-sm font-bold text-neutral-900">
                  Arquivos Anexados ({arquivos.length})
                </Text>

                {arquivos.length === 0 ? (
                  <Text className="text-sm text-neutral-400 italic">
                    Nenhum arquivo enviado nesta solicitação.
                  </Text>
                ) : (
                  <View className="gap-2">
                    {arquivos.map((arq) => (
                      <View
                        key={arq.id}
                        className="flex-row items-center justify-between rounded-xl border border-neutral-200 bg-white p-3"
                      >
                        <View className="flex-row items-center gap-3 flex-1 mr-3 min-w-0">
                          <Ionicons name="document-text-outline" size={22} color="#6f4f28" />
                          <View className="flex-1 min-w-0">
                            <Text className="text-sm font-medium text-neutral-800 truncate" numberOfLines={1}>
                              {arq.nome}
                            </Text>
                            <Text className="text-xs text-neutral-400">
                              {formatarTamanho(arq.tamanhoBytes)}
                            </Text>
                          </View>
                        </View>

                        {Platform.OS === "web" ? (
                          <Pressable
                            onPress={() => handleBaixar(arq.url, arq.nome, arq.id)}
                            disabled={baixandoId === arq.id}
                            className="flex-row items-center gap-1.5 rounded-lg border border-neutral-300 bg-neutral-50 px-3 py-1.5 active:bg-neutral-200"
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

          {/* Rodapé com botão de fechar */}
          <View className="border-t border-neutral-200 px-6 py-4 flex-row justify-end bg-neutral-50">
            <Pressable
              onPress={onClose}
              className="rounded-lg bg-neutral-800 px-5 py-2.5 active:bg-neutral-900"
            >
              <Text className="text-sm font-medium text-white">Fechar</Text>
            </Pressable>
          </View>
        </View>
      </View>
    </Modal>
  );
}
