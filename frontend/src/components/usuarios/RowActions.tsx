// RowActions.tsx
import { View, Text, Pressable } from "react-native";

interface RowActionsProps {
  onEdit?: () => void;
  onToggle?: () => void;
  toggleLabel: "Ativar" | "Desativar";
}

export function RowActions({ onEdit, onToggle, toggleLabel }: RowActionsProps) {
  return (
    <View className="flex-row justify-end gap-5">
      <Pressable onPress={onEdit}>
        <Text className="text-sm font-medium text-red-500">Editar</Text>
      </Pressable>
      <Pressable onPress={onToggle}>
        <Text className="text-sm font-medium text-red-500">{toggleLabel}</Text>
      </Pressable>
    </View>
  );
}