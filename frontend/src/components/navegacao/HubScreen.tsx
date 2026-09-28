import React, { useState } from "react";
import {
  LayoutChangeEvent,
  ScrollView,
  Text,
  TouchableOpacity,
  useWindowDimensions,
  View,
} from "react-native";
import { useRouter, type Href } from "expo-router";
import { Ionicons } from "@expo/vector-icons";

// A largura do card é medida com onLayout, e não calculada a partir da largura
// da janela: o container tem max-w, mx-auto, padding responsivo e safe area, e
// qualquer um desses faz a largura da janela divergir da largura real do
// conteúdo — o card estoura a margem. onLayout entrega a largura já sem o
// padding do container, então não há nada para estimar.
//
// A contagem de colunas continua vindo da janela, porque uma decisão grossa
// (1, 2 ou 3 colunas) tolera imprecisão; só a largura em pixel exige medidação.
const COLUNAS_2_BREAKPOINT = 640;
const COLUNAS_3_BREAKPOINT = 1024;
const GAP_CARDS = 16;

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
  const { width: larguraJanela } = useWindowDimensions();
  const [larguraGrid, setLarguraGrid] = useState(0);

  const colunas =
    larguraJanela >= COLUNAS_3_BREAKPOINT
      ? 3
      : larguraJanela >= COLUNAS_2_BREAKPOINT
        ? 2
        : 1;

  // O Math.floor garante que os cards somem menos que a linha mesmo com erro de
  // ponto flutuante: sobra um pixel no fim da linha em vez de o último card
  // quebrar sozinho para a linha de baixo.
  const larguraCard =
    larguraGrid > 0
      ? Math.floor(
          (larguraGrid - GAP_CARDS * (colunas - 1)) / colunas,
        )
      : 0;

  // Nada é renderizado antes do primeiro onLayout: com largura 0 os cards
  // colapsariam e a lista daria um salto visível ao abrir a tela.
  const itensRenderizaveis = larguraGrid > 0 ? itens : [];

  function medirGrid(event: LayoutChangeEvent) {
    setLarguraGrid(event.nativeEvent.layout.width);
  }

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

        <View
          className="flex-row flex-wrap"
          style={{ gap: GAP_CARDS }}
          onLayout={medirGrid}
        >
          {itensRenderizaveis.map((item) => {
            const disponivel = item.route !== undefined;
            return (
              <TouchableOpacity
                key={item.nome}
                accessibilityRole="button"
                accessibilityState={{ disabled: !disponivel }}
                disabled={!disponivel}
                onPress={() => item.route && router.push(item.route)}
                className={`rounded-2xl bg-white border border-zinc-200 p-5 active:bg-zinc-100 ${
                  disponivel ? "shadow-sm" : "opacity-45"
                }`}
                style={{ width: larguraCard, flexShrink: 0 }}
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
