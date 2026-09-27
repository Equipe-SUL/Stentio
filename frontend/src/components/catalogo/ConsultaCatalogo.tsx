import type { ReactNode } from "react";
import { ScrollView, View } from "react-native";
import { CabecalhoTela } from "../navegacao/CabecalhoTela";

interface ConsultaCatalogoProps {
  contexto: string;
  secao: string;
  titulo: string;
  descricao?: string;
  children: ReactNode;
}

export function ConsultaCatalogo({
  contexto,
  secao,
  titulo,
  descricao,
  children,
}: ConsultaCatalogoProps) {
  return (
    <ScrollView
      className="flex-1 bg-[#fbfaf8]"
      contentContainerStyle={{ flexGrow: 1, paddingBottom: 40 }}
      keyboardShouldPersistTaps="handled"
    >
      <View className="w-full max-w-5xl mx-auto px-4 py-8 md:px-8">
        <CabecalhoTela
          contexto={contexto}
          secao={secao}
          titulo={titulo}
          descricao={descricao}
          rotaFallback="/configs"
        />

        {children}
      </View>
    </ScrollView>
  );
}
