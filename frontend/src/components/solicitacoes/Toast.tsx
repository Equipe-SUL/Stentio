// Toast.tsx
import { useCallback, useEffect, useRef, useState } from "react";
import { Pressable, Text, View } from "react-native";
import { Ionicons } from "@expo/vector-icons";

type TipoToast = "sucesso" | "erro";

export interface ToastState {
  mensagem: string;
  tipo: TipoToast;
}

// Alert.alert não faz nada no react-native-web, então o aviso é um componente próprio.
export function useToast(duracaoMs = 4000) {
  const [toast, setToast] = useState<ToastState | null>(null);
  const timer = useRef<ReturnType<typeof setTimeout> | undefined>(undefined);

  const fechar = useCallback(() => {
    clearTimeout(timer.current);
    setToast(null);
  }, []);

  const mostrar = useCallback(
    (mensagem: string, tipo: TipoToast = "sucesso") => {
      clearTimeout(timer.current);
      setToast({ mensagem, tipo });
      timer.current = setTimeout(() => setToast(null), duracaoMs);
    },
    [duracaoMs],
  );

  useEffect(() => () => clearTimeout(timer.current), []);

  return { toast, mostrar, fechar };
}

interface ToastProps {
  toast: ToastState | null;
  onClose: () => void;
}

// Renderizar como irmão do ScrollView, dentro de um container `flex-1`.
export function Toast({ toast, onClose }: ToastProps) {
  if (!toast) return null;

  const sucesso = toast.tipo === "sucesso";

  return (
    <View className="absolute inset-x-0 top-4 z-50 items-center px-4">
      <View
        accessibilityRole="alert"
        className={`w-full max-w-md flex-row items-center gap-3 rounded-xl border px-4 py-3 shadow-lg ${
          sucesso ? "border-emerald-200 bg-emerald-50" : "border-red-200 bg-red-50"
        }`}
      >
        <Ionicons
          name={sucesso ? "checkmark-circle" : "alert-circle"}
          size={22}
          color={sucesso ? "#16a34a" : "#dc2626"}
        />
        <Text
          className={`flex-1 text-sm font-medium ${sucesso ? "text-emerald-800" : "text-red-800"}`}
        >
          {toast.mensagem}
        </Text>
        <Pressable onPress={onClose} hitSlop={8} accessibilityLabel="Fechar aviso">
          <Ionicons name="close" size={18} color="#737373" />
        </Pressable>
      </View>
    </View>
  );
}