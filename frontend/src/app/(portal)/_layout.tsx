import React from "react";
import { Redirect, Slot, usePathname, useRouter } from "expo-router";
import { ActivityIndicator, Pressable, Text, View } from "react-native";
import { Ionicons } from "@expo/vector-icons";
import { useSafeAreaInsets } from "react-native-safe-area-context";
import { useSession } from "../../lib/session";
import { homeRouteForRole } from "../../lib/routePermissions";

export default function PortalRecursoLayout() {
  const { usuario, carregando, sair } = useSession();
  const pathname = usePathname();
  const router = useRouter();
  const insets = useSafeAreaInsets();

  if (carregando) {
    return (
      <View className="flex-1 items-center justify-center bg-neutral-50">
        <ActivityIndicator size="large" color="#8c5230" />
      </View>
    );
  }

  if (!usuario) return <Redirect href="/" />;
  if (usuario.role !== "RECURSO") {
    return <Redirect href={homeRouteForRole(usuario.role) as never} />;
  }

  return (
    <View className="flex-1 bg-neutral-50" style={{ paddingTop: insets.top }}>
      <View className="border-b border-neutral-200 bg-white px-4 py-3 md:px-5 md:py-4">
        <View className="mx-auto w-full max-w-5xl flex-col gap-3 md:flex-row md:items-center md:justify-between md:gap-4">
          <View className="flex-row items-center justify-between gap-3">
            <View className="flex-row items-center gap-3">
            <View className="h-10 w-10 items-center justify-center rounded-xl bg-[#8c5230]">
              <Text className="text-lg font-bold text-white">S</Text>
            </View>
            <View>
              <Text className="font-bold text-neutral-900">Portal do recurso</Text>
              <Text className="text-xs text-neutral-500">{usuario.nome}</Text>
            </View>
            </View>
            <Pressable
              accessibilityRole="button"
              accessibilityLabel="Sair do portal"
              onPress={sair}
              className="h-10 w-10 items-center justify-center rounded-lg border border-neutral-200 md:hidden"
            >
              <Ionicons name="log-out-outline" size={19} color="#52525b" />
            </Pressable>
          </View>
          <View className="flex-row items-center justify-between gap-1 md:w-auto md:justify-end md:gap-2">
            <Pressable
              accessibilityRole="button"
              accessibilityState={{ selected: pathname === "/portal" }}
              onPress={() => router.replace("/portal")}
              className={`flex-1 flex-row items-center justify-center gap-1.5 rounded-lg px-2 py-2 md:flex-none md:px-3 ${pathname === "/portal" ? "bg-[#f5ede7]" : "bg-white"}`}
            >
              <Ionicons name="briefcase-outline" size={17} color="#8c5230" />
              <Text className="text-xs font-medium text-neutral-800 md:text-sm">Minhas demandas</Text>
            </Pressable>
            <Pressable
              accessibilityRole="button"
              accessibilityState={{ selected: pathname === "/portal/certificacoes" }}
              onPress={() => router.replace("/portal/certificacoes" as never)}
              className={`flex-1 flex-row items-center justify-center gap-1.5 rounded-lg px-2 py-2 md:flex-none md:px-3 ${pathname === "/portal/certificacoes" ? "bg-[#f5ede7]" : "bg-white"}`}
            >
              <Ionicons name="ribbon-outline" size={18} color="#8c5230" />
              <Text className="text-xs font-medium text-neutral-800 md:text-sm">Certificações</Text>
            </Pressable>
            <Pressable
              accessibilityRole="button"
              accessibilityState={{ selected: pathname === "/portal/perfil" }}
              onPress={() => router.replace("/portal/perfil")}
              className={`flex-1 flex-row items-center justify-center gap-1.5 rounded-lg px-2 py-2 md:flex-none md:px-3 ${pathname === "/portal/perfil" ? "bg-[#f5ede7]" : "bg-white"}`}
            >
              <Ionicons name="person-circle-outline" size={18} color="#8c5230" />
              <Text className="text-xs font-medium text-neutral-800 md:text-sm">Meu perfil</Text>
            </Pressable>
            <Pressable
              accessibilityRole="button"
              accessibilityLabel="Sair do portal"
              onPress={sair}
              className="ml-1 hidden h-10 w-10 items-center justify-center rounded-lg border border-neutral-200 md:flex"
            >
              <Ionicons name="log-out-outline" size={19} color="#52525b" />
            </Pressable>
          </View>
        </View>
      </View>
      <View className="flex-1">
        <Slot />
      </View>
    </View>
  );
}
