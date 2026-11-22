import React, { useCallback, useEffect, useState } from "react";
import {
  ActivityIndicator,
  Modal,
  Pressable,
  Text,
  TextInput,
  View,
} from "react-native";
import { Ionicons } from "@expo/vector-icons";
import { useSession } from "../../lib/session";
import { getApiErrorMessage } from "../../lib/api";
import {
  aprovarCertificacao,
  baixarArquivoCertificacaoGestor,
  listarCertificacoesGestor,
  rejeitarCertificacao,
  type CertificacaoPaginaResponse,
  type CertificacaoResponse,
  type StatusCertificacao,
} from "./certificacoesApi";

type FiltroStatusFila = "PENDENTE" | "APROVADA" | "REJEITADA" | "TODAS";

function formatarDataHora(valor: string | null): string {
  if (!valor) return "—";
  const data = new Date(valor);
  if (Number.isNaN(data.getTime())) return valor;
  return new Intl.DateTimeFormat("pt-BR", {
    dateStyle: "short",
    timeStyle: "short",
  }).format(data);
}

interface FilaCertificacoesProps {
  onCertificacaoAnalisada?: () => void;
}

export function FilaCertificacoes({ onCertificacaoAnalisada }: FilaCertificacoesProps) {
  const { usuario } = useSession();
  const podeAnalisar = usuario?.role === "ADMIN" || usuario?.role === "GESTOR_PROJETO";

  const [filtro, setFiltro] = useState<FiltroStatusFila>("PENDENTE");
  const [pagina, setPagina] = useState(1);
  const [dados, setDados] = useState<CertificacaoPaginaResponse | null>(null);
  const [carregando, setCarregando] = useState(true);
  const [erro, setErro] = useState("");
  const [mensagemSucesso, setMensagemSucesso] = useState("");

  const [analisandoId, setAnalisandoId] = useState<string | null>(null);
  const [baixandoId, setBaixandoId] = useState<string | null>(null);

  // Modal de Rejeição
  const [certificacaoParaRejeitar, setCertificacaoParaRejeitar] = useState<CertificacaoResponse | null>(null);
  const [motivoRejeicao, setMotivoRejeicao] = useState("");
  const [erroRejeicao, setErroRejeicao] = useState("");
  const [enviandoRejeicao, setEnviandoRejeicao] = useState(false);

  const carregarFila = useCallback(async (p: number, f: FiltroStatusFila) => {
    setCarregando(true);
    setErro("");
    try {
      const statusParam = f === "TODAS" ? undefined : (f as StatusCertificacao);
      const res = await listarCertificacoesGestor({
        status: statusParam,
        pagina: p,
        tamanho_pagina: 10,
      });
      setDados(res);
    } catch (e) {
      setErro(getApiErrorMessage(e));
    } finally {
      setCarregando(false);
    }
  }, []);

  useEffect(() => {
    if (!podeAnalisar) return;
    void carregarFila(pagina, filtro);
  }, [carregarFila, filtro, pagina, podeAnalisar]);

  if (!podeAnalisar) {
    return null;
  }

  async function handleAprovar(cert: CertificacaoResponse) {
    setAnalisandoId(cert.id);
    setMensagemSucesso("");
    setErro("");
    try {
      await aprovarCertificacao(cert.id);
      setMensagemSucesso(
        `Certificação de ${cert.recurso_nome || "recurso"} (${cert.idioma_origem.nome} → ${cert.idioma_destino.nome}) aprovada com sucesso!`
      );
      await carregarFila(pagina, filtro);
      onCertificacaoAnalisada?.();
    } catch (e) {
      setErro(getApiErrorMessage(e));
    } finally {
      setAnalisandoId(null);
    }
  }

  function handleAbrirRejeicao(cert: CertificacaoResponse) {
    setCertificacaoParaRejeitar(cert);
    setMotivoRejeicao("");
    setErroRejeicao("");
  }

  async function handleConfirmarRejeicao() {
    if (!certificacaoParaRejeitar) return;
    const motivoTrim = motivoRejeicao.trim();
    if (!motivoTrim) {
      setErroRejeicao("Informe o motivo da rejeição.");
      return;
    }

    setEnviandoRejeicao(true);
    setErroRejeicao("");
    try {
      await rejeitarCertificacao(certificacaoParaRejeitar.id, motivoTrim);
      setMensagemSucesso(
        `Certificação de ${certificacaoParaRejeitar.recurso_nome || "recurso"} rejeitada. O profissional foi notificado.`
      );
      setCertificacaoParaRejeitar(null);
      await carregarFila(pagina, filtro);
      onCertificacaoAnalisada?.();
    } catch (e) {
      setErroRejeicao(getApiErrorMessage(e));
    } finally {
      setEnviandoRejeicao(false);
    }
  }

  async function handleBaixar(cert: CertificacaoResponse) {
    setBaixandoId(cert.id);
    try {
      await baixarArquivoCertificacaoGestor(cert.id, cert.arquivo_nome);
    } catch (e) {
      setErro(getApiErrorMessage(e));
    } finally {
      setBaixandoId(null);
    }
  }

  const certificacoes = dados?.certificacoes ?? [];
  const totalRegistros = dados?.total_registros ?? 0;
  const totalPaginas = dados?.total_paginas ?? 1;

  return (
    <View className="w-full gap-4 rounded-2xl border border-neutral-200 bg-white p-5 shadow-sm md:p-6">
      {/* Top Header */}
      <View className="flex-row flex-wrap items-start justify-between gap-3 border-b border-neutral-100 pb-4">
        <View className="flex-1 gap-1 min-w-[240px]">
          <View className="flex-row items-center gap-2">
            <View className="h-8 w-8 items-center justify-center rounded-lg bg-[#6f4f28]/10">
              <Ionicons name="ribbon-outline" size={18} color="#6f4f28" />
            </View>
            <Text className="text-lg font-bold text-neutral-900">Fila de certificações</Text>
            {filtro === "PENDENTE" && totalRegistros > 0 ? (
              <View className="rounded-full bg-amber-100 px-2.5 py-0.5">
                <Text className="text-xs font-semibold text-amber-800">
                  {totalRegistros} {totalRegistros === 1 ? "pendente" : "pendentes"}
                </Text>
              </View>
            ) : null}
          </View>
          <Text className="text-xs text-neutral-500">
            Análise de documentos comprobatórios para liberação de alocação por par de idiomas
          </Text>
        </View>

        <Pressable
          accessibilityRole="button"
          accessibilityLabel="Recarregar fila de certificações"
          onPress={() => void carregarFila(pagina, filtro)}
          disabled={carregando}
          className="flex-row items-center gap-1.5 rounded-lg border border-neutral-200 px-3 py-1.5 hover:bg-neutral-50"
        >
          {carregando ? (
            <ActivityIndicator size="small" color="#6f4f28" />
          ) : (
            <Ionicons name="refresh-outline" size={16} color="#525252" />
          )}
          <Text className="text-xs font-medium text-neutral-700">Atualizar</Text>
        </Pressable>
      </View>

      {/* Alerts */}
      {mensagemSucesso ? (
        <View className="flex-row items-center justify-between rounded-xl border border-emerald-200 bg-emerald-50 p-3">
          <View className="flex-row items-center gap-2 flex-1">
            <Ionicons name="checkmark-circle" size={18} color="#059669" />
            <Text className="text-xs font-medium text-emerald-800">{mensagemSucesso}</Text>
          </View>
          <Pressable onPress={() => setMensagemSucesso("")}>
            <Ionicons name="close" size={16} color="#059669" />
          </Pressable>
        </View>
      ) : null}

      {erro ? (
        <View className="flex-row items-center justify-between rounded-xl border border-red-200 bg-red-50 p-3">
          <View className="flex-row items-center gap-2 flex-1">
            <Ionicons name="alert-circle" size={18} color="#dc2626" />
            <Text className="text-xs font-medium text-red-700">{erro}</Text>
          </View>
          <Pressable onPress={() => setErro("")}>
            <Ionicons name="close" size={16} color="#dc2626" />
          </Pressable>
        </View>
      ) : null}

      {/* Filter Tabs */}
      <View className="flex-row flex-wrap gap-1">
        {(
          [
            { id: "PENDENTE", label: "Pendentes de análise" },
            { id: "APROVADA", label: "Aprovadas" },
            { id: "REJEITADA", label: "Rejeitadas" },
            { id: "TODAS", label: "Todas" },
          ] as { id: FiltroStatusFila; label: string }[]
        ).map((opcao) => {
          const selecionado = filtro === opcao.id;
          return (
            <Pressable
              key={opcao.id}
              accessibilityRole="button"
              accessibilityState={{ selected: selecionado }}
              onPress={() => {
                setFiltro(opcao.id);
                setPagina(1);
              }}
              className={`rounded-lg px-3 py-1.5 ${
                selecionado ? "bg-[#6f4f28] shadow-sm" : "bg-neutral-100 hover:bg-neutral-200"
              }`}
            >
              <Text
                className={`text-xs font-semibold ${
                  selecionado ? "text-white" : "text-neutral-600"
                }`}
              >
                {opcao.label}
              </Text>
            </Pressable>
          );
        })}
      </View>

      {/* Content */}
      {carregando && !dados ? (
        <View className="items-center justify-center gap-2 py-8">
          <ActivityIndicator color="#6f4f28" />
          <Text className="text-xs text-neutral-500">Carregando fila de certificações...</Text>
        </View>
      ) : certificacoes.length === 0 ? (
        <View className="items-center justify-center gap-2 rounded-xl border border-dashed border-neutral-200 bg-neutral-50/50 py-10">
          <Ionicons name="checkmark-done-circle-outline" size={32} color="#a3a3a3" />
          <Text className="text-sm font-semibold text-neutral-800">
            {filtro === "PENDENTE"
              ? "Nenhuma certificação pendente de análise"
              : "Nenhuma certificação encontrada neste filtro"}
          </Text>
          <Text className="text-xs text-neutral-500 max-w-sm text-center">
            {filtro === "PENDENTE"
              ? "Todas as comprovações enviadas pelos profissionais já foram revisadas."
              : "Altere o filtro acima para consultar outros registros."}
          </Text>
        </View>
      ) : (
        <View className="gap-3">
          {certificacoes.map((cert) => {
            const isPendente = cert.status === "PENDENTE";
            const isAprovada = cert.status === "APROVADA";
            const isRejeitada = cert.status === "REJEITADA";
            const isProcessando = analisandoId === cert.id;
            const isBaixando = baixandoId === cert.id;

            return (
              <View
                key={cert.id}
                className="gap-3 rounded-xl border border-neutral-200 bg-white p-4 shadow-2xs hover:border-neutral-300"
              >
                {/* Header row: Recurso name, Language pair, Status */}
                <View className="flex-row flex-wrap items-center justify-between gap-2">
                  <View className="flex-row flex-wrap items-center gap-2">
                    <Text className="text-sm font-bold text-neutral-900">
                      {cert.recurso_nome || "Profissional"}
                    </Text>
                    <View className="flex-row items-center gap-1 rounded-md bg-neutral-100 px-2 py-0.5">
                      <Text className="text-xs font-semibold text-neutral-800">
                        {cert.idioma_origem.nome}
                      </Text>
                      <Ionicons name="arrow-forward" size={12} color="#737373" />
                      <Text className="text-xs font-semibold text-neutral-800">
                        {cert.idioma_destino.nome}
                      </Text>
                    </View>
                  </View>

                  {/* Status Badge */}
                  <View
                    className={`rounded-full border px-2.5 py-0.5 ${
                      isAprovada
                        ? "border-emerald-200 bg-emerald-50 text-emerald-800"
                        : isRejeitada
                        ? "border-red-200 bg-red-50 text-red-800"
                        : "border-amber-200 bg-amber-50 text-amber-800"
                    }`}
                  >
                    <Text
                      className={`text-xs font-semibold ${
                        isAprovada
                          ? "text-emerald-800"
                          : isRejeitada
                          ? "text-red-800"
                          : "text-amber-800"
                      }`}
                    >
                      {isAprovada
                        ? "Aprovada"
                        : isRejeitada
                        ? "Rejeitada"
                        : "Pendente de análise"}
                    </Text>
                  </View>
                </View>

                {/* Details row: Document file, sent date */}
                <View className="flex-row flex-wrap items-center justify-between gap-3 border-t border-neutral-100 pt-3">
                  <View className="flex-row flex-wrap items-center gap-3">
                    <View className="flex-row items-center gap-1.5">
                      <Ionicons name="document-text-outline" size={16} color="#6f4f28" />
                      <Text className="text-xs font-medium text-neutral-800" numberOfLines={1}>
                        {cert.arquivo_nome}
                      </Text>
                    </View>

                    <Text className="text-xs text-neutral-400">·</Text>

                    <Text className="text-xs text-neutral-500">
                      Enviado em {formatarDataHora(cert.enviado_em)}
                    </Text>

                    {cert.analisado_em ? (
                      <>
                        <Text className="text-xs text-neutral-400">·</Text>
                        <Text className="text-xs text-neutral-500">
                          Analisado em {formatarDataHora(cert.analisado_em)}
                        </Text>
                      </>
                    ) : null}
                  </View>

                  {/* Document Download Button */}
                  <Pressable
                    accessibilityRole="button"
                    accessibilityLabel={`Baixar documento ${cert.arquivo_nome}`}
                    onPress={() => void handleBaixar(cert)}
                    disabled={isBaixando}
                    className="flex-row items-center gap-1 rounded-lg border border-neutral-200 bg-neutral-50 px-2.5 py-1 hover:bg-neutral-100"
                  >
                    {isBaixando ? (
                      <ActivityIndicator size="small" color="#6f4f28" />
                    ) : (
                      <Ionicons name="download-outline" size={14} color="#6f4f28" />
                    )}
                    <Text className="text-xs font-medium text-neutral-700">Baixar documento</Text>
                  </Pressable>
                </View>

                {/* Rejection reason box if rejected */}
                {cert.motivo_rejeicao ? (
                  <View className="rounded-xl border border-red-200 bg-red-50 p-2.5 flex-row gap-2">
                    <Ionicons name="alert-circle-outline" size={16} color="#b91c1c" />
                    <View className="flex-1">
                      <Text className="text-xs font-semibold text-red-900">Motivo da rejeição:</Text>
                      <Text className="text-xs text-red-800 mt-0.5">{cert.motivo_rejeicao}</Text>
                    </View>
                  </View>
                ) : null}

                {/* Analysis Actions (only for PENDENTE) */}
                {isPendente ? (
                  <View className="flex-row justify-end items-center gap-2 border-t border-neutral-100 pt-2.5">
                    <Pressable
                      accessibilityRole="button"
                      accessibilityLabel={`Rejeitar certificação de ${cert.recurso_nome || "recurso"}`}
                      onPress={() => handleAbrirRejeicao(cert)}
                      disabled={isProcessando}
                      className="flex-row items-center gap-1 rounded-lg border border-red-200 bg-red-50 px-3 py-1.5 hover:bg-red-100"
                    >
                      <Ionicons name="close-circle-outline" size={15} color="#b91c1c" />
                      <Text className="text-xs font-semibold text-red-700">Rejeitar...</Text>
                    </Pressable>

                    <Pressable
                      accessibilityRole="button"
                      accessibilityLabel={`Aprovar certificação de ${cert.recurso_nome || "recurso"}`}
                      onPress={() => void handleAprovar(cert)}
                      disabled={isProcessando}
                      className="flex-row items-center gap-1 rounded-lg bg-emerald-700 px-3.5 py-1.5 hover:bg-emerald-800"
                    >
                      {isProcessando ? (
                        <ActivityIndicator size="small" color="#ffffff" />
                      ) : (
                        <Ionicons name="checkmark-circle-outline" size={15} color="#ffffff" />
                      )}
                      <Text className="text-xs font-semibold text-white">Aprovar certificação</Text>
                    </Pressable>
                  </View>
                ) : null}
              </View>
            );
          })}
        </View>
      )}

      {/* Pagination Footer */}
      {totalPaginas > 1 ? (
        <View className="flex-row items-center justify-between border-t border-neutral-100 pt-3">
          <Text className="text-xs text-neutral-500">
            Página {pagina} de {totalPaginas} ({totalRegistros} itens)
          </Text>
          <View className="flex-row gap-2">
            <Pressable
              accessibilityRole="button"
              accessibilityLabel="Página anterior"
              disabled={pagina <= 1 || carregando}
              onPress={() => setPagina((p) => Math.max(1, p - 1))}
              className={`rounded-lg border px-3 py-1 ${
                pagina <= 1 ? "border-neutral-200 opacity-50" : "border-neutral-300 hover:bg-neutral-50"
              }`}
            >
              <Text className="text-xs font-medium text-neutral-700">Anterior</Text>
            </Pressable>
            <Pressable
              accessibilityRole="button"
              accessibilityLabel="Próxima página"
              disabled={pagina >= totalPaginas || carregando}
              onPress={() => setPagina((p) => Math.min(totalPaginas, p + 1))}
              className={`rounded-lg border px-3 py-1 ${
                pagina >= totalPaginas
                  ? "border-neutral-200 opacity-50"
                  : "border-neutral-300 hover:bg-neutral-50"
              }`}
            >
              <Text className="text-xs font-medium text-neutral-700">Próxima</Text>
            </Pressable>
          </View>
        </View>
      ) : null}

      {/* Modal de Rejeição */}
      <Modal
        visible={certificacaoParaRejeitar !== null}
        transparent
        animationType="fade"
        onRequestClose={() => setCertificacaoParaRejeitar(null)}
      >
        <View className="flex-1 items-center justify-center bg-black/40 px-4">
          <View className="w-full max-w-md gap-4 rounded-2xl bg-white p-6 shadow-xl">
            <View className="flex-row items-center justify-between border-b border-neutral-100 pb-3">
              <View className="flex-row items-center gap-2">
                <View className="h-8 w-8 items-center justify-center rounded-lg bg-red-100">
                  <Ionicons name="alert-circle-outline" size={18} color="#b91c1c" />
                </View>
                <Text className="text-base font-bold text-neutral-900">Rejeitar certificação</Text>
              </View>
              <Pressable
                onPress={() => setCertificacaoParaRejeitar(null)}
                className="h-8 w-8 items-center justify-center rounded-lg border border-neutral-200"
              >
                <Ionicons name="close" size={16} color="#737373" />
              </Pressable>
            </View>

            {certificacaoParaRejeitar ? (
              <View className="rounded-xl bg-neutral-50 p-3 gap-1">
                <Text className="text-xs font-bold text-neutral-800">
                  {certificacaoParaRejeitar.recurso_nome || "Profissional"}
                </Text>
                <Text className="text-xs text-neutral-600">
                  Par: {certificacaoParaRejeitar.idioma_origem.nome} → {certificacaoParaRejeitar.idioma_destino.nome}
                </Text>
                <Text className="text-xs text-neutral-500">
                  Arquivo: {certificacaoParaRejeitar.arquivo_nome}
                </Text>
              </View>
            ) : null}

            {erroRejeicao ? (
              <View className="rounded-lg border border-red-200 bg-red-50 p-2.5">
                <Text className="text-xs text-red-700">{erroRejeicao}</Text>
              </View>
            ) : null}

            <View className="gap-1.5">
              <Text className="text-xs font-semibold text-neutral-700">
                Motivo da rejeição (obrigatório) *
              </Text>
              <TextInput
                value={motivoRejeicao}
                onChangeText={setMotivoRejeicao}
                maxLength={1000}
                placeholder="Ex.: Documento ilegível, certificado expirado ou par divergente..."
                placeholderTextColor="#a3a3a3"
                multiline
                numberOfLines={3}
                className="min-h-[80px] rounded-xl border border-neutral-300 p-3 text-xs text-neutral-900 align-top"
              />
              <Text className="text-[11px] text-neutral-500">
                O profissional receberá este motivo por e-mail e poderá reenviar a certificação pelo portal.
              </Text>
            </View>

            <View className="flex-row justify-end gap-2.5 pt-2 border-t border-neutral-100">
              <Pressable
                onPress={() => setCertificacaoParaRejeitar(null)}
                disabled={enviandoRejeicao}
                className="rounded-lg border border-neutral-300 px-4 py-2"
              >
                <Text className="text-xs font-medium text-neutral-700">Cancelar</Text>
              </Pressable>
              <Pressable
                onPress={() => void handleConfirmarRejeicao()}
                disabled={enviandoRejeicao || !motivoRejeicao.trim()}
                className={`flex-row items-center gap-1.5 rounded-lg px-4 py-2 ${
                  enviandoRejeicao || !motivoRejeicao.trim() ? "bg-neutral-300" : "bg-red-700"
                }`}
              >
                {enviandoRejeicao ? (
                  <ActivityIndicator size="small" color="#ffffff" />
                ) : (
                  <Ionicons name="close-circle-outline" size={15} color="#ffffff" />
                )}
                <Text className="text-xs font-semibold text-white">Confirmar rejeição</Text>
              </Pressable>
            </View>
          </View>
        </View>
      </Modal>
    </View>
  );
}
