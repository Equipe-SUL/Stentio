import { Text, View } from "react-native";
import { Ionicons } from "@expo/vector-icons";
import { DataTable } from "../usuarios/DataTable";
import type { ColumnDef } from "../usuarios/types";
import type { PreviaImportacao } from "./types";

type Amostra = PreviaImportacao["amostra_recursos"][number] & {
  totalPrecos: number;
};
const columns: ColumnDef<Amostra>[] = [
  {
    key: "dados",
    header: "Profissional",
    render: ({ dados }) => (
      <View className="min-w-0 gap-1">
        <Text className="text-sm font-semibold text-zinc-900">
          {dados.nome}
        </Text>
        <Text className="text-xs text-zinc-500" selectable>
          {dados.email}
        </Text>
      </View>
    ),
  },
  {
    key: "linhas",
    header: "Linhas do CSV",
    width: 120,
    render: ({ linhas }) => (
      <Text className="text-sm text-zinc-600">{linhas.join(", ")}</Text>
    ),
  },
  {
    key: "totalPrecos",
    header: "Preços",
    width: 125,
    render: ({ dados }) => (
      <Text className="text-sm text-zinc-600">
        {dados.precos.length} {dados.precos.length === 1 ? "preço" : "preços"}
        {"\n"}
        {dados.tiposServicoIds.length} serviço(s)
      </Text>
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
            Estrutura e dados locais conferidos. Idiomas, serviços e cadastros
            existentes serão verificados pelo sistema durante a importação.
          </Text>
        </View>
      </View>
      <View className="flex-row flex-wrap gap-3">
        {[
          { valor: previa.total_linhas, label: "Linhas no arquivo" },
          { valor: previa.total_recursos, label: "Recursos para importar" },
        ].map((item) => (
          <View
            key={item.label}
            className="flex-1 min-w-32 rounded-xl border border-zinc-200 bg-white p-4"
          >
            <Text className="text-2xl font-bold text-zinc-900">
              {item.valor}
            </Text>
            <Text className="text-xs text-zinc-500 mt-1">{item.label}</Text>
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
        data={previa.amostra_recursos.map((item) => ({
          ...item,
          totalPrecos: item.dados.precos.length,
        }))}
        columns={columns}
        keyExtractor={(item) => item.dados.email}
        scrollEnabled={false}
        mobileBreakpoint={580}
      />
    </View>
  );
}
