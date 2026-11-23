import { useLocalSearchParams } from "expo-router";
import { EntregaFinalRevisaoScreen } from "../../../../components/ordens/EntregaFinalRevisaoScreen";

export default function OrdemVerificacaoRoute() {
  const { id } = useLocalSearchParams<{ id: string }>();
  const idString = Array.isArray(id) ? id[0] : id ?? "";

  return <EntregaFinalRevisaoScreen ordemId={idString} />;
}