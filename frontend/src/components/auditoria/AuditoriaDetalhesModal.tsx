import { Ionicons } from "@expo/vector-icons";
import { Modal, Pressable, ScrollView, Text, useWindowDimensions, View } from "react-native";
import type { LogAuditoria } from "./types";
import { formatarDataHora, formatarStatus, iniciais, NOME_ENTIDADE, NOME_ORIGEM } from "./formatacao";

interface Props {
  log: LogAuditoria | null;
  aoFechar: () => void;
}

function Metadado({ titulo, valor }: { titulo: string; valor: string }) {
  return (
    <View style={{ width: "47%", minWidth: 125, gap: 6 }}>
      <Text className="text-[10px] font-bold uppercase tracking-wider text-[#887c70]">{titulo}</Text>
      <Text selectable className="text-sm font-semibold text-[#382a20]">{valor}</Text>
    </View>
  );
}

export function AuditoriaDetalhesModal({ log, aoFechar }: Props) {
  const { width } = useWindowDimensions();
  return (
    <Modal
      visible={Boolean(log)}
      transparent
      animationType="fade"
      onRequestClose={aoFechar}
    >
      <View style={{ flex: 1, flexDirection: "row", justifyContent: "flex-end", backgroundColor: "#22181066" }}>
        <Pressable
          onPress={aoFechar}
          accessibilityLabel="Fechar detalhes da alteração"
          style={{ flex: 1 }}
        />
        <View style={{ width: Math.min(width, 470), backgroundColor: "#ffffff", height: "100%" }}>
          <ScrollView contentContainerStyle={{ paddingBottom: 40 }}>
            <View className="flex-row items-start justify-between border-b border-[#e9e0d7] p-6">
              <View className="flex-1 pr-3 gap-2">
                <Text className="text-[11px] font-bold tracking-wider text-[#9a502d]">REGISTRO DE ATIVIDADE</Text>
                <Text className="text-2xl font-serif font-bold text-[#2c211b]">Detalhes da alteração</Text>
              </View>
              <Pressable
                accessibilityRole="button"
                accessibilityLabel="Fechar"
                onPress={aoFechar}
                className="rounded-lg border border-[#e8ded4] p-2.5"
              >
                <Ionicons name="close" size={20} color="#854722" />
              </Pressable>
            </View>
            {log && (
              <View className="gap-6 p-6">
                <View className="flex-row items-center gap-3 rounded-xl border border-[#e9e0d7] bg-[#fcfaf8] p-4">
                  <View className="h-11 w-11 items-center justify-center rounded-full bg-[#95502b]">
                    <Text className="font-bold text-white">{iniciais(log.usuario.nome)}</Text>
                  </View>
                  <View className="flex-1 gap-1">
                    <Text className="font-bold text-[#33251e]">{log.usuario.nome}</Text>
                    <Text className="text-xs text-[#8a7b70]">alterou o status de {NOME_ENTIDADE[log.entidade].toLocaleLowerCase("pt-BR")}</Text>
                  </View>
                </View>

                <View className="flex-row flex-wrap justify-between gap-y-5">
                  <Metadado titulo="Data e hora" valor={formatarDataHora(log.data_hora)} />
                  <Metadado titulo="Ação" valor="Alteração de status" />
                  <Metadado titulo="Área" valor={NOME_ENTIDADE[log.entidade]} />
                  <Metadado titulo="Origem" valor={NOME_ORIGEM[log.origem_acao]} />
                  <Metadado titulo="Referência" valor={log.referencia_legivel || log.entidade_id} />
                  <Metadado titulo="Perfil" valor={log.usuario.perfil || "Não informado"} />
                </View>

                <View className="h-px bg-[#e9e0d7]" />
                <Text className="font-serif text-lg font-bold text-[#2d211a]">Alterações realizadas</Text>
                <Text className="text-xs font-medium text-[#685a4e]">Status</Text>
                <View className="flex-row items-center gap-2">
                  <View className="min-w-0 flex-1 gap-2 rounded-lg border border-[#e6ded6] p-3">
                    <Text className="text-[10px] font-bold tracking-widest text-[#978b81]">ANTES</Text>
                    <Text className="text-sm font-semibold text-[#382b22]">{formatarStatus(log.status_anterior)}</Text>
                  </View>
                  <Ionicons name="arrow-forward" size={18} color="#9b9085" />
                  <View className="min-w-0 flex-1 gap-2 rounded-lg border border-[#cce4d4] bg-[#f3faf5] p-3">
                    <Text className="text-[10px] font-bold tracking-widest text-[#59876a]">DEPOIS</Text>
                    <Text className="text-sm font-semibold text-[#2b4e39]">{formatarStatus(log.status_novo)}</Text>
                  </View>
                </View>

                {log.observacao ? (
                  <View className="gap-2">
                    <Text className="text-[11px] font-bold uppercase tracking-wider text-[#887c70]">Observação</Text>
                    <Text selectable className="text-sm leading-6 text-[#51453a]">{log.observacao}</Text>
                  </View>
                ) : null}

                <View className="flex-row items-start gap-2 rounded-lg bg-[#f5f6f3] p-3">
                  <Ionicons name="checkmark" size={16} color="#668570" />
                  <Text className="flex-1 text-xs leading-5 text-[#6a756c]">
                    Esta atividade foi registrada automaticamente e não pode ser alterada.
                  </Text>
                </View>
              </View>
            )}
          </ScrollView>
        </View>
      </View>
    </Modal>
  );
}
