import type { ReactNode } from "react";
import { Text, TouchableOpacity, View } from "react-native";
import { useRouter, type Href } from "expo-router";
import { Ionicons } from "@expo/vector-icons";

interface CabecalhoTelaProps {
  contexto: string;
  secao: string;
  titulo: string;
  descricao?: string;
  rotaFallback?: Href;
  acoes?: ReactNode;
}

export function CabecalhoTela({
  contexto,
  secao,
  titulo,
  descricao,
  rotaFallback = "/admin",
  acoes,
}: CabecalhoTelaProps) {
  const router = useRouter();

  return (
    <>
      <View className="flex-row items-center justify-between mb-8 pb-4 border-b border-zinc-200">
        {/* flex-1 + min-w-0: no React Native o flexShrink padrão é 0, diferente do
            CSS, então sem isto o bloco de título não encolhe e estoura a largura
            do celular. min-w-0 é o que permite o texto quebrar. */}
        <View className="flex-1 min-w-0 flex-row items-center gap-3">
          <TouchableOpacity
            onPress={() => (router.canGoBack() ? router.back() : router.push(rotaFallback))}
            className="p-2.5 rounded-xl bg-white border border-zinc-200 shadow-sm active:bg-zinc-100"
            accessibilityLabel="Voltar"
          >
            <Ionicons name="arrow-back" size={20} color="#8c5230" />
          </TouchableOpacity>

          <View className="flex-1 min-w-0">
            <View className="flex-row items-center gap-2 flex-wrap">
              <Text className="text-xs font-bold text-[#8c5230] uppercase tracking-wider">
                {contexto}
              </Text>
              <Text className="text-xs text-zinc-400">•</Text>
              <Text className="text-xs text-zinc-500 font-medium">{secao}</Text>
            </View>
            <Text className="text-2xl md:text-3xl font-serif font-bold text-zinc-900 mt-1">
              {titulo}
            </Text>
          </View>
        </View>

        {acoes}
      </View>

      {descricao ? (
        <Text className="text-sm text-zinc-600 mb-6 max-w-2xl">{descricao}</Text>
      ) : null}
    </>
  );
}
