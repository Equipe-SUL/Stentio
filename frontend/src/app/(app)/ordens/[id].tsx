import { ScrollView, View } from "react-native";
import { useLocalSearchParams, useRouter } from "expo-router";
import { CabecalhoTela } from "../../../components/navegacao/CabecalhoTela";
import { OrdemServicoDetalhesView } from "../../../components/ordens/OrdemServicoDetalhesView";

export default function OrdemDetalheRoute() {
  const router = useRouter();
  const { id } = useLocalSearchParams<{ id: string }>();

  const idString = Array.isArray(id) ? id[0] : id ?? "";

  return (
    <ScrollView
      className="flex-1 bg-[#fbfaf8]"
      contentContainerStyle={{ flexGrow: 1, paddingBottom: 40 }}
      keyboardShouldPersistTaps="handled"
    >
      <View className="w-full max-w-5xl mx-auto px-4 py-8 md:px-8 gap-6">
        <CabecalhoTela
          contexto="Operacional"
          secao="Ordens de Serviço"
          titulo="Detalhes da Ordem"
          descricao="Consulte as informações completas do projeto, dados do cliente, solicitação, idiomas e arquivos anexados."
          rotaFallback="/ordens"
        />

        <OrdemServicoDetalhesView
          id={idString}
          onVoltar={() => router.push("/ordens")}
        />
      </View>
    </ScrollView>
  );
}
