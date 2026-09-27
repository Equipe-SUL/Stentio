import { Redirect, Slot } from "expo-router";
import { ActivityIndicator, Pressable, Text, View } from "react-native";
import { useSession } from "../../lib/session";
import { AppShell, type MenuGroup } from "../../components/navegacao/AppShell";

const MENU_CONFIG: MenuGroup[] = [
  {
    title: "PAINEL",
    icon: "grid-outline",
    items: [{ name: "Dashboard", icon: "grid-outline" }],
  },
  {
    title: "OPERACIONAL",
    icon: "construct-outline",
    items: [
      { name: "Clientes", icon: "people-outline" },
      { name: "Solicitações", icon: "document-text-outline" },
      { name: "Ordens de Serviço", icon: "clipboard-outline" },
      { name: "Recursos", icon: "person-outline", route: "/recursos" },
    ],
  },
  {
    title: "SISTEMA",
    icon: "shield-checkmark-outline",
    items: [
      {
        name: "Central do sistema",
        icon: "settings-outline",
        route: "/admin",
      },
    ],
  },
];

export default function AppLayout() {
  const { usuario, carregando, sair } = useSession();

  if (carregando) {
    return (
      <View className="flex-1 items-center justify-center bg-neutral-50">
        <ActivityIndicator size="large" color="#6f4f28" />
      </View>
    );
  }

  if (!usuario) {
    return <Redirect href="/" />;
  }

  if (usuario.role !== "ADMIN") {
    return (
      <View className="flex-1 items-center justify-center gap-4 bg-neutral-50 p-6">
        <Text className="text-lg font-semibold text-neutral-900">Acesso restrito</Text>
        <Text className="text-center text-sm text-neutral-500">
          Este painel é acessível apenas para administradores.
        </Text>
        <Pressable onPress={sair} className="rounded-lg bg-[#6f4f28] px-5 py-3">
          <Text className="font-medium text-white">Sair</Text>
        </Pressable>
      </View>
    );
  }

  return (
    <AppShell
      groups={MENU_CONFIG}
      usuario={usuario}
      homeRoute="/admin"
      onSair={sair}
    >
      <Slot />
    </AppShell>
  );
}
