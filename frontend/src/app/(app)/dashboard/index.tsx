import { View, Text } from "react-native";

/** Estrutura inicial da rota; os cards e a lista serão adicionados nas T8/T9. */
export default function DashboardRoute() {
  return (
    <View className="flex-1 bg-[#f4f4f5] p-6">
      <View className="rounded-2xl border border-[#e7e5e4] bg-white p-6">
        <Text className="text-3xl font-semibold text-[#2D1A11]">Dashboard</Text>
        <Text className="mt-3 text-base text-[#6b625b]">
          Painel central de monitoramento operacional.
        </Text>
        <Text className="mt-2 text-sm text-[#78716c]">
          Indicadores e pendências serão apresentados nas próximas tarefas.
        </Text>
      </View>
    </View>
  );
}
