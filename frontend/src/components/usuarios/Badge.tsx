import { Text, View } from "react-native";

export function RoleBadge({ role }: { role: string }) {
  switch (role) {
    case "Gestor_Projeto":
      return (
        <View className="self-start rounded-md bg-blue-50 px-2.5 py-1">
          <Text className="font-mono text-[11px] font-medium uppercase text-blue-700">
            {role}
          </Text>
        </View>
      );

    case "Financeiro":
      return (
        <View className="self-start rounded-md bg-amber-50 px-2.5 py-1">
          <Text className="font-mono text-[11px] font-medium uppercase text-amber-700">
            {role}
          </Text>
        </View>
      );

    case "Admin":
      return (
        <View className="self-start rounded-md bg-violet-50 px-2.5 py-1">
          <Text className="font-mono text-[11px] font-medium uppercase text-violet-700">
            {role}
          </Text>
        </View>
      );

    case "Atendente":
      return (
        <View className="self-start rounded-md bg-emerald-50 px-2.5 py-1">
          <Text className="font-mono text-[11px] font-medium uppercase text-emerald-700">
            {role}
          </Text>
        </View>
      );

    default:
      return (
        <View className="self-start rounded-md bg-neutral-100 px-2.5 py-1">
          <Text className="font-mono text-[11px] font-medium uppercase text-neutral-700">
            {role}
          </Text>
        </View>
      );
  }
}

export function StatusBadge({ status }: { status: string }) {
  switch (status) {
    case "Ativo":
      return (
        <View className="self-start rounded-md bg-blue-50 px-2.5 py-1">
          <Text className="text-xs font-medium text-blue-700">{status}</Text>
        </View>
      );

    case "Inativo":
      return (
        <View className="self-start rounded-md bg-amber-50 px-2.5 py-1">
          <Text className="text-xs font-medium text-amber-700">{status}</Text>
        </View>
      );

    default:
      return (
        <View className="self-start rounded-md bg-neutral-100 px-2.5 py-1">
          <Text className="text-xs font-medium text-neutral-700">{status}</Text>
        </View>
      );
  }
}
