import { ActivityIndicator, Pressable, Text, View } from "react-native";
import { Ionicons } from "@expo/vector-icons";
import { formatarBytes } from "../../lib/importacaoService";
import type { ArquivoCsv, ConfiguracaoImportacao, Separador } from "./types";

interface Props {
  arquivo: ArquivoCsv | null;
  configuracao: ConfiguracaoImportacao;
  separador: Separador;
  disabled: boolean;
  selecionando: boolean;
  onSelecionar: () => void;
  onRemover: () => void;
  onSeparador: (valor: Separador) => void;
}

export function ArquivoCsvField({
  arquivo,
  configuracao,
  separador,
  disabled,
  selecionando,
  onSelecionar,
  onRemover,
  onSeparador,
}: Props) {
  return (
    <View className="gap-5">
      <Pressable
        onPress={onSelecionar}
        disabled={disabled}
        accessibilityRole="button"
        accessibilityLabel={
          arquivo ? "Trocar arquivo CSV" : "Selecionar arquivo CSV"
        }
        accessibilityState={{ disabled }}
        className={`items-center justify-center gap-3 rounded-xl border-2 border-dashed border-[#8c5230]/40 bg-[#8c5230]/5 px-5 py-10 active:bg-[#8c5230]/10 ${disabled ? "opacity-60" : ""}`}
      >
        <View className="h-14 w-14 items-center justify-center rounded-2xl bg-white border border-[#8c5230]/15">
          {selecionando ? (
            <ActivityIndicator color="#8c5230" />
          ) : (
            <Ionicons name="cloud-upload-outline" size={28} color="#8c5230" />
          )}
        </View>
        <Text className="text-center text-base font-semibold text-zinc-900">
          {selecionando
            ? "Abrindo arquivos..."
            : arquivo
              ? "Selecionar outro arquivo"
              : "Selecione seu arquivo CSV"}
        </Text>
        <Text className="text-center text-sm text-zinc-500">
          Um arquivo por vez • até{" "}
          {formatarBytes(configuracao.max_upload_bytes)}
        </Text>
      </Pressable>

      {arquivo ? (
        <View className="flex-row items-center gap-3 rounded-xl border border-zinc-200 bg-white p-4">
          <Ionicons name="document-text-outline" size={26} color="#8c5230" />
          <View className="flex-1 min-w-0">
            <Text
              className="text-sm font-semibold text-zinc-900"
              numberOfLines={2}
            >
              {arquivo.name}
            </Text>
            <Text className="text-xs text-zinc-500 mt-1">
              {formatarBytes(arquivo.file?.size ?? arquivo.size)}
            </Text>
          </View>
          <Pressable
            onPress={onRemover}
            disabled={disabled}
            accessibilityRole="button"
            accessibilityLabel="Remover arquivo CSV"
            accessibilityState={{ disabled }}
            hitSlop={8}
            className="p-2"
          >
            <Ionicons
              name="close-outline"
              size={22}
              color={disabled ? "#a1a1aa" : "#71717a"}
            />
          </Pressable>
        </View>
      ) : null}

      <View className="gap-2">
        <Text className="text-sm font-semibold text-zinc-800">
          Separador de colunas
        </Text>
        <View className="flex-row flex-wrap gap-2">
          {(
            [
              { valor: ";", texto: "Ponto e vírgula (;)" },
              { valor: ",", texto: "Vírgula (,)" },
            ] as const
          ).map(({ valor, texto }) => (
            <Pressable
              key={valor}
              onPress={() => onSeparador(valor)}
              disabled={disabled}
              accessibilityRole="radio"
              accessibilityState={{ selected: separador === valor, disabled }}
              className={`rounded-lg border px-3 py-2 ${separador === valor ? "border-[#8c5230] bg-[#8c5230]/10" : "border-zinc-200 bg-white"} ${disabled ? "opacity-60" : ""}`}
            >
              <Text
                className={`text-xs font-medium ${separador === valor ? "text-[#8c5230]" : "text-zinc-600"}`}
              >
                {texto}
              </Text>
            </Pressable>
          ))}
        </View>
        <Text className="text-xs leading-5 text-zinc-500">
          Selecione o separador do arquivo. O modelo usa ponto e vírgula (;).
        </Text>
      </View>
    </View>
  );
}
