import React, { useCallback, useEffect, useMemo, useState } from "react";
import {
  ActivityIndicator,
  Pressable,
  ScrollView,
  Text,
  View,
} from "react-native";
import { Ionicons } from "@expo/vector-icons";
import { useRouter } from "expo-router";
import { getApiErrorMessage } from "../../../lib/api";
import {
  buscarMeuPerfilRecurso,
  type PerfilRecurso,
} from "../../../components/recursos/portalRecursoApi";
import {
  baixarMinhaCertificacao,
  listarMinhasCertificacoes,
  type CertificacaoResponse,
} from "../../../components/recursos/certificacoesApi";
import {
  ModalEnviarCertificacao,
  type ParIdiomaOpcao,
} from "../../../components/recursos/ModalEnviarCertificacao";

function formatarDataHora(valor: string | null): string {
  if (!valor) return "—";
  const data = new Date(valor);
  if (Number.isNaN(data.getTime())) return valor;
  return new Intl.DateTimeFormat("pt-BR", {
    dateStyle: "medium",
    timeStyle: "short",
  }).format(data);
}

interface ParIdiomaComCertificacao {
  chave: string;
  idiomaOrigem: { id: string; nome: string; codigoIso?: string; codigo_iso?: string };
  idiomaDestino: { id: string; nome: string; codigoIso?: string; codigo_iso?: string };
  certificacao?: CertificacaoResponse;
}

export default function MinhasCertificacoesScreen() {
  const router = useRouter();
  const [perfil, setPerfil] = useState<PerfilRecurso | null>(null);
  const [certificacoes, setCertificacoes] = useState<CertificacaoResponse[]>([]);
  const [carregando, setCarregando] = useState(true);
  const [erro, setErro] = useState("");
  const [baixandoId, setBaixandoId] = useState<string | null>(null);

  // Modal
  const [modalAberto, setModalAberto] = useState(false);
  const [parPreselecionado, setParPreselecionado] = useState<{
    idiomaOrigemId: string;
    idiomaDestinoId: string;
  } | null>(null);
  const [mensagemFeedback, setMensagemFeedback] = useState("");

  const carregarDados = useCallback(async () => {
    setCarregando(true);
    setErro("");
    try {
      const [dadosPerfil, dadosCerts] = await Promise.all([
        buscarMeuPerfilRecurso(),
        listarMinhasCertificacoes(),
      ]);
      setPerfil(dadosPerfil);
      setCertificacoes(dadosCerts);
    } catch (e) {
      setErro(getApiErrorMessage(e));
    } finally {
      setCarregando(false);
    }
  }, []);

  useEffect(() => {
    let ativo = true;
    Promise.all([buscarMeuPerfilRecurso(), listarMinhasCertificacoes()])
      .then(([dadosPerfil, dadosCerts]) => {
        if (!ativo) return;
        setPerfil(dadosPerfil);
        setCertificacoes(dadosCerts);
      })
      .catch((e) => {
        if (!ativo) return;
        setErro(getApiErrorMessage(e));
      })
      .finally(() => {
        if (ativo) setCarregando(false);
      });

    return () => {
      ativo = false;
    };
  }, []);

  async function handleBaixar(cert: CertificacaoResponse) {
    setBaixandoId(cert.id);
    try {
      await baixarMinhaCertificacao(cert.id, cert.arquivo_nome);
    } catch (e) {
      setErro(getApiErrorMessage(e));
    } finally {
      setBaixandoId(null);
    }
  }

  function abrirModalParaPar(idiomaOrigemId?: string, idiomaDestinoId?: string) {
    if (idiomaOrigemId && idiomaDestinoId) {
      setParPreselecionado({ idiomaOrigemId, idiomaDestinoId });
    } else {
      setParPreselecionado(null);
    }
    setModalAberto(true);
  }

  // Agrupa os pares de idiomas atendidos de forma única para evitar duplicação por modalidade/unidade de cobrança
  const paresUnicos = useMemo<ParIdiomaComCertificacao[]>(() => {
    const mapa = new Map<string, ParIdiomaComCertificacao>();

    for (const preco of perfil?.precos ?? []) {
      const chave = `${preco.idiomaOrigem.id}:::${preco.idiomaDestino.id}`;
      if (!mapa.has(chave)) {
        const certExistente = certificacoes.find(
          (c) =>
            c.idioma_origem.id === preco.idiomaOrigem.id &&
            c.idioma_destino.id === preco.idiomaDestino.id
        );
        mapa.set(chave, {
          chave,
          idiomaOrigem: preco.idiomaOrigem,
          idiomaDestino: preco.idiomaDestino,
          certificacao: certExistente,
        });
      }
    }

    // Se houver alguma certificação enviada que não esteja nos preços atuais, garante inclusão única
    for (const cert of certificacoes) {
      const chave = `${cert.idioma_origem.id}:::${cert.idioma_destino.id}`;
      if (!mapa.has(chave)) {
        mapa.set(chave, {
          chave,
          idiomaOrigem: cert.idioma_origem,
          idiomaDestino: cert.idioma_destino,
          certificacao: cert,
        });
      }
    }

    return Array.from(mapa.values());
  }, [perfil?.precos, certificacoes]);

  // Prepara pares disponíveis sem duplicidades para o modal de seleção
  const paresDisponiveis: ParIdiomaOpcao[] = useMemo(() => {
    return paresUnicos.map((p) => ({
      idiomaOrigem: p.idiomaOrigem,
      idiomaDestino: p.idiomaDestino,
      statusCertificacao: p.certificacao?.status,
    }));
  }, [paresUnicos]);

  const totalAprovadas = paresUnicos.filter((p) => p.certificacao?.status === "APROVADA").length;
  const totalPendentes = paresUnicos.filter((p) => p.certificacao?.status === "PENDENTE").length;
  const totalRejeitadas = paresUnicos.filter((p) => p.certificacao?.status === "REJEITADA").length;

  return (
    <ScrollView className="flex-1" contentContainerClassName="items-center p-5 py-8 md:p-8">
      <View className="w-full max-w-5xl gap-6">
        {/* Top Header */}
        <View className="flex-row flex-wrap items-start justify-between gap-4">
          <View className="flex-1 gap-1 min-w-65">
            <Text className="text-2xl font-bold text-neutral-900">Minhas certificações</Text>
            <Text className="text-sm text-neutral-600">
              Envie comprovações para cada par de idiomas que você atende para liberar alocações.
            </Text>
          </View>

          <Pressable
            accessibilityRole="button"
            accessibilityLabel="Enviar nova certificação"
            onPress={() => abrirModalParaPar()}
            className="flex-row items-center gap-2 rounded-xl bg-[#8c5230] px-4 py-2.5 shadow-xs"
          >
            <Ionicons name="add" size={18} color="#ffffff" />
            <Text className="font-semibold text-white">Enviar certificação</Text>
          </Pressable>
        </View>

        {/* Feedback message */}
        {mensagemFeedback ? (
          <View className="flex-row items-center justify-between rounded-xl border border-emerald-200 bg-emerald-50 p-4">
            <View className="flex-row items-center gap-2.5 flex-1">
              <Ionicons name="checkmark-circle" size={20} color="#059669" />
              <Text className="text-sm font-medium text-emerald-900">{mensagemFeedback}</Text>
            </View>
            <Pressable onPress={() => setMensagemFeedback("")}>
              <Ionicons name="close" size={18} color="#059669" />
            </Pressable>
          </View>
        ) : null}

        {/* Error message */}
        {erro ? (
          <View className="flex-row items-center justify-between rounded-xl border border-red-200 bg-red-50 p-4">
            <View className="flex-row items-center gap-2.5 flex-1">
              <Ionicons name="alert-circle" size={20} color="#dc2626" />
              <Text className="text-sm font-medium text-red-900">{erro}</Text>
            </View>
            <Pressable onPress={() => setErro("")}>
              <Ionicons name="close" size={18} color="#dc2626" />
            </Pressable>
          </View>
        ) : null}

        {/* Informational Guidance Banner */}
        <View className="gap-3 rounded-2xl border border-neutral-200 bg-white p-5 shadow-xs">
          <View className="flex-row items-center gap-3">
            <View className="h-10 w-10 items-center justify-center rounded-xl bg-[#f5ede7]">
              <Ionicons name="shield-checkmark-outline" size={22} color="#8c5230" />
            </View>
            <View className="flex-1">
              <Text className="text-base font-bold text-neutral-900">
                Certificação por par de idiomas
              </Text>
              <Text className="text-xs text-neutral-500">
                A elegibilidade para alocação direta e disparos de etapas exige certificação aprovada para o par da etapa.
              </Text>
            </View>
          </View>

          <View className="flex-row flex-wrap gap-3 border-t border-neutral-100 pt-3">
            <View className="flex-1 min-w-32.5 rounded-xl bg-emerald-50 p-3">
              <Text className="text-xs font-semibold text-emerald-800">Aprovadas</Text>
              <Text className="text-xl font-bold text-emerald-900">{totalAprovadas}</Text>
              <Text className="text-[11px] text-emerald-700">Pares liberados</Text>
            </View>

            <View className="flex-1 min-w-32.5 rounded-xl bg-amber-50 p-3">
              <Text className="text-xs font-semibold text-amber-800">Em análise</Text>
              <Text className="text-xl font-bold text-amber-900">{totalPendentes}</Text>
              <Text className="text-[11px] text-amber-700">Aguardando gestor</Text>
            </View>

            <View className="flex-1 min-w-32.5 rounded-xl bg-red-50 p-3">
              <Text className="text-xs font-semibold text-red-800">Rejeitadas</Text>
              <Text className="text-xl font-bold text-red-900">{totalRejeitadas}</Text>
              <Text className="text-[11px] text-red-700">Necessitam reenvio</Text>
            </View>
          </View>
        </View>

        {/* Content list */}
        {carregando ? (
          <View className="items-center justify-center gap-3 rounded-2xl border border-neutral-200 bg-white p-12">
            <ActivityIndicator color="#8c5230" />
            <Text className="text-sm text-neutral-500">Carregando certificações...</Text>
          </View>
        ) : paresUnicos.length === 0 ? (
          <View className="items-center gap-3 rounded-2xl border border-neutral-200 bg-white p-10">
            <View className="h-14 w-14 items-center justify-center rounded-full bg-neutral-100">
              <Ionicons name="language-outline" size={26} color="#8c5230" />
            </View>
            <Text className="text-base font-semibold text-neutral-900">
              Nenhum par de idiomas cadastrado
            </Text>
            <Text className="max-w-md text-center text-sm text-neutral-500">
              Para enviar uma certificação, adicione primeiro os pares de idiomas atendidos e seus valores no seu perfil.
            </Text>
            <Pressable
              accessibilityRole="button"
              onPress={() => router.replace("/portal/perfil" as never)}
              className="mt-2 rounded-xl bg-[#8c5230] px-4 py-2"
            >
              <Text className="text-sm font-semibold text-white">Ir para Meu Perfil</Text>
            </Pressable>
          </View>
        ) : (
          <View className="gap-4">
            <Text className="text-base font-bold text-neutral-900">
              Pares cadastrados e certificações
            </Text>

            {paresUnicos.map((par) => {
              const cert = par.certificacao;
              const status = cert?.status;
              const isAprovada = status === "APROVADA";
              const isPendente = status === "PENDENTE";
              const isRejeitada = status === "REJEITADA";
              const isSemCertificacao = !status;
              const isBaixando = cert && baixandoId === cert.id;

              return (
                <View
                  key={par.chave}
                  className="gap-4 rounded-2xl border border-neutral-200 bg-white p-5 shadow-xs"
                >
                  {/* Card Header: Pair and Status Badge */}
                  <View className="flex-row flex-wrap items-center justify-between gap-3">
                    <View className="flex-row items-center gap-2">
                      <View className="h-9 w-9 items-center justify-center rounded-xl bg-neutral-100">
                        <Ionicons name="language" size={18} color="#52525b" />
                      </View>
                      <View>
                        <Text className="text-base font-bold text-neutral-900">
                          {par.idiomaOrigem.nome} → {par.idiomaDestino.nome}
                        </Text>
                        <Text className="text-xs text-neutral-500">
                          Par de atuação cadastrado no seu perfil
                        </Text>
                      </View>
                    </View>

                    {/* Status Badge */}
                    <View
                      className={`flex-row items-center gap-1.5 rounded-full px-3 py-1 border ${
                        isAprovada
                          ? "border-emerald-200 bg-emerald-50 text-emerald-800"
                          : isPendente
                          ? "border-amber-200 bg-amber-50 text-amber-800"
                          : isRejeitada
                          ? "border-red-200 bg-red-50 text-red-800"
                          : "border-neutral-200 bg-neutral-100 text-neutral-700"
                      }`}
                    >
                      <Ionicons
                        name={
                          isAprovada
                            ? "checkmark-circle"
                            : isPendente
                            ? "time-outline"
                            : isRejeitada
                            ? "close-circle"
                            : "alert-circle-outline"
                        }
                        size={14}
                        color={
                          isAprovada
                            ? "#059669"
                            : isPendente
                            ? "#d97706"
                            : isRejeitada
                            ? "#dc2626"
                            : "#525252"
                        }
                      />
                      <Text
                        className={`text-xs font-semibold ${
                          isAprovada
                            ? "text-emerald-800"
                            : isPendente
                            ? "text-amber-800"
                            : isRejeitada
                            ? "text-red-800"
                            : "text-neutral-700"
                        }`}
                      >
                        {isAprovada
                          ? "Certificado aprovado"
                          : isPendente
                          ? "Em análise pelo gestor"
                          : isRejeitada
                          ? "Certificação recusada"
                          : "Sem certificação enviada"}
                      </Text>
                    </View>
                  </View>

                  {/* Document & Details */}
                  {cert ? (
                    <View className="gap-2.5 rounded-xl bg-neutral-50 p-4 border border-neutral-100">
                      <View className="flex-row flex-wrap items-center justify-between gap-3">
                        <View className="flex-row items-center gap-2 flex-1 min-w-50">
                          <Ionicons name="document-text-outline" size={18} color="#8c5230" />
                          <View className="flex-1">
                            <Text className="text-xs font-semibold text-neutral-900" numberOfLines={1}>
                              {cert.arquivo_nome}
                            </Text>
                            <Text className="text-[11px] text-neutral-500">
                              Enviado em {formatarDataHora(cert.enviado_em)}
                            </Text>
                          </View>
                        </View>

                        <Pressable
                          accessibilityRole="button"
                          accessibilityLabel={`Baixar meu documento ${cert.arquivo_nome}`}
                          onPress={() => void handleBaixar(cert)}
                          disabled={isBaixando}
                          className="flex-row items-center gap-1.5 rounded-lg border border-neutral-300 bg-white px-3 py-1.5 hover:bg-neutral-100"
                        >
                          {isBaixando ? (
                            <ActivityIndicator size="small" color="#8c5230" />
                          ) : (
                            <Ionicons name="download-outline" size={15} color="#8c5230" />
                          )}
                          <Text className="text-xs font-semibold text-neutral-700">
                            Baixar arquivo
                          </Text>
                        </Pressable>
                      </View>

                      {/* Motivo de Rejeição */}
                      {isRejeitada && cert.motivo_rejeicao ? (
                        <View className="mt-2 rounded-lg border border-red-200 bg-red-50 p-3">
                          <View className="flex-row items-center gap-1.5 mb-1">
                            <Ionicons name="alert-circle" size={15} color="#dc2626" />
                            <Text className="text-xs font-bold text-red-900">
                              Motivo informado pelo gestor:
                            </Text>
                          </View>
                          <Text className="text-xs text-red-800 leading-4">
                            {cert.motivo_rejeicao}
                          </Text>
                        </View>
                      ) : null}

                      {/* Info for Approved */}
                      {isAprovada ? (
                        <View className="mt-1 flex-row items-center gap-1.5">
                          <Ionicons name="checkmark" size={14} color="#059669" />
                          <Text className="text-xs text-emerald-800 font-medium">
                            Você está habilitado para receber demandas deste par de idiomas.
                          </Text>
                        </View>
                      ) : null}
                    </View>
                  ) : (
                    <View className="rounded-xl bg-neutral-50 p-4 border border-dashed border-neutral-300">
                      <Text className="text-xs text-neutral-600">
                        Nenhum documento comprobatório foi enviado para este par de idiomas.
                        Envie um certificado ou diploma para que o gestor possa analisar e liberar você para alocações.
                      </Text>
                    </View>
                  )}

                  {/* Actions Row */}
                  <View className="flex-row justify-end items-center gap-3 border-t border-neutral-100 pt-3">
                    {isSemCertificacao ? (
                      <Pressable
                        accessibilityRole="button"
                        onPress={() => abrirModalParaPar(par.idiomaOrigem.id, par.idiomaDestino.id)}
                        className="flex-row items-center gap-1.5 rounded-xl bg-[#8c5230] px-4 py-2 hover:bg-[#744127]"
                      >
                        <Ionicons name="cloud-upload-outline" size={16} color="#ffffff" />
                        <Text className="text-xs font-semibold text-white">
                          Enviar comprovação
                        </Text>
                      </Pressable>
                    ) : isRejeitada ? (
                      <Pressable
                        accessibilityRole="button"
                        onPress={() => abrirModalParaPar(par.idiomaOrigem.id, par.idiomaDestino.id)}
                        className="flex-row items-center gap-1.5 rounded-xl bg-[#8c5230] px-4 py-2 hover:bg-[#744127]"
                      >
                        <Ionicons name="refresh-outline" size={16} color="#ffffff" />
                        <Text className="text-xs font-semibold text-white">
                          Reenviar certificação
                        </Text>
                      </Pressable>
                    ) : isPendente ? (
                      <Pressable
                        accessibilityRole="button"
                        onPress={() => abrirModalParaPar(par.idiomaOrigem.id, par.idiomaDestino.id)}
                        className="flex-row items-center gap-1.5 rounded-xl border border-neutral-300 bg-white px-4 py-2 hover:bg-neutral-50"
                      >
                        <Ionicons name="swap-horizontal-outline" size={16} color="#71717a" />
                        <Text className="text-xs font-semibold text-neutral-700">
                          Substituir documento
                        </Text>
                      </Pressable>
                    ) : null}
                  </View>
                </View>
              );
            })}
          </View>
        )}
      </View>

      <ModalEnviarCertificacao
        visible={modalAberto}
        onClose={() => setModalAberto(false)}
        onSuccess={(cert) => {
          setMensagemFeedback(
            `Certificação para ${cert.idioma_origem.nome} → ${cert.idioma_destino.nome} enviada com sucesso! Ela está pendente de análise pelo Gestor.`
          );
          void carregarDados();
        }}
        pares={paresDisponiveis}
        parInicial={parPreselecionado}
      />
    </ScrollView>
  );
}
