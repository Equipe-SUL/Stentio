import { Text, View } from "react-native";
import { Ionicons } from "@expo/vector-icons";
import { DataTable } from "../usuarios/DataTable";
import type { ColumnDef } from "../usuarios/types";
import type { PreviaImportacao, RecursoPrevia } from "./types";

const columns: ColumnDef<RecursoPrevia>[] = [
  {
    key: "nome",
    header: "Profissional",
    render: (item) => (
      <View className="gap-1 min-w-0">
        <Text className="text-sm font-semibold text-zinc-900">{item.nome}</Text>
        <Text className="text-xs text-zinc-500" selectable>
          {item.email}
        </Text>
        <Text className="text-xs text-zinc-500">
          {item.telefone ?? "Sem telefone"}
        </Text>
      </View>
    ),
  },
  {
    key: "ativo",
    header: "Status",
    width: 90,
    render: ({ ativo }) => (
      <Text className="text-sm text-zinc-600">
        {ativo ? "Ativo" : "Inativo"}
      </Text>
    ),
  },
  {
    key: "servicos",
    header: "Serviços",
    render: ({ servicos }) => (
      <Text className="text-sm text-zinc-600">{servicos.join(", ")}</Text>
    ),
  },
];

export function PreviaRecursos({ previa }: { previa: PreviaImportacao }) {
  return (
    <View className="gap-5">
      <View className="rounded-xl border border-emerald-200 bg-emerald-50 p-4 flex-row items-start gap-3">
        <Ionicons name="checkmark-circle-outline" size={22} color="#047857" />
        <View className="flex-1 gap-1">
          <Text className="text-sm font-semibold text-emerald-800">
            Arquivo validado
          </Text>
          <Text className="text-sm leading-5 text-emerald-800">
            Estrutura e dados locais conferidos. Conflitos com os cadastros
            existentes serão verificados durante a importação. Nenhum dado foi
            gravado.
          </Text>
        </View>
      </View>
      <View className="flex-row flex-wrap gap-3">
        {[
          ["Linhas no arquivo", previa.total_linhas],
          ["Recursos", previa.total_recursos],
          ["Serviços distintos", previa.total_servicos],
          ["Vínculos", previa.total_vinculos],
        ].map(([label, valor]) => (
          <View
            key={label}
            style={{ flexBasis: "45%", flexGrow: 1, minWidth: 128 }}
            className="rounded-xl border border-zinc-200 bg-white p-4"
          >
            <Text className="text-2xl font-bold text-zinc-900">{valor}</Text>
            <Text className="text-xs text-zinc-500 mt-1">{label}</Text>
          </View>
        ))}
      </View>
      <View className="gap-2">
        <Text className="text-base font-semibold text-zinc-900">
          Prévia dos recursos
        </Text>
        <Text className="text-sm text-zinc-500">
          Amostra de {previa.amostra_recursos.length} de {previa.total_recursos}{" "}
          recursos. Linhas com o mesmo e-mail são agrupadas em um cadastro.
        </Text>
      </View>
      <DataTable
        data={previa.amostra_recursos}
        columns={columns}
        keyExtractor={(item) => item.email}
        scrollEnabled={false}
        mobileBreakpoint={580}
      />
    </View>
  );
}
