import React from "react";
import { ScrollView, Text, TouchableOpacity, View } from "react-native";
import { useRouter, type Href } from "expo-router";
import { Ionicons } from "@expo/vector-icons";

export interface HubItem {
  nome: string;
  descricao: string;
  icon: React.ComponentProps<typeof Ionicons>["name"];
  route?: Href;
}

interface HubScreenProps {
  breadcrumb: string;
  titulo: string;
  subtitulo: string;
  itens: HubItem[];
}

export function HubScreen({
  breadcrumb,
  titulo,
  subtitulo,
  itens,
}: HubScreenProps) {
  const router = useRouter();

  return (
    <ScrollView
      className="flex-1 bg-[#fbfaf8]"
      contentContainerStyle={{ flexGrow: 1, paddingBottom: 40 }}
    >
      <View className="w-full max-w-5xl mx-auto px-4 py-8 md:px-8">
        <View className="flex-row items-center justify-between mb-8 pb-4 border-b border-zinc-200">
          <View className="flex-1 min-w-0 flex-row items-center gap-3">
            <TouchableOpacity
              onPress={() =>
                router.canGoBack() ? router.back() : router.push("/admin")
              }
              className="p-2.5 rounded-xl bg-white border border-zinc-200 shadow-sm active:bg-zinc-100"
              accessibilityLabel="Voltar"
            >
              <Ionicons name="arrow-back" size={20} color="#8c5230" />
            </TouchableOpacity>

            <View className="flex-1 min-w-0">
              <View className="flex-row items-center gap-2 flex-wrap">
                <Text className="text-xs font-bold text-[#8c5230] uppercase tracking-wider">
                  {breadcrumb}
                </Text>
                <Text className="text-xs text-zinc-400">•</Text>
                <Text className="text-xs text-zinc-500 font-medium">
                  {titulo}
                </Text>
              </View>
              <Text className="text-2xl md:text-3xl font-serif font-bold text-zinc-900 mt-1">
                {titulo}
              </Text>
            </View>
          </View>
        </View>

        <Text className="text-sm text-zinc-600 mb-6 max-w-2xl">{subtitulo}</Text>

        <View className="flex-row flex-wrap gap-4">
          {itens.map((item) => {
            const disponivel = item.route !== undefined;
            return (
              <TouchableOpacity
                key={item.nome}
                accessibilityRole="button"
                accessibilityState={{ disabled: !disponivel }}
                disabled={!disponivel}
                onPress={() => item.route && router.push(item.route)}
                className={`w-full sm:w-[calc(50%-0.5rem)] lg:w-[calc(33.333%-0.6667rem)] rounded-2xl bg-white border border-zinc-200 p-5 active:bg-zinc-100 ${
                  disponivel ? "shadow-sm" : "opacity-45"
                }`}
              >
                <View className="flex-row items-start gap-4">
                  <View
                    className={`w-11 h-11 rounded-xl items-center justify-center ${
                      disponivel ? "bg-[#8c5230]/10" : "bg-zinc-100"
                    }`}
                  >
                    <Ionicons
                      name={item.icon}
                      size={22}
                      color={disponivel ? "#8c5230" : "#a1a1aa"}
                    />
                  </View>

                  <View className="flex-1 min-w-0">
                    <View className="flex-row items-center gap-2">
                      <Text className="text-base font-semibold text-zinc-900 flex-1">
                        {item.nome}
                      </Text>
                      {disponivel ? (
                        <Ionicons
                          name="chevron-forward"
                          size={16}
                          color="#a1a1aa"
                        />
                      ) : (
                        <View className="bg-zinc-100 px-2 py-0.5 rounded-full">
                          <Text className="text-[10px] font-bold text-zinc-500 tracking-wider">
                            EM BREVE
                          </Text>
                        </View>
                      )}
                    </View>
                    <Text className="text-sm text-zinc-500 mt-1">
                      {item.descricao}
                    </Text>
                  </View>
                </View>
              </TouchableOpacity>
            );
          })}
        </View>
      </View>
    </ScrollView>
  );
}
