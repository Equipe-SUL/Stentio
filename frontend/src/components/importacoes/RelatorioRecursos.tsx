import { useState } from "react";
import { Pressable, Text, View } from "react-native";
import { DataTable } from "../usuarios/DataTable";
import type { ColumnDef } from "../usuarios/types";
import type {
  RelatorioImportacao,
  ResultadoRecurso,
  StatusRecurso,
} from "./types";

const STATUS: Record<StatusRecurso, { label: string; classe: string }> = {
  IMPORTADO: { label: "Importado", classe: "bg-emerald-50 text-emerald-800" },
  CONFLITO: { label: "Conflito", classe: "bg-amber-50 text-amber-800" },
  REJEITADO: { label: "Rejeitado", classe: "bg-red-50 text-red-700" },
  INDETERMINADO: {
    label: "Indeterminado",
    classe: "bg-amber-50 text-amber-800",
  },
  NAO_PROCESSADO: {
    label: "Não processado",
    classe: "bg-zinc-100 text-zinc-600",
  },
  FALHA_CONEXAO: {
    label: "Falha de conexão",
    classe: "bg-red-50 text-red-700",
  },
};
const TITULOS: Record<RelatorioImportacao["status"], string> = {
  CONCLUIDA: "Importação concluída",
  CONCLUIDA_COM_CONFLITOS: "Concluída com conflitos",
  PARCIAL: "Importação parcialmente concluída",
  FALHOU: "Importação não concluída",
  INDETERMINADA: "Há resultados a conferir",
};
const columns: ColumnDef<ResultadoRecurso>[] = [
  {
    key: "email",
    header: "Profissional",
    render: (item) => (
      <View className="gap-1 min-w-0">
        <Text className="text-sm font-medium text-zinc-900" selectable>
          {item.email}
        </Text>
        <Text className="text-xs text-zinc-500">
          Linhas: {item.linhas.join(", ")}
        </Text>
      </View>
    ),
  },
  {
    key: "status",
    header: "Resultado",
    width: 150,
    render: ({ status }) => {
      const info = STATUS[status];
      return (
        <View
          className={`self-start rounded-full px-2.5 py-1 ${info.classe.split(" ")[0]}`}
        >
          <Text
            className={`text-xs font-semibold ${info.classe.split(" ")[1]}`}
          >
            {info.label}
          </Text>
        </View>
      );
    },
  },
  {
    key: "mensagem",
    header: "Detalhes",
    render: (item) => (
      <Text className="text-xs leading-5 text-zinc-600" selectable>
        {item.mensagem ?? (item.id ? `Cadastro criado: ${item.id}` : "—")}
      </Text>
    ),
  },
];

export function RelatorioRecursos({
  relatorio,
}: {
  relatorio: RelatorioImportacao;
}) {
  const [soPendencias, setSoPendencias] = useState(false);
  const [pagina, setPagina] = useState(1);
  const resultados = soPendencias
    ? relatorio.resultados.filter((item) => item.status !== "IMPORTADO")
    : relatorio.resultados;
  const paginas = Math.max(1, Math.ceil(resultados.length / 10));
  const atual = Math.min(pagina, paginas);
  const tudoImportado = relatorio.status === "CONCLUIDA";
  return (
    <View className="gap-5">
      <View
        className={`rounded-xl border p-4 gap-1 ${tudoImportado ? "bg-emerald-50 border-emerald-200" : "bg-amber-50 border-amber-200"}`}
      >
        <Text
          className={`text-base font-semibold ${tudoImportado ? "text-emerald-800" : "text-amber-900"}`}
        >
          {TITULOS[relatorio.status]}
        </Text>
        <Text className="text-sm leading-5 text-zinc-700">
          {relatorio.importados} de {relatorio.total_recursos} recursos
          importados. Os cadastros confirmados já estão salvos no sistema.
        </Text>
        {relatorio.indeterminados > 0 ? (
          <Text className="text-sm font-semibold text-amber-900 mt-2">
            Confira os resultados indeterminados na lista de recursos antes de
            reenviar o arquivo.
          </Text>
        ) : null}
      </View>
      <View className="flex-row flex-wrap gap-3">
        {[
          ["Importados", relatorio.importados],
          ["Conflitos", relatorio.conflitos],
          ["Rejeitados", relatorio.rejeitados],
          ["Indeterminados", relatorio.indeterminados],
          ["Não processados", relatorio.nao_processados],
          ["Falhas de conexão", relatorio.falhas_conexao],
        ].map(([label, count]) => (
          <View
            key={label}
            style={{ flexBasis: "30%", flexGrow: 1, minWidth: 128 }}
            className="rounded-xl border border-zinc-200 bg-white p-4"
          >
            <Text className="text-xl font-bold text-zinc-900">{count}</Text>
            <Text className="text-xs text-zinc-500 mt-1">{label}</Text>
          </View>
        ))}
      </View>
      <View className="flex-row items-center justify-between flex-wrap gap-3">
        <Text className="text-base font-semibold text-zinc-900">
          Resultado por recurso
        </Text>
        <Pressable
          onPress={() => {
            setSoPendencias(!soPendencias);
            setPagina(1);
          }}
          accessibilityRole="checkbox"
          accessibilityState={{ checked: soPendencias }}
          className="rounded-lg border border-zinc-200 bg-white px-3 py-2"
        >
          <Text className="text-xs font-medium text-[#8c5230]">
            {soPendencias ? "Mostrar todos" : "Mostrar pendências"}
          </Text>
        </Pressable>
      </View>
      <Text className="text-xs text-zinc-500" selectable>
        Arquivo: {relatorio.filename}
      </Text>
      <DataTable
        data={resultados.slice((atual - 1) * 10, atual * 10)}
        columns={columns}
        keyExtractor={(item) => item.email}
        scrollEnabled={false}
        mobileBreakpoint={650}
        emptyMessage="Nenhuma pendência encontrada."
      />
      <View className="flex-row items-center justify-between gap-2">
        <Pressable
          disabled={atual === 1}
          onPress={() => setPagina(atual - 1)}
          accessibilityRole="button"
          className={`rounded-lg border border-zinc-200 bg-white px-3 py-2 ${atual === 1 ? "opacity-40" : ""}`}
        >
          <Text className="text-xs text-zinc-700">Anterior</Text>
        </Pressable>
        <Text className="text-xs text-zinc-500">
          Página {atual} de {paginas}
        </Text>
        <Pressable
          disabled={atual === paginas}
          onPress={() => setPagina(atual + 1)}
          accessibilityRole="button"
          className={`rounded-lg border border-zinc-200 bg-white px-3 py-2 ${atual === paginas ? "opacity-40" : ""}`}
        >
          <Text className="text-xs text-zinc-700">Próxima</Text>
        </Pressable>
      </View>
    </View>
  );
}
