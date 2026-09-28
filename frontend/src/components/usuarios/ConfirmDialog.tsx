// ConfirmDialog.tsx
import { Modal, View, Text, Pressable } from "react-native";

interface ConfirmDialogProps {
  visible: boolean;
  title: string;
  message: string;
  confirmLabel?: string;
  onConfirm: () => void;
  onCancel: () => void;
}

export function ConfirmDialog({
  visible,
  title,
  message,
  confirmLabel = "Confirmar",
  onConfirm,
  onCancel,
}: ConfirmDialogProps) {
  return (
    <Modal visible={visible} transparent animationType="fade" onRequestClose={onCancel}>
      <View className="flex-1 items-center justify-center bg-black/40 px-6">
        <View className="w-full max-w-sm rounded-2xl bg-white p-6 gap-4">
          <Text className="text-lg font-bold text-neutral-900">{title}</Text>
          <Text className="text-sm text-neutral-600">{message}</Text>

          <View className="flex-row justify-end gap-3 pt-2">
            <Pressable onPress={onCancel} className="rounded-lg px-4 py-2">
              <Text className="text-sm font-medium text-neutral-600">Cancelar</Text>
            </Pressable>
            <Pressable onPress={onConfirm} className="rounded-lg bg-red-600 px-4 py-2">
              <Text className="text-sm font-medium text-white">{confirmLabel}</Text>
            </Pressable>
          </View>
        </View>
      </View>
    </Modal>
  );
}