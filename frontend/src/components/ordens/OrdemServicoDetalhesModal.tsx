import { Modal, Pressable, Text, View } from "react-native";
import { Ionicons } from "@expo/vector-icons";
import { useRouter } from "expo-router";
import { OrdemServicoDetalhesView } from "./OrdemServicoDetalhesView";
import type { OrdemServicoDetalhe } from "./types";

interface OrdemServicoDetalhesModalProps {
  ordemId: string | null;
  ordemInicial?: OrdemServicoDetalhe | null;
  onClose: () => void;
}

export function OrdemServicoDetalhesModal({
  ordemId,
  ordemInicial,
  onClose,
}: OrdemServicoDetalhesModalProps) {
  const router = useRouter();

  if (!ordemId) return null;

  const handleAbrirTelaCheia = () => {
    onClose();
    router.push(`/ordens/${ordemId}` as const);
  };

  return (
    <Modal
      visible={Boolean(ordemId)}
      transparent
      animationType="fade"
      onRequestClose={onClose}
    >
      <View className="flex-1 items-center justify-center bg-black/60 p-4">
        <View className="w-full max-w-3xl max-h-[92%] rounded-2xl bg-[#fbfaf8] shadow-2xl overflow-hidden flex-col border border-neutral-200">
          {/* Cabeçalho do modal */}
          <View className="flex-row items-center justify-between border-b border-neutral-200 px-6 py-4 bg-white">
            <View className="flex-1 mr-4">
              <Text className="text-xs uppercase font-bold text-[#8c5230] tracking-wider">
                Operacional • Ordens de Serviço
              </Text>
              <Text
                className="text-lg font-bold text-neutral-900 truncate"
                numberOfLines={1}
              >
                {ordemInicial?.codigo || "Detalhes da Ordem de Serviço"}
              </Text>
            </View>

            <View className="flex-row items-center gap-2">
              <Pressable
                onPress={handleAbrirTelaCheia}
                className="flex-row items-center gap-1.5 rounded-lg border border-neutral-300 bg-white px-3 py-1.5 active:bg-neutral-100 mr-2"
                accessibilityRole="button"
                accessibilityLabel="Abrir em página cheia"
              >
                <Ionicons name="open-outline" size={16} color="#6f4f28" />
                <Text className="text-xs font-medium text-neutral-700">
                  Tela cheia
                </Text>
              </Pressable>

              <Pressable
                onPress={onClose}
                hitSlop={8}
                accessibilityRole="button"
                accessibilityLabel="Fechar modal"
                className="rounded-lg p-1.5 text-neutral-400 hover:bg-neutral-100 active:bg-neutral-200"
              >
                <Ionicons name="close" size={22} color="#737373" />
              </Pressable>
            </View>
          </View>

          {/* Conteúdo principal com OrdemServicoDetalhesView */}
          <View className="flex-1 p-6">
            <OrdemServicoDetalhesView
              id={ordemId}
              ordemInicial={ordemInicial}
              onVoltar={onClose}
            />
          </View>

          {/* Rodapé com botão de fechar */}
          <View className="border-t border-neutral-200 px-6 py-3.5 flex-row justify-end bg-white">
            <Pressable
              onPress={onClose}
              className="rounded-lg bg-neutral-800 px-5 py-2.5 active:bg-neutral-900"
            >
              <Text className="text-sm font-medium text-white">Fechar</Text>
            </Pressable>
          </View>
        </View>
      </View>
    </Modal>
  );
}
