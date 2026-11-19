import { useState } from "react";
import { Ionicons } from "@expo/vector-icons";
import { useQuery } from "@tanstack/react-query";
import {
  ActivityIndicator, Pressable, ScrollView, Text, useWindowDimensions, View,
} from "react-native";
import { CabecalhoTela } from "../navegacao/CabecalhoTela";
import { getApiErrorMessage } from "../../lib/api";
import { useSession } from "../../lib/session";
import { listarLogsAuditoria } from "../../lib/auditoriaService";
import type { LogAuditoria } from "./types";
import { AuditoriaDetalhesModal } from "./AuditoriaDetalhesModal";
import { formatarDataHora, formatarStatus, iniciais, NOME_ENTIDADE, NOME_ORIGEM } from "./formatacao";

const TAMANHO_PAGINA = 20;

function StatusAlterado({ log }: { log: LogAuditoria }) {
  return (
    <View className="flex-row flex-wrap items-center gap-1.5">
      <Text className="text-xs text-[#83766a]">{formatarStatus(log.status_anterior)}</Text>
      <Ionicons name="arrow-forward" size={13} color="#94877b" />
      <Text className="text-xs font-semibold text-[#78502f]">{formatarStatus(log.status_novo)}</Text>
    </View>
  );
}

function LinhaCompacta({ log, abrir }: { log: LogAuditoria; abrir: () => void }) {
  return (
    <Pressable
      accessibilityRole="button"
      accessibilityLabel={`Ver detalhes de ${log.referencia_legivel}`}
      onPress={abrir}
      className="flex-row items-center gap-3 border-b border-[#f0e9e2] px-4 py-4 active:bg-[#faf5f0] md:px-6"
    >
      <View className="h-10 w-10 items-center justify-center rounded-full bg-[#93502e]">
        <Text className="text-xs font-bold text-white">{iniciais(log.usuario.nome)}</Text>
      </View>
      <View className="min-w-0 flex-1 gap-1.5">
        <Text className="text-sm text-[#504037]">
          <Text className="font-bold text-[#2b211a]">{log.usuario.nome} </Text>
          alterou o status de {NOME_ENTIDADE[log.entidade].toLocaleLowerCase("pt-BR")}
        </Text>
        <View className="flex-row flex-wrap items-center gap-2">
          <View className="rounded-md bg-[#f5eae1] px-2 py-0.5">
            <Text className="text-[10px] font-semibold text-[#95512e]">{NOME_ENTIDADE[log.entidade]}</Text>
          </View>
          <Text numberOfLines={1} className="max-w-full text-xs text-[#87786b]">{log.referencia_legivel || log.entidade_id}</Text>
        </View>
        <StatusAlterado log={log} />
        <Text className="text-[11px] text-[#948578]">{formatarDataHora(log.data_hora)} · {NOME_ORIGEM[log.origem_acao]}</Text>
        {log.observacao ? (
          <Text numberOfLines={1} className="text-xs text-[#83776d]">{log.observacao}</Text>
        ) : null}
      </View>
      <Ionicons name="chevron-forward" size={18} color="#a66742" />
    </Pressable>
  );
}

function LinhaTabela({ log, abrir }: { log: LogAuditoria; abrir: () => void }) {
  return (
    <Pressable
      accessibilityRole="button"
      accessibilityLabel={`Ver detalhes de ${log.referencia_legivel}`}
      onPress={abrir}
      className="flex-row items-center gap-3 border-b border-[#efe8e1] px-4 py-3 active:bg-[#faf5f0]"
    >
      <Text className="w-28 text-xs text-[#786d64]">{formatarDataHora(log.data_hora)}</Text>
      <Text className="w-28 text-xs font-semibold text-[#8e502c]">{NOME_ENTIDADE[log.entidade]}</Text>
      <Text numberOfLines={2} className="min-w-0 flex-1 text-xs text-[#46382e]">{log.referencia_legivel || log.entidade_id}</Text>
      <View className="w-44"><StatusAlterado log={log} /></View>
      <Text numberOfLines={2} className="w-28 text-xs text-[#51443a]">{log.usuario.nome}</Text>
      <Text className="w-20 text-xs text-[#79695b]">{NOME_ORIGEM[log.origem_acao]}</Text>
      <Text numberOfLines={2} className="min-w-0 flex-1 text-xs text-[#87786b]">{log.observacao || "—"}</Text>
      <Ionicons name="chevron-forward" size={16} color="#a66742" />
    </Pressable>
  );
}

export function AuditoriaScreen() {
  const { usuario } = useSession();
  const { width } = useWindowDimensions();
  const [pagina, setPagina] = useState(1);
  const [selecionado, setSelecionado] = useState<LogAuditoria | null>(null);
  const tabelaAmpla = width >= 1440;
  const ehAdministrador = usuario?.role === "ADMIN";

  const consulta = useQuery({
    queryKey: ["auditoria", "logs", usuario?.id, pagina],
    queryFn: () => listarLogsAuditoria({ page: pagina, size: TAMANHO_PAGINA }),
    enabled: ehAdministrador,
    retry: 1,
    staleTime: 0,
  });

  const logs = consulta.data?.logs ?? [];
  const total = consulta.data?.total_registros ?? 0;
  const totalPaginas = consulta.data?.total_paginas ?? 0;
  const de = total ? (pagina - 1) * TAMANHO_PAGINA + 1 : 0;
  const ate = Math.min(pagina * TAMANHO_PAGINA, total);

  return (
    <ScrollView className="flex-1 bg-[#fbfaf8]" contentContainerStyle={{ flexGrow: 1, paddingBottom: 48 }}>
      <View className="mx-auto w-full max-w-7xl px-4 py-6 md:px-8">
        <CabecalhoTela
          contexto="SISTEMA"
          secao="Logs de alterações"
          titulo="Logs de alterações"
          descricao="Acompanhe as alterações de status realizadas nos orçamentos, ordens de serviço e etapas."
        />

        {!ehAdministrador ? (
          <View className="rounded-xl border border-[#edddd2] bg-white p-6">
            <Text className="font-semibold text-[#70482d]">Acesso exclusivo do administrador.</Text>
          </View>
        ) : (
          <View className="overflow-hidden rounded-xl border border-[#e8ddd3] bg-white">
            <View className="flex-row flex-wrap items-center justify-between gap-3 border-b border-[#ece3da] px-5 py-4">
              <View className="gap-1">
                <Text className="font-serif text-lg font-bold text-[#302219]">Histórico de atividades</Text>
                <Text className="text-xs text-[#988478]">
                  {consulta.isLoading ? "Buscando registros..." : `${total} registro${total === 1 ? "" : "s"} encontrado${total === 1 ? "" : "s"}`}
                </Text>
              </View>
              <Pressable
                accessibilityRole="button"
                accessibilityLabel="Atualizar logs de auditoria"
                onPress={() => { void consulta.refetch(); }}
                disabled={consulta.isFetching}
                className="flex-row items-center gap-2 rounded-lg border border-[#e8dcd1] bg-white px-3 py-2"
              >
                <Ionicons name="refresh-outline" size={16} color="#86502d" />
                <Text className="text-xs font-semibold text-[#86502d]">{consulta.isFetching ? "Atualizando..." : "Atualizar"}</Text>
              </Pressable>
            </View>

            {consulta.isLoading ? (
              <View className="items-center gap-3 px-5 py-14">
                <ActivityIndicator size="large" color="#91502b" />
                <Text className="text-sm text-[#837367]">Carregando histórico de alterações...</Text>
              </View>
            ) : consulta.isError ? (
              <View className="items-center gap-3 px-5 py-12">
                <Ionicons name="alert-circle-outline" size={28} color="#ad5538" />
                <Text className="text-center text-sm text-[#8e4633]">{getApiErrorMessage(consulta.error)}</Text>
                <Pressable onPress={() => { void consulta.refetch(); }} className="rounded-lg bg-[#84502c] px-4 py-2.5">
                  <Text className="text-xs font-bold text-white">Tentar novamente</Text>
                </Pressable>
              </View>
            ) : logs.length === 0 ? (
              <View className="items-center gap-3 px-5 py-14">
                <Ionicons name="receipt-outline" size={30} color="#a18e7f" />
                <Text className="text-center font-semibold text-[#544439]">Nenhum registro de auditoria encontrado.</Text>
                <Text className="text-center text-xs text-[#948275]">As alterações registradas aparecerão aqui.</Text>
              </View>
            ) : (
              <>
                {tabelaAmpla ? (
                  <View className="flex-row items-center gap-3 border-b border-[#ede4db] bg-[#faf7f3] px-4 py-3">
                    <Text className="w-28 text-[11px] font-bold text-[#755e4f]">Data e hora</Text>
                    <Text className="w-28 text-[11px] font-bold text-[#755e4f]">Entidade</Text>
                    <Text className="min-w-0 flex-1 text-[11px] font-bold text-[#755e4f]">Referência</Text>
                    <Text className="w-44 text-[11px] font-bold text-[#755e4f]">Status anterior → novo</Text>
                    <Text className="w-28 text-[11px] font-bold text-[#755e4f]">Usuário</Text>
                    <Text className="w-20 text-[11px] font-bold text-[#755e4f]">Origem</Text>
                    <Text className="min-w-0 flex-1 text-[11px] font-bold text-[#755e4f]">Observação</Text>
                    <View style={{ width: 16 }} />
                  </View>
                ) : null}
                {logs.map((log) => tabelaAmpla ? (
                  <LinhaTabela key={log.id} log={log} abrir={() => setSelecionado(log)} />
                ) : (
                  <LinhaCompacta key={log.id} log={log} abrir={() => setSelecionado(log)} />
                ))}
              </>
            )}

            {totalPaginas > 0 ? (
              <View className="flex-row flex-wrap items-center justify-between gap-3 px-5 py-4">
                <Text className="text-xs text-[#8d7a6c]">Exibindo {de}–{ate} de {total}</Text>
                <View className="flex-row items-center gap-3">
                  <Pressable
                    onPress={() => setPagina(atual => Math.max(1, atual - 1))}
                    disabled={pagina === 1 || consulta.isFetching}
                    accessibilityRole="button"
                    accessibilityLabel="Página anterior"
                    style={{ opacity: pagina === 1 || consulta.isFetching ? 0.4 : 1 }}
                    className="rounded-lg border border-[#e7dcd0] px-3 py-2"
                  >
                    <Text className="text-xs font-semibold text-[#80502e]">Anterior</Text>
                  </Pressable>
                  <Text className="text-xs font-semibold text-[#6e5b4d]">{pagina} / {totalPaginas}</Text>
                  <Pressable
                    onPress={() => setPagina(atual => Math.min(totalPaginas, atual + 1))}
                    disabled={pagina >= totalPaginas || consulta.isFetching}
                    accessibilityRole="button"
                    accessibilityLabel="Próxima página"
                    style={{ opacity: pagina >= totalPaginas || consulta.isFetching ? 0.4 : 1 }}
                    className="rounded-lg border border-[#e7dcd0] px-3 py-2"
                  >
                    <Text className="text-xs font-semibold text-[#80502e]">Próxima</Text>
                  </Pressable>
                </View>
              </View>
            ) : null}
          </View>
        )}
      </View>
      <AuditoriaDetalhesModal log={selecionado} aoFechar={() => setSelecionado(null)} />
    </ScrollView>
  );
}
