import React from "react";
import { Redirect, Slot, usePathname, useRouter } from "expo-router";
import { ActivityIndicator, Pressable, Text, View } from "react-native";
import { Ionicons } from "@expo/vector-icons";
import { useSession } from "../../lib/session";
import { homeRouteForRole } from "../../lib/routePermissions";

export default function PortalRecursoLayout() {
  const { usuario, carregando, sair } = useSession();
  const pathname = usePathname();
  const router = useRouter();

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
    <View className="flex-1 bg-neutral-50">
      <View className="border-b border-neutral-200 bg-white px-5 py-4">
        <View className="mx-auto w-full max-w-5xl flex-row items-center justify-between gap-4">
          <View className="flex-row items-center gap-3">
            <View className="h-10 w-10 items-center justify-center rounded-xl bg-[#8c5230]">
              <Text className="text-lg font-bold text-white">S</Text>
            </View>
            <View>
              <Text className="font-bold text-neutral-900">Portal do recurso</Text>
              <Text className="text-xs text-neutral-500">{usuario.nome}</Text>
            </View>
          </View>
          <View className="flex-row items-center gap-2">
            <Pressable
              accessibilityRole="button"
              accessibilityState={{ selected: pathname === "/portal" }}
              onPress={() => router.replace("/portal")}
              className={`flex-row items-center gap-2 rounded-lg px-3 py-2 ${pathname === "/portal" ? "bg-[#f5ede7]" : "bg-white"}`}
            >
              <Ionicons name="briefcase-outline" size={17} color="#8c5230" />
              <Text className="text-sm font-medium text-neutral-800">Minhas demandas</Text>
            </Pressable>
            <Pressable
              accessibilityRole="button"
              accessibilityState={{ selected: pathname === "/portal/perfil" }}
              onPress={() => router.replace("/portal/perfil")}
              className={`flex-row items-center gap-2 rounded-lg px-3 py-2 ${pathname === "/portal/perfil" ? "bg-[#f5ede7]" : "bg-white"}`}
            >
              <Ionicons name="person-circle-outline" size={18} color="#8c5230" />
              <Text className="text-sm font-medium text-neutral-800">Meu perfil</Text>
            </Pressable>
            <Pressable
              accessibilityRole="button"
              accessibilityLabel="Sair do portal"
              onPress={sair}
              className="ml-1 h-10 w-10 items-center justify-center rounded-lg border border-neutral-200"
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
