import { Text, View } from "react-native";
import { Ionicons } from "@expo/vector-icons";
import type { RelatorioImportacao } from "./types";

export function RelatorioRecursos({
  relatorio,
}: {
  relatorio: RelatorioImportacao;
}) {
  const novos =
    relatorio.recursos_criados +
    relatorio.servicos_criados +
    relatorio.vinculos_criados;
  return (
    <View className="gap-5">
      <View
        accessibilityLiveRegion="polite"
        className="rounded-xl border border-emerald-200 bg-emerald-50 p-4 gap-2"
      >
        <View className="flex-row gap-2 items-center">
          <Ionicons name="checkmark-circle-outline" size={22} color="#047857" />
          <Text className="text-base font-semibold text-emerald-800">
            Importação concluída
          </Text>
        </View>
        <Text className="text-sm leading-5 text-emerald-800">
          A transação foi confirmada. Foram processados{" "}
          {relatorio.total_recursos} recursos e {relatorio.total_servicos}{" "}
          serviços distintos.
        </Text>
        {novos === 0 ? (
          <Text className="text-sm font-semibold text-emerald-800">
            Nenhum cadastro ou vínculo novo: todos já existiam no sistema.
          </Text>
        ) : null}
      </View>
      <View className="flex-row flex-wrap gap-3">
        {[
          ["Recursos criados", relatorio.recursos_criados],
          ["Recursos reutilizados", relatorio.recursos_reutilizados],
          ["Serviços criados", relatorio.servicos_criados],
          ["Serviços reutilizados", relatorio.servicos_reutilizados],
          ["Vínculos criados", relatorio.vinculos_criados],
          ["Vínculos existentes", relatorio.vinculos_existentes],
        ].map(([label, valor]) => (
          <View
            key={label}
            style={{ flexBasis: "30%", flexGrow: 1, minWidth: 128 }}
            className="rounded-xl border border-zinc-200 bg-white p-4"
          >
            <Text className="text-2xl font-bold text-zinc-900">{valor}</Text>
            <Text className="text-xs text-zinc-500 mt-1">{label}</Text>
          </View>
        ))}
      </View>
      <View className="rounded-xl border border-zinc-200 bg-white p-4 gap-2">
        <Text className="text-sm font-semibold text-zinc-900">
          Resumo do arquivo
        </Text>
        <Text className="text-sm text-zinc-600" selectable>
          {relatorio.filename}
        </Text>
        <Text className="text-sm text-zinc-600">
          {relatorio.total_linhas} linhas • {relatorio.total_recursos} recursos
          • {relatorio.total_servicos} serviços
        </Text>
        <Text className="text-xs leading-5 text-zinc-500">
          O serviço retorna contadores do arquivo inteiro. Cadastros e vínculos
          existentes foram preservados.
        </Text>
      </View>
    </View>
  );
}
