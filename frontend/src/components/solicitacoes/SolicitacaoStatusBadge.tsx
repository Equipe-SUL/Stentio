import { Text, View } from "react-native";

export type StatusSolicitacaoTipo =
  | "PENDENTE_ORCAMENTO"
  | "AGUARDANDO_APROVACAO"
  | "ORCAMENTO_REJEITADO"
  | "EM_EXECUCAO"
  | string;

const STATUS_MAP: Record<string, { label: string; bg: string; text: string }> = {
  PENDENTE_ORCAMENTO: {
    label: "Pendente Orçamento",
    bg: "bg-amber-50 border border-amber-200",
    text: "text-amber-700",
  },
  AGUARDANDO_APROVACAO: {
    label: "Aguardando aprovação",
    bg: "bg-blue-50 border border-blue-200",
    text: "text-blue-700",
  },
  EM_EXECUCAO: {
    label: "Em execução",
    bg: "bg-emerald-50 border border-emerald-200",
    text: "text-emerald-700",
  },
  ORCAMENTO_REJEITADO: {
    label: "Orçamento rejeitado",
    bg: "bg-red-50 border border-red-200",
    text: "text-red-700",
  },
};

export function SolicitacaoStatusBadge({ status }: { status: StatusSolicitacaoTipo }) {
  const config = STATUS_MAP[status] ?? {
    label: status,
    bg: "bg-neutral-100 border border-neutral-200",
    text: "text-neutral-700",
  };

  return (
    <View className={`self-start rounded-full px-2.5 py-1 ${config.bg}`}>
      <Text className={`text-xs font-semibold ${config.text}`}>{config.label}</Text>
    </View>
  );
}
