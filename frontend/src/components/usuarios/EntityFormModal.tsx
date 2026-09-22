// EntityFormModal.tsx
import { useEffect, useState } from "react";
import { ActivityIndicator, Modal, View, Text, TextInput, Pressable } from "react-native";
import { Ionicons } from "@expo/vector-icons";
import type { FieldDef } from "./types";

interface EntityFormModalProps<T> {
  visible: boolean;
  title: string;
  fields: FieldDef<T>[];
  initialValues: T;
  onSubmit: (values: T) => Promise<boolean>;
  onCancel: () => void;
  onDelete?: () => void; // presente só no modo edição
  submitLabel?: string;
  erro?: string;
}

export function EntityFormModal<T extends Record<string, any>>({
  visible,
  title,
  fields,
  initialValues,
  onSubmit,
  onCancel,
  onDelete,
  submitLabel = "Salvar",
  erro,
}: EntityFormModalProps<T>) {
  const [values, setValues] = useState<T>(initialValues);
  const [enviando, setEnviando] = useState(false);

  // Resincroniza sempre que o modal reabrir ou o registro alvo mudar
  useEffect(() => {
    setValues(initialValues);
  }, [initialValues, visible]);

  function setField<K extends keyof T>(key: K, value: T[K]) {
    setValues((atual) => ({ ...atual, [key]: value }));
  }

  function isValido() {
    return fields.every((field) => {
      if (field.type === "boolean") return true;
      const valor = String(values[field.key] ?? "").trim();
      if (field.type === "iso") return /^[a-z]{2,3}(-[a-zA-Z]{2,4})?$/.test(valor);
      return valor.length > 0;
    });
  }

  async function handleSubmit() {
    if (!isValido()) return;

    setEnviando(true);
    try {
      await onSubmit(values);
    } finally {
      setEnviando(false);
    }
  }

  return (
    <Modal visible={visible} transparent animationType="fade" onRequestClose={onCancel}>
      <View className="flex-1 items-center justify-center bg-black/40 px-6">
        <View className="w-full max-w-sm gap-4 rounded-2xl bg-white p-6">
          <Text className="text-lg font-semibold text-neutral-900">{title}</Text>

          {erro ? (
            <View className="rounded-lg border border-red-200 bg-red-50 p-3">
              <Text className="text-sm font-medium text-red-600">{erro}</Text>
            </View>
          ) : null}

          {fields.map((field) => (
            <View key={String(field.key)} className="gap-2">
              <Text className="text-sm font-medium text-neutral-700">{field.label}</Text>

              {field.type === "boolean" ? (
                <View className="flex-row gap-2">
                  {[true, false].map((opcao) => (
                    <Pressable
                      key={String(opcao)}
                      onPress={() => setField(field.key, opcao as T[typeof field.key])}
                      className={`rounded-lg border px-4 py-2 ${
                        Boolean(values[field.key]) === opcao
                          ? "border-neutral-900 bg-neutral-900"
                          : "border-neutral-300 bg-white"
                      }`}
                    >
                      <Text
                        className={
                          Boolean(values[field.key]) === opcao ? "text-white" : "text-neutral-700"
                        }
                      >
                        {opcao ? "Ativo" : "Inativo"}
                      </Text>
                    </Pressable>
                  ))}
                </View>
              ) : (
                <TextInput
                  value={String(values[field.key] ?? "")}
                  onChangeText={(texto) =>
                    setField(
                      field.key,
                      (field.type === "iso"
                        ? texto.toLowerCase()
                        : texto) as T[typeof field.key]
                    )
                  }
                  placeholder={field.placeholder ?? field.label}
                  autoCapitalize={field.type === "iso" ? "none" : "sentences"}
                  className="rounded-lg border border-neutral-300 px-4 py-3 text-neutral-900"
                />
              )}
            </View>
          ))}

          <View className="flex-row items-center justify-between pt-2">
            {onDelete ? (
              <Pressable
                onPress={onDelete}
                className="flex-row items-center gap-2"
                accessibilityLabel="Excluir"
              >
                <Ionicons name="trash-outline" size={18} color="#dc2626" />
                <Text className="text-sm font-medium text-red-600">Excluir</Text>
              </Pressable>
            ) : (
              <View />
            )}

            <View className="flex-row gap-2">
              <Pressable onPress={onCancel} className="rounded-lg border border-neutral-300 px-5 py-3">
                <Text className="font-medium text-neutral-700">Cancelar</Text>
              </Pressable>

              <Pressable
                onPress={handleSubmit}
                disabled={enviando}
                className="rounded-lg bg-neutral-900 px-5 py-3"
              >
                {enviando ? (
                  <ActivityIndicator color="#fff" />
                ) : (
                  <Text className="font-medium text-white">{submitLabel}</Text>
                )}
              </Pressable>
            </View>
          </View>
        </View>
      </View>
    </Modal>
  );
}