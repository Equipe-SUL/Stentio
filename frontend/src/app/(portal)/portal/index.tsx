import React, { useCallback, useEffect, useMemo, useState } from "react";
import { ActivityIndicator, Platform, Pressable, ScrollView, Text, View } from "react-native";
import { Ionicons } from "@expo/vector-icons";
import * as DocumentPicker from "expo-document-picker";
import { isAxiosError } from "axios";
import { getApiErrorMessage } from "../../../lib/api";
import {
  aceitarEtapaRecurso,
  baixarEntrega,
  entregarEtapaRecurso,
  listarMinhasDemandas,
  recusarEtapaRecurso,
  type DemandaRecurso,
  type EntregaEtapaResponse,
} from "../../../components/recursos/portalRecursoApi";

type AbaPortal = "MINHAS" | "DISPONIVEIS";

const LIMITE_ENTREGA_BYTES = 10 * 1024 * 1024;
const EXTENSOES_ENTREGA = new Set(["pdf", "doc", "docx", "odt", "rtf", "txt", "xls", "xlsx", "ppt", "pptx", "jpg", "jpeg", "png"]);

const STATUS: Record<string, { label: string; cor: string }> = {
  PENDENTE: { label: "Pendente", cor: "bg-amber-50 text-amber-800 border-amber-200" },
  AGUARDANDO_ETAPA_ANTERIOR: {
    label: "Aguardando etapa anterior",
    cor: "bg-neutral-100 text-neutral-700 border-neutral-200",
  },
  ALOCADO: { label: "Alocada", cor: "bg-emerald-50 text-emerald-800 border-emerald-200" },
  DISPARADA: { label: "Convite aberto", cor: "bg-sky-50 text-sky-800 border-sky-200" },
  ACEITA: { label: "Em execução", cor: "bg-indigo-50 text-indigo-800 border-indigo-200" },
  ENTREGUE: { label: "Entregue", cor: "bg-neutral-100 text-neutral-700 border-neutral-300" },
};

function formatarDataHora(valor?: string | null): string {
  if (!valor) return "Prazo não definido";
  const data = new Date(valor);
  if (Number.isNaN(data.getTime())) return "Prazo não definido";
  return new Intl.DateTimeFormat("pt-BR", { dateStyle: "medium", timeStyle: "short" }).format(data);
}

function isEtapaLiberada(demanda: DemandaRecurso): boolean {
  if (typeof demanda.liberada === "boolean") return demanda.liberada;
  return demanda.ordem === 1;
}

function resumirResultadoAvanco(resultado: EntregaEtapaResponse): {
  titulo: string;
  descricao: string;
} {
  const { avanco, ordem_servico } = resultado;

  if (!avanco || ordem_servico?.status === "AGUARDANDO_VERIFICACAO") {
    return {
      titulo: "Entrega registrada · OS aguardando verificação",
      descricao: `O arquivo "${resultado.arquivo_nome}" concluiu a última etapa. A Ordem de Serviço avançou para verificação final.`,
    };
  }

  if (!avanco.convite_automatico && avanco.recursos_notificados === 0) {
    return {
      titulo: "Entrega registrada · Próxima etapa pendente",
      descricao: `O arquivo "${resultado.arquivo_nome}" foi enviado. A próxima etapa foi liberada, mas ficou pendente porque nenhum recurso elegível foi encontrado para disparo automático.`,
    };
  }

  if (avanco.convite_automatico) {
    const qtd = avanco.recursos_notificados;
    return {
      titulo: "Entrega registrada · Convite automático enviado",
      descricao:
        qtd > 0
          ? `O arquivo "${resultado.arquivo_nome}" foi enviado. A próxima etapa foi liberada e disparada automaticamente para ${qtd} ${qtd === 1 ? "recurso elegível" : "recursos elegíveis"}.`
          : `O arquivo "${resultado.arquivo_nome}" foi enviado e a próxima etapa foi liberada como pendente (nenhum recurso elegível encontrado para disparo automático).`,
    };
  }

  const destinoLabel =
    avanco.proxima_etapa_status === "ACEITA"
      ? "já aceita pelo próximo recurso"
      : "alocada ao próximo recurso";

  return {
    titulo: "Entrega registrada · Próxima etapa liberada",
    descricao: `O arquivo "${resultado.arquivo_nome}" foi enviado. A próxima etapa (${destinoLabel}) foi liberada e o responsável já foi notificado.`,
  };
}

export default function MinhasDemandasScreen() {
  const [demandas, setDemandas] = useState<DemandaRecurso[]>([]);
  const [abaAtiva, setAbaAtiva] = useState<AbaPortal>("MINHAS");
  const [carregando, setCarregando] = useState(true);
  const [erro, setErro] = useState("");
  const [erroAcao, setErroAcao] = useState("");
  const [etapaProcessandoId, setEtapaProcessandoId] = useState<string | null>(null);
  const [resultadoEntrega, setResultadoEntrega] = useState<EntregaEtapaResponse | null>(null);
  const [avisoAceite, setAvisoAceite] = useState("");
  const [progresso, setProgresso] = useState<number | null>(null);

  const carregar = useCallback(async () => {
    try {
      setDemandas(await listarMinhasDemandas());
      setErro("");
    } catch (error) {
      setErro(getApiErrorMessage(error));
    } finally {
      setCarregando(false);
    }
  }, []);

  useEffect(() => {
    let ativo = true;

    listarMinhasDemandas()
      .then((dados) => {
        if (ativo) {
          setDemandas(dados);
          setErro("");
        }
      })
      .catch((error: unknown) => {
        if (ativo) setErro(getApiErrorMessage(error));
      })
      .finally(() => {
        if (ativo) setCarregando(false);
      });

    return () => {
      ativo = false;
    };
  }, []);

  const disponiveis = useMemo(
    () => demandas.filter((d) => d.situacao === "DISPARADA"),
    [demandas],
  );

  const minhasDemandas = useMemo(
    () => demandas.filter((d) => d.situacao !== "DISPARADA"),
    [demandas],
  );

  const listaExibida = abaAtiva === "DISPONIVEIS" ? disponiveis : minhasDemandas;

  async function handleAceitar(etapaId: string) {
    setErroAcao("");
    setAvisoAceite("");
    setEtapaProcessandoId(etapaId);
    try {
      await aceitarEtapaRecurso(etapaId);
      setAvisoAceite("Etapa aceita com sucesso! Ela já está disponível em Minhas demandas.");
      setAbaAtiva("MINHAS");
      await carregar();
    } catch (error) {
      if (isAxiosError(error) && error.response?.status === 409) {
        setErroAcao(
          "Esta etapa já foi aceita por outro recurso ou ainda aguarda a conclusão da etapa anterior.",
        );
        await carregar();
      } else {
        setErroAcao(getApiErrorMessage(error));
      }
    } finally {
      setEtapaProcessandoId(null);
    }
  }

  async function handleRecusar(etapaId: string) {
    setErroAcao("");
    setAvisoAceite("");
    setEtapaProcessandoId(etapaId);
    try {
      await recusarEtapaRecurso(etapaId);
      setAvisoAceite("Alocação recusada. O gestor será notificado.");
      await carregar();
    } catch (error) {
      if (isAxiosError(error) && error.response?.status === 409) {
        setErroAcao("Esta etapa não pode mais ser recusada. A lista foi atualizada.");
        await carregar();
      } else {
        setErroAcao(getApiErrorMessage(error));
      }
    } finally {
      setEtapaProcessandoId(null);
    }
  }

  async function handleBaixarEntrega(demanda: DemandaRecurso) {
    if (!demanda.arquivo_entrega_url) return;
    setErroAcao("");
    try {
      await baixarEntrega(demanda.arquivo_entrega_url, demanda.arquivo_nome || "entrega");
    } catch (error) {
      setErroAcao(getApiErrorMessage(error));
    }
  }

  async function handleSelecionarEEntregar(demanda: DemandaRecurso) {
    // Bloqueio defensivo na UI caso já esteja entregue
    if (demanda.situacao === "ENTREGUE") return;

    setErroAcao("");
    setAvisoAceite("");

    try {
      const selecao = await DocumentPicker.getDocumentAsync({
        copyToCacheDirectory: true,
        multiple: false,
      });

      if (selecao.canceled || !selecao.assets?.length) return;

      const asset = selecao.assets[0];
      const extensao = asset.name.split(".").pop()?.toLowerCase() ?? "";
      if (!EXTENSOES_ENTREGA.has(extensao)) {
        setErroAcao("Tipo de arquivo não permitido. Envie PDF, Office, texto ou imagem compatível.");
        return;
      }
      if (asset.size !== undefined && asset.size > LIMITE_ENTREGA_BYTES) {
        setErroAcao("O arquivo ultrapassa o limite de 10 MB.");
        return;
      }

      setEtapaProcessandoId(demanda.etapa_id);
      setProgresso(0);

      const resposta = await entregarEtapaRecurso(
        demanda.etapa_id,
        {
          uri: asset.uri,
          name: asset.name,
          mimeType: asset.mimeType,
          file: (asset as unknown as { file?: File }).file,
        },
        setProgresso,
      );

      setResultadoEntrega(resposta);
      await carregar();
    } catch (error) {
      if (isAxiosError(error) && error.response?.status === 409) {
        setErroAcao(
          "Não foi possível registrar a entrega: a etapa já foi entregue ou a etapa anterior ainda não foi concluída.",
        );
        await carregar();
      } else {
        setErroAcao(getApiErrorMessage(error));
      }
    } finally {
      setEtapaProcessandoId(null);
      setProgresso(null);
    }
  }

  const resumoAvanco = resultadoEntrega ? resumirResultadoAvanco(resultadoEntrega) : null;

  return (
    <ScrollView className="flex-1" contentContainerClassName="items-center p-5 py-8 md:p-8">
      <View className="w-full max-w-5xl gap-6">
        <View className="flex-row flex-wrap items-start justify-between gap-4">
          <View className="flex-1 gap-1">
            <Text className="text-2xl font-bold text-neutral-900">Demandas de produção</Text>
            <Text className="text-sm text-neutral-600">
              Acompanhe suas etapas alocadas, convites abertos e registre suas entregas.
            </Text>
          </View>

          <View className="flex-row rounded-xl border border-neutral-200 bg-white p-1">
            <Pressable
              accessibilityRole="tab"
              accessibilityState={{ selected: abaAtiva === "MINHAS" }}
              onPress={() => setAbaAtiva("MINHAS")}
              className={`flex-row items-center gap-2 rounded-lg px-3.5 py-2 ${
                abaAtiva === "MINHAS" ? "bg-[#8c5230]" : "bg-transparent"
              }`}
            >
              <Text
                className={`text-xs font-semibold ${
                  abaAtiva === "MINHAS" ? "text-white" : "text-neutral-700"
                }`}
              >
                Minhas demandas
              </Text>
              <View
                className={`rounded-full px-2 py-0.5 ${
                  abaAtiva === "MINHAS" ? "bg-white/20" : "bg-neutral-100"
                }`}
              >
                <Text
                  className={`text-xs font-bold ${
                    abaAtiva === "MINHAS" ? "text-white" : "text-neutral-700"
                  }`}
                >
                  {minhasDemandas.length}
                </Text>
              </View>
            </Pressable>

            <Pressable
              accessibilityRole="tab"
              accessibilityState={{ selected: abaAtiva === "DISPONIVEIS" }}
              onPress={() => setAbaAtiva("DISPONIVEIS")}
              className={`flex-row items-center gap-2 rounded-lg px-3.5 py-2 ${
                abaAtiva === "DISPONIVEIS" ? "bg-[#8c5230]" : "bg-transparent"
              }`}
            >
              <Text
                className={`text-xs font-semibold ${
                  abaAtiva === "DISPONIVEIS" ? "text-white" : "text-neutral-700"
                }`}
              >
                Disponíveis
              </Text>
              <View
                className={`rounded-full px-2 py-0.5 ${
                  abaAtiva === "DISPONIVEIS"
                    ? "bg-white/20"
                    : disponiveis.length > 0
                      ? "bg-sky-100"
                      : "bg-neutral-100"
                }`}
              >
                <Text
                  className={`text-xs font-bold ${
                    abaAtiva === "DISPONIVEIS"
                      ? "text-white"
                      : disponiveis.length > 0
                        ? "text-sky-800"
                        : "text-neutral-700"
                  }`}
                >
                  {disponiveis.length}
                </Text>
              </View>
            </Pressable>
          </View>
        </View>

        {/* Feedback do avanço após entrega (CA.3 / CA.4 / CA.8) */}
        {resumoAvanco ? (
          <View className="flex-row items-start justify-between gap-3 rounded-2xl border border-emerald-200 bg-emerald-50 p-4">
            <View className="mt-0.5">
              <Ionicons name="checkmark-circle" size={22} color="#047857" />
            </View>
            <View className="flex-1 gap-1">
              <Text className="text-sm font-bold text-emerald-900">{resumoAvanco.titulo}</Text>
              <Text className="text-sm leading-5 text-emerald-800">{resumoAvanco.descricao}</Text>
            </View>
            <Pressable
              accessibilityLabel="Fechar aviso de entrega"
              onPress={() => setResultadoEntrega(null)}
              className="p-1"
            >
              <Ionicons name="close" size={18} color="#065f46" />
            </Pressable>
          </View>
        ) : null}

        {avisoAceite ? (
          <View className="flex-row items-center justify-between gap-3 rounded-2xl border border-emerald-200 bg-emerald-50 p-4">
            <View className="flex-1 flex-row items-center gap-2.5">
              <Ionicons name="checkmark-circle-outline" size={20} color="#047857" />
              <Text className="flex-1 text-sm font-medium text-emerald-900">{avisoAceite}</Text>
            </View>
            <Pressable onPress={() => setAvisoAceite("")} className="p-1">
              <Ionicons name="close" size={18} color="#065f46" />
            </Pressable>
          </View>
        ) : null}

        {erroAcao ? (
          <View className="flex-row items-center justify-between gap-3 rounded-2xl border border-red-200 bg-red-50 p-4">
            <View className="flex-1 flex-row items-center gap-2.5">
              <Ionicons name="alert-circle-outline" size={20} color="#b91c1c" />
              <Text accessibilityRole="alert" className="flex-1 text-sm text-red-800">
                {erroAcao}
              </Text>
            </View>
            <Pressable onPress={() => setErroAcao("")} className="p-1">
              <Ionicons name="close" size={18} color="#991b1b" />
            </Pressable>
          </View>
        ) : null}

        {carregando ? (
          <View className="items-center justify-center gap-3 rounded-2xl border border-neutral-200 bg-white p-12">
            <ActivityIndicator color="#8c5230" />
            <Text className="text-sm text-neutral-500">Carregando suas demandas...</Text>
          </View>
        ) : erro ? (
          <View className="items-center gap-3 rounded-2xl border border-red-200 bg-white p-8">
            <Ionicons name="cloud-offline-outline" size={30} color="#b91c1c" />
            <Text accessibilityRole="alert" className="text-center text-sm text-red-700">
              {erro}
            </Text>
            <Pressable
              onPress={() => void carregar()}
              className="rounded-lg border border-neutral-300 px-4 py-2"
            >
              <Text className="font-medium text-neutral-700">Tentar novamente</Text>
            </Pressable>
          </View>
        ) : listaExibida.length === 0 ? (
          <View className="items-center gap-3 rounded-2xl border border-neutral-200 bg-white p-10">
            <View className="h-14 w-14 items-center justify-center rounded-full bg-neutral-100">
              <Ionicons
                name={abaAtiva === "DISPONIVEIS" ? "sparkles-outline" : "briefcase-outline"}
                size={25}
                color="#8c5230"
              />
            </View>
            <Text className="text-base font-semibold text-neutral-900">
              {abaAtiva === "DISPONIVEIS"
                ? "Nenhum convite disponível no momento"
                : "Nenhuma demanda destinada"}
            </Text>
            <Text className="max-w-md text-center text-sm leading-5 text-neutral-500">
              {abaAtiva === "DISPONIVEIS"
                ? "Quando uma etapa compatível com suas competências for disparada, o convite aparecerá aqui."
                : "Quando uma etapa for atribuída ou aceita por você, ela aparecerá nesta lista."}
            </Text>
          </View>
        ) : (
          <View className="gap-3">
            {listaExibida.map((demanda) => {
              const status = STATUS[demanda.situacao] ?? {
                label: demanda.situacao,
                cor: "bg-neutral-100 text-neutral-700 border-neutral-200",
              };
              const liberada = isEtapaLiberada(demanda);
              const entregue = demanda.situacao === "ENTREGUE";
              const destacadaLiberada =
                !entregue &&
                liberada &&
                (demanda.situacao === "ALOCADO" || demanda.situacao === "ACEITA");
              const processando = etapaProcessandoId === demanda.etapa_id;

              return (
                <View
                  key={demanda.etapa_id}
                  className={`gap-4 rounded-2xl border p-5 shadow-sm ${
                    destacadaLiberada
                      ? "border-[#8c5230] bg-[#fdfaf8]"
                      : entregue
                        ? "border-neutral-200 bg-neutral-50/70"
                        : "border-neutral-200 bg-white"
                  }`}
                >
                  <View className="flex-row flex-wrap items-start justify-between gap-3">
                    <View className="flex-1 gap-1.5">
                      <View className="flex-row flex-wrap items-center gap-2">
                        <Text className="text-xs font-semibold uppercase tracking-wide text-neutral-500">
                          OS {demanda.ordem_servico_codigo || demanda.ordem_servico_id} · Etapa{" "}
                          {demanda.ordem}
                        </Text>
                        {destacadaLiberada ? (
                          <View className="flex-row items-center gap-1 rounded-full bg-[#f5ede7] px-2.5 py-0.5">
                            <Ionicons name="flash" size={12} color="#8c5230" />
                            <Text className="text-[11px] font-bold text-[#744127]">
                              Etapa liberada
                            </Text>
                          </View>
                        ) : null}
                      </View>
                      <Text className="text-base font-bold text-neutral-900">
                        {demanda.nome || `Etapa ${demanda.ordem}`}
                      </Text>
                    </View>

                    <View className={`rounded-full border px-3 py-1.5 ${status.cor}`}>
                      <Text className="text-xs font-semibold">{status.label}</Text>
                    </View>
                  </View>

                  <View className="flex-row flex-wrap gap-x-8 gap-y-3 border-t border-neutral-100 pt-4">
                    <View className="min-w-[130px] flex-1 gap-1">
                      <Text className="text-xs text-neutral-500">Tipo de serviço</Text>
                      <Text className="text-sm font-medium text-neutral-800">
                        {demanda.tipo_servico}
                      </Text>
                    </View>
                    <View className="min-w-[170px] flex-1 gap-1">
                      <Text className="text-xs text-neutral-500">Par de idiomas</Text>
                      <Text className="text-sm font-medium text-neutral-800">
                        {demanda.idioma_origem} → {demanda.idioma_destino}
                      </Text>
                    </View>
                    <View className="min-w-[160px] flex-1 gap-1">
                      <Text className="text-xs text-neutral-500">Prazo</Text>
                      <Text className="text-sm font-medium text-neutral-800">
                        {formatarDataHora(demanda.data_limite_entrega)}
                      </Text>
                    </View>
                    <View className="min-w-[130px] flex-1 gap-1">
                      <Text className="text-xs text-neutral-500">Estimativa</Text>
                      <Text className="text-sm font-medium text-neutral-800">
                        {demanda.prazo_estimado_horas} h
                      </Text>
                    </View>
                  </View>

                  {/* Alerta de reabertura / motivo da reprovação (US-015 Task 3 · CA.11/CA.13) */}
                  {demanda.situacao === "ACEITA" && demanda.motivo_revisao ? (
                    <View className="flex-row items-start gap-2.5 rounded-xl border border-amber-300 bg-amber-50 p-3.5">
                      <Ionicons name="alert-circle" size={18} color="#b45309" />
                      <View className="flex-1 gap-0.5">
                        <Text className="text-xs font-bold text-amber-950">
                          Entrega devolvida para correção pelo Gestor
                        </Text>
                        <Text className="text-xs leading-5 text-amber-900">
                          Motivo: “{demanda.motivo_revisao}”
                        </Text>
                      </View>
                    </View>
                  ) : null}

                  {/* Barra de ações por situação da etapa */}
                  <View className="flex-row flex-wrap items-center justify-between gap-3 border-t border-neutral-100 pt-3">
                    {entregue ? (
                      <>
                        <View className="flex-1 flex-row items-center gap-2">
                          <Ionicons name="checkmark-done-circle" size={18} color="#047857" />
                          <Text className="flex-1 text-xs font-medium text-neutral-600">
                            Entrega concluída
                            {demanda.arquivo_nome ? ` (${demanda.arquivo_nome})` : ""}
                            {demanda.entregue_em
                              ? ` em ${formatarDataHora(demanda.entregue_em)}`
                              : ""}
                            . Reenvio bloqueado.
                          </Text>
                        </View>
                        {demanda.arquivo_entrega_url && Platform.OS === "web" ? (
                          <Pressable
                            onPress={() => void handleBaixarEntrega(demanda)}
                            className="flex-row items-center gap-2 rounded-xl border border-neutral-300 bg-white px-4 py-2.5"
                          >
                            <Ionicons name="download-outline" size={16} color="#404040" />
                            <Text className="text-xs font-semibold text-neutral-700">
                              Baixar arquivo entregue
                            </Text>
                          </Pressable>
                        ) : null}
                      </>
                    ) : demanda.situacao === "DISPARADA" ? (
                      <>
                        <Text className="text-xs text-neutral-500">
                          Convite aberto para profissionais compatíveis.
                        </Text>
                        <Pressable
                          disabled={processando}
                          onPress={() => void handleAceitar(demanda.etapa_id)}
                          className="flex-row items-center gap-2 rounded-xl bg-[#8c5230] px-4 py-2.5"
                        >
                          {processando ? (
                            <ActivityIndicator size="small" color="#fff" />
                          ) : (
                            <>
                              <Ionicons name="hand-left-outline" size={16} color="#fff" />
                              <Text className="text-xs font-semibold text-white">
                                Aceitar etapa
                              </Text>
                            </>
                          )}
                        </Pressable>
                      </>
                    ) : demanda.situacao === "ALOCADO" ? (
                      <>
                        <Text className="text-xs text-neutral-600">
                          {liberada
                            ? "Você foi alocado e a etapa já está liberada para início."
                            : "Convite nominal: você já pode aceitar antecipadamente, mas a entrega aguarda a etapa anterior."}
                        </Text>
                        <View className="flex-row flex-wrap gap-2">
                          <Pressable
                            disabled={processando}
                            onPress={() => void handleRecusar(demanda.etapa_id)}
                            className="rounded-xl border border-neutral-300 bg-white px-4 py-2.5"
                          >
                            <Text className="text-xs font-semibold text-neutral-700">Recusar</Text>
                          </Pressable>
                          <Pressable
                            disabled={processando}
                            onPress={() => void handleAceitar(demanda.etapa_id)}
                            className="flex-row items-center gap-2 rounded-xl bg-[#8c5230] px-4 py-2.5"
                          >
                            {processando ? (
                              <ActivityIndicator size="small" color="#fff" />
                            ) : (
                              <>
                                <Ionicons name="checkmark-outline" size={16} color="#fff" />
                                <Text className="text-xs font-semibold text-white">
                                  Aceitar demanda
                                </Text>
                              </>
                            )}
                          </Pressable>
                        </View>
                      </>
                    ) : demanda.situacao === "ACEITA" && liberada ? (
                      <>
                        <Text className="text-xs font-medium text-[#744127]">
                          Pronta para envio do arquivo final desta etapa.
                        </Text>
                        <Pressable
                          disabled={processando}
                          onPress={() => void handleSelecionarEEntregar(demanda)}
                          className="flex-row items-center gap-2 rounded-xl bg-[#8c5230] px-4 py-2.5"
                        >
                          {processando ? (
                            <>
                              <ActivityIndicator size="small" color="#fff" />
                              {progresso !== null ? (
                                <Text className="text-xs font-semibold text-white">
                                  Enviando {progresso}%
                                </Text>
                              ) : null}
                            </>
                          ) : (
                            <>
                              <Ionicons name="cloud-upload-outline" size={16} color="#fff" />
                              <Text className="text-xs font-semibold text-white">
                                Entregar arquivo
                              </Text>
                            </>
                          )}
                        </Pressable>
                      </>
                    ) : (
                      <View className="flex-row items-center gap-2">
                        <Ionicons name="lock-closed-outline" size={16} color="#71717a" />
                        <Text className="text-xs text-neutral-500">
                          Entrega liberada automaticamente assim que a etapa anterior for entregue.
                        </Text>
                      </View>
                    )}
                  </View>
                </View>
              );
            })}
          </View>
        )}
      </View>
    </ScrollView>
  );
}