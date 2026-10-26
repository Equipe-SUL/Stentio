// ArquivosField.tsx
import { useState } from "react";
import { Pressable, Text, View } from "react-native";
import * as DocumentPicker from "expo-document-picker";
import { Ionicons } from "@expo/vector-icons";
import type { ArquivoSelecionado } from "./types";

// Alinhado com spring.servlet.multipart.max-file-size do backend (10MB).
export const TAMANHO_MAXIMO_MB = 10;
const TAMANHO_MAXIMO_BYTES = TAMANHO_MAXIMO_MB * 1024 * 1024;

function formatarTamanho(bytes?: number): string {
  if (bytes === undefined) return "";
  if (bytes < 1024) return `${bytes} B`;
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(0)} KB`;
  return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
}

interface ArquivosFieldProps {
  arquivos: ArquivoSelecionado[];
  onChange: (arquivos: ArquivoSelecionado[]) => void;
  disabled?: boolean;
}

export function ArquivosField({ arquivos, onChange, disabled = false }: ArquivosFieldProps) {
  const [abrindo, setAbrindo] = useState(false);
  const [aviso, setAviso] = useState("");

  async function selecionar() {
    if (abrindo || disabled) return;

    setAviso("");
    setAbrindo(true);
    try {
      // CA.3: um ou vários arquivos. `copyToCacheDirectory` garante uma uri legível no envio.
      const resultado = await DocumentPicker.getDocumentAsync({
        multiple: true,
        copyToCacheDirectory: true,
        type: "*/*",
      });
      if (resultado.canceled) return;

      const novos: ArquivoSelecionado[] = [];
      const grandesDemais: string[] = [];

      for (const asset of resultado.assets) {
        if (asset.size !== undefined && asset.size > TAMANHO_MAXIMO_BYTES) {
          grandesDemais.push(asset.name);
          continue;
        }

        const repetido = [...arquivos, ...novos].some(
          (existente) => existente.nome === asset.name && existente.tamanho === asset.size,
        );
        if (repetido) continue;

        novos.push({
          id: asset.uri,
          nome: asset.name,
          uri: asset.uri,
          tipo: asset.mimeType ?? "application/octet-stream",
          tamanho: asset.size,
          file: asset.file,
        });
      }

      if (novos.length > 0) onChange([...arquivos, ...novos]);
      if (grandesDemais.length > 0) {
        setAviso(`Acima de ${TAMANHO_MAXIMO_MB} MB e não adicionado: ${grandesDemais.join(", ")}`);
      }
    } finally {
      setAbrindo(false);
    }
  }

  // CA.4: remover um arquivo antes do envio.
  function remover(id: string) {
    onChange(arquivos.filter((arquivo) => arquivo.id !== id));
  }

  return (
    <View className="gap-3">
      <Pressable
        onPress={selecionar}
        disabled={disabled || abrindo}
        accessibilityRole="button"
        accessibilityLabel="Selecionar arquivos"
        className={`items-center justify-center gap-2 rounded-xl border-2 border-dashed border-neutral-300 bg-neutral-50 px-6 py-10 active:bg-neutral-100 ${
          disabled ? "opacity-60" : ""
        }`}
      >
        <Ionicons name="cloud-upload-outline" size={32} color="#6f4f28" />
        <Text className="text-center text-sm font-medium text-neutral-800">
          {abrindo ? "Abrindo..." : "Toque para selecionar arquivos"}
        </Text>
        <Text className="text-center text-xs text-neutral-500">
          Opcional • até {TAMANHO_MAXIMO_MB} MB por arquivo
        </Text>
      </Pressable>

      {aviso ? <Text className="text-xs font-medium text-red-600">{aviso}</Text> : null}

      {arquivos.length > 0 ? (
        <View className="gap-2">
          <Text className="text-xs font-medium uppercase tracking-wide text-neutral-500">
            {arquivos.length === 1 ? "1 arquivo selecionado" : `${arquivos.length} arquivos selecionados`}
          </Text>

          {arquivos.map((arquivo) => (
            <View
              key={arquivo.id}
              className="flex-row items-center gap-3 rounded-lg border border-neutral-200 bg-white px-3 py-2.5"
            >
              <Ionicons name="document-text-outline" size={20} color="#6f4f28" />

              <View className="flex-1 min-w-0">
                <Text className="text-sm text-neutral-800" numberOfLines={1}>
                  {arquivo.nome}
                </Text>
                {arquivo.tamanho !== undefined ? (
                  <Text className="text-xs text-neutral-500">{formatarTamanho(arquivo.tamanho)}</Text>
                ) : null}
              </View>

              <Pressable
                onPress={() => remover(arquivo.id)}
                disabled={disabled}
                hitSlop={8}
                accessibilityRole="button"
                accessibilityLabel={`Remover ${arquivo.nome}`}
              >
                <Ionicons name="close-circle-outline" size={22} color="#ef4444" />
              </Pressable>
            </View>
          ))}
        </View>
      ) : null}
    </View>
  );
}