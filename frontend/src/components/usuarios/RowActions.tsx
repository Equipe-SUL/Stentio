// RowActions.tsx
import { Platform, View, Pressable } from "react-native";
import type { View as ViewRef } from "react-native";
import { Ionicons } from "@expo/vector-icons";

interface RowActionsProps {
  onEdit?: () => void;
  onToggle?: () => void;
  toggleLabel?: "Ativar" | "Desativar";
}

type ElementoWeb = { setAttribute?: (nome: string, valor: string) => void };

function legenda(texto: string) {
  if (Platform.OS !== "web") return {};
  return {
    ref: (node: ViewRef | null) => {
      (node as unknown as ElementoWeb | null)?.setAttribute?.("title", texto);
    },
  };
}

export function RowActions({ onEdit, onToggle, toggleLabel }: RowActionsProps) {
  return (
    <View className="flex-row justify-end gap-5">
      {onEdit ? (
        <Pressable {...legenda("Editar")} onPress={onEdit} accessibilityLabel="Editar" hitSlop={8}>
          <Ionicons name="create-outline" size={20} color="#ef4444" />
        </Pressable>
      ) : null}

      {onToggle && toggleLabel ? (
        <Pressable
          {...legenda(toggleLabel)}
          onPress={onToggle}
          accessibilityLabel={toggleLabel}
          hitSlop={8}
        >
          <Ionicons
            name={toggleLabel === "Ativar" ? "checkmark-circle-outline" : "close-circle-outline"}
            size={20}
            color={toggleLabel === "Ativar" ? "#16a34a" : "#ef4444"}
          />
        </Pressable>
      ) : null}
    </View>
  );
}