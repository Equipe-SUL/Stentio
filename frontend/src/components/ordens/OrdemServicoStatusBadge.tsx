import { Text, View } from "react-native";
import type { StatusOrdemServico } from "./types";

interface StatusConfig {
  label: string;
  bg: string;
  text: string;
  dotColor: string;
}

const STATUS_MAP: Record<string, StatusConfig> = {
  PENDENTE_ALOCACAO: {
    label: "Pendente de Alocação",
    bg: "bg-amber-50 border border-amber-200",
    text: "text-amber-800",
    dotColor: "bg-amber-500",
  },
  EM_ANDAMENTO: {
    label: "Em Andamento",
    bg: "bg-blue-50 border border-blue-200",
    text: "text-blue-800",
    dotColor: "bg-blue-500",
  },
  CONCLUIDA: {
    label: "Concluída",
    bg: "bg-emerald-50 border border-emerald-200",
    text: "text-emerald-800",
    dotColor: "bg-emerald-500",
  },
  FINALIZADA: {
    label: "Finalizada",
    bg: "bg-emerald-50 border border-emerald-200",
    text: "text-emerald-800",
    dotColor: "bg-emerald-500",
  },
  CANCELADA: {
    label: "Cancelada",
    bg: "bg-red-50 border border-red-200",
    text: "text-red-800",
    dotColor: "bg-red-500",
  },
};

export function OrdemServicoStatusBadge({
  status,
  tamanho = "normal",
}: {
  status: StatusOrdemServico;
  tamanho?: "pequeno" | "normal";
}) {
  const config = STATUS_MAP[status] ?? {
    label: status ? status.replace(/_/g, " ") : "Desconhecido",
    bg: "bg-neutral-100 border border-neutral-200",
    text: "text-neutral-700",
    dotColor: "bg-neutral-400",
  };

  const padraoClasses =
    tamanho === "pequeno"
      ? "px-2 py-0.5 text-[11px]"
      : "px-2.5 py-1 text-xs";

  return (
    <View
      className={`self-start flex-row items-center gap-1.5 rounded-full ${config.bg} ${padraoClasses}`}
    >
      <View className={`h-1.5 w-1.5 rounded-full ${config.dotColor}`} />
      <Text className={`font-semibold ${config.text}`}>{config.label}</Text>
    </View>
  );
}
