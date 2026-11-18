import { Ionicons } from "@expo/vector-icons";
import type { ComponentProps } from "react";
import { Text, View } from "react-native";

type IconName = ComponentProps<typeof Ionicons>["name"];

interface MetricCardProps {
  titulo: string;
  descricao: string;
  valor?: number;
  icone: IconName;
  cor: string;
  fundoIcone: string;
  largura: number | `${number}%`;
}

export function MetricCard({
  titulo, descricao, valor, icone, cor, fundoIcone, largura,
}: MetricCardProps) {
  return (
    <View
      style={{ width: largura, minWidth: 0, minHeight: 165 }}
      className="rounded-2xl border border-[#e8e2db] bg-white px-5 py-5 shadow-sm"
    >
      <View className="flex-row items-start justify-between gap-3">
        <Text className="flex-1 text-sm font-medium text-[#615a54]">{titulo}</Text>
        <View
          className="h-9 w-9 items-center justify-center rounded-xl"
          style={{ backgroundColor: fundoIcone }}
        >
          <Ionicons name={icone} size={19} color={cor} />
        </View>
      </View>
      <Text className="mt-3 text-4xl font-semibold tracking-tight text-[#2e241d]">
        {valor === undefined ? "—" : new Intl.NumberFormat("pt-BR").format(valor)}
      </Text>
      <Text className="mt-2 text-xs leading-5 text-[#8a8179]">{descricao}</Text>
    </View>
  );
}
