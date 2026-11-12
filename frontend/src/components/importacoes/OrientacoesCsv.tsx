import { useState } from "react";
import { Platform, Pressable, Text, View } from "react-native";
import { Ionicons } from "@expo/vector-icons";
import type { ConfiguracaoImportacao } from "./types";

const COLUNAS = [
  ["ativo", "Status do profissional: 1 para ativo ou 0 para inativo."],
  ["e-mail", "E-mail válido. Identifica e agrupa o profissional."],
  ["nome", "Nome do profissional, até 255 caracteres."],
  ["telefone", "Opcional. Até 20 caracteres; mantenha como texto."],
  ["tipo_servico", "Nome do serviço, até 255 caracteres."],
  ["descricao_servico", "Opcional. Descrição do serviço, até 500 caracteres."],
];
const MODELO =
  "ativo;e-mail;nome;telefone;tipo_servico;descricao_servico\r\n1;ana@example.com;Ana;01199990000;Tradução;\r\n1;ana@example.com;Ana;01199990000;Revisão;\r\n1;bruno@example.com;Bruno;;Tradução;\r\n";
function baixarModelo() {
  const url = URL.createObjectURL(
    new Blob(["\uFEFF", MODELO], { type: "text/csv;charset=utf-8" }),
  );
  const link = document.createElement("a");
  link.href = url;
  link.download = "recursos-servicos.csv";
  document.body.appendChild(link);
  link.click();
  link.remove();
  setTimeout(() => URL.revokeObjectURL(url), 1000);
}

export function OrientacoesCsv({
  configuracao,
}: {
  configuracao: ConfiguracaoImportacao | null;
}) {
  const [expandido, setExpandido] = useState(false);
  return (
    <View className="rounded-2xl border border-zinc-200 bg-white p-5 gap-4">
      <View className="flex-row gap-2 items-center">
        <Ionicons name="information-circle-outline" size={20} color="#8c5230" />
        <Text className="text-base font-semibold text-zinc-900">
          Prepare seu arquivo
        </Text>
      </View>
      <Text className="text-sm leading-6 text-zinc-600">
        Use um CSV em UTF-8, com cabeçalho e pelo menos um registro. São aceitos
        vírgula e ponto e vírgula como separador.
      </Text>
      <View className="gap-3">
        <Text className="text-sm leading-5 text-zinc-600">
          Uma linha representa um vínculo entre profissional e serviço. Repita o
          mesmo e-mail para associar outros serviços ao profissional.
        </Text>
        {configuracao ? (
          <Text className="text-sm leading-5 text-zinc-600">
            Até {configuracao.max_recursos} recursos e {configuracao.max_linhas}{" "}
            linhas por arquivo.
          </Text>
        ) : null}
      </View>
      <View className="rounded-lg bg-amber-50 p-3 gap-1">
        <Text className="text-xs font-semibold text-amber-900">
          Antes de importar
        </Text>
        <Text className="text-xs leading-5 text-amber-900">
          Serviços existentes são reutilizados e os ausentes são criados.
          Conflitos recusam o arquivo inteiro; cadastros não são sobrescritos.
        </Text>
      </View>
      {Platform.OS === "web" ? (
        <Pressable
          onPress={baixarModelo}
          accessibilityLabel="Baixar CSV de exemplo"
          accessibilityRole="button"
          className="flex-row items-center justify-center gap-2 border border-[#8c5230]/30 rounded-lg px-3 py-3"
        >
          <Ionicons name="download-outline" size={17} color="#8c5230" />
          <Text className="text-sm font-semibold text-[#8c5230]">
            Baixar CSV de exemplo
          </Text>
        </Pressable>
      ) : null}
      <Text className="text-xs leading-5 text-zinc-500">
        O exemplo representa 2 recursos, 2 serviços e 3 vínculos. Use dados
        fictícios no teste. O layout é provisório até o alinhamento com o
        cliente.
      </Text>
      <Pressable
        onPress={() => setExpandido(!expandido)}
        accessibilityLabel="Colunas do arquivo"
        accessibilityRole="button"
        accessibilityState={{ expanded: expandido }}
        className="flex-row items-center justify-between gap-2 py-2"
      >
        <Text className="text-sm font-semibold text-zinc-800">
          Colunas do arquivo
        </Text>
        <Ionicons
          name={expandido ? "chevron-up" : "chevron-down"}
          size={16}
          color="#71717a"
        />
      </Pressable>
      {expandido ? (
        <View className="gap-3">
          {COLUNAS.map(([nome, descricao]) => (
            <View key={nome} className="border-t border-zinc-100 pt-3 gap-1">
              <Text className="text-xs font-bold text-zinc-800" selectable>
                {nome}
              </Text>
              <Text className="text-xs leading-5 text-zinc-500">
                {descricao}
              </Text>
            </View>
          ))}
        </View>
      ) : null}
    </View>
  );
}
