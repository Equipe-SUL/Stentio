import type { ReactNode } from "react";
import { Ionicons } from "@expo/vector-icons";
import { useQuery } from "@tanstack/react-query";
import { Picker } from "@react-native-picker/picker";
import { Pressable, Text, TextInput, View } from "react-native";
import { api } from "../../lib/api";
import type { EntidadeAuditada } from "./types";

export type PeriodoAuditoria = "7" | "30" | "90" | "todos";

export interface FiltrosAuditoriaUi {
  busca: string;
  entidade: EntidadeAuditada | "";
  periodo: PeriodoAuditoria;
  usuarioId: string;
}

export const FILTROS_INICIAIS: FiltrosAuditoriaUi = {
  busca: "",
  entidade: "",
  periodo: "30",
  usuarioId: "",
};

interface UsuarioLista {
  id: string;
  nome: string;
  email: string;
  ativo: boolean;
}

interface Props {
  filtros: FiltrosAuditoriaUi;
  aoMudar: (filtros: FiltrosAuditoriaUi) => void;
}

function Campo({ titulo, children }: { titulo: string; children: ReactNode }) {
  return (
    <View style={{ flexGrow: 1, flexBasis: 210, minWidth: 0, gap: 7 }}>
      <Text className="text-xs font-semibold text-[#6b4d3b]">{titulo}</Text>
      {children}
    </View>
  );
}

export function AuditoriaFiltros({ filtros, aoMudar }: Props) {
  // A API do Auth já fornece os IDs necessários para o filtro do Core.
  // Somente o administrador acessa essa tela e essa listagem de usuários.
  const usuarios = useQuery({
    queryKey: ["auditoria", "usuarios"],
    queryFn: async () => {
      const { data } = await api.get<UsuarioLista[]>("/api/v1/usuarios");
      return data;
    },
    staleTime: 60_000,
    retry: 1,
  });

  return (
    <View className="mb-4 gap-4 rounded-xl border border-[#e7dcd2] bg-white p-4 md:p-5">
      <View className="flex-row flex-wrap items-center justify-between gap-3">
        <View className="flex-row items-center gap-2">
          <Ionicons name="options-outline" size={18} color="#74472e" />
          <Text className="font-serif text-base font-bold text-[#302219]">Filtros</Text>
        </View>
        <Pressable
          accessibilityRole="button"
          onPress={() => aoMudar({ ...FILTROS_INICIAIS, periodo: "todos" })}
        >
          <Text className="text-xs font-semibold text-[#9b512b]">Limpar filtros</Text>
        </Pressable>
      </View>

      <View className="flex-row flex-wrap gap-3">
        <Campo titulo="Buscar por código ou ID">
          <View className="h-11 flex-row items-center gap-2 rounded-lg border border-[#e4d8cf] px-3">
            <Ionicons name="search-outline" size={17} color="#aa9a8d" />
            <TextInput
              accessibilityLabel="Buscar pelo código do registro ou ID"
              value={filtros.busca}
              onChangeText={(busca) => aoMudar({ ...filtros, busca })}
              placeholder="Ex.: ORC-2026 ou UUID"
              placeholderTextColor="#aaa0a0"
              autoCapitalize="none"
              autoCorrect={false}
              className="h-full min-w-0 flex-1 text-sm text-[#463528]"
            />
          </View>
        </Campo>

        <Campo titulo="Área do sistema">
          <View className="h-11 justify-center overflow-hidden rounded-lg border border-[#e4d8cf]">
            <Picker
              accessibilityLabel="Filtrar área do sistema"
              selectedValue={filtros.entidade}
              onValueChange={(entidade) => aoMudar({ ...filtros, entidade: entidade as FiltrosAuditoriaUi["entidade"] })}
              style={{ height: 44, width: "100%", color: "#463528", fontSize: 13 }}
            >
              <Picker.Item label="Todas as áreas auditadas" value="" />
              <Picker.Item label="Orçamentos" value="ORCAMENTO" />
              <Picker.Item label="Ordens de Serviço" value="ORDEM_SERVICO" />
              <Picker.Item label="Etapas de OS" value="ORDEM_SERVICO_ETAPA" />
            </Picker>
          </View>
        </Campo>

        <Campo titulo="Período">
          <View className="h-11 justify-center overflow-hidden rounded-lg border border-[#e4d8cf]">
            <Picker
              accessibilityLabel="Filtrar período"
              selectedValue={filtros.periodo}
              onValueChange={(periodo) => aoMudar({ ...filtros, periodo: periodo as PeriodoAuditoria })}
              style={{ height: 44, width: "100%", color: "#463528", fontSize: 13 }}
            >
              <Picker.Item label="Últimos 7 dias" value="7" />
              <Picker.Item label="Últimos 30 dias" value="30" />
              <Picker.Item label="Últimos 90 dias" value="90" />
              <Picker.Item label="Todo o período" value="todos" />
            </Picker>
          </View>
        </Campo>

        <Campo titulo="Usuário responsável">
          <View className="h-11 justify-center overflow-hidden rounded-lg border border-[#e4d8cf]">
            <Picker
              accessibilityLabel="Filtrar usuário responsável"
              selectedValue={filtros.usuarioId}
              onValueChange={(usuarioId) => aoMudar({ ...filtros, usuarioId: String(usuarioId) })}
              style={{ height: 44, width: "100%", color: "#463528", fontSize: 13 }}
            >
              <Picker.Item label="Todos os usuários" value="" />
              {(usuarios.data ?? []).map((usuario) => (
                <Picker.Item key={usuario.id} label={`${usuario.nome}${usuario.ativo ? "" : " (inativo)"}`} value={usuario.id} />
              ))}
            </Picker>
          </View>
          {usuarios.isError ? (
            <Text className="text-xs text-[#a3523b]">Não foi possível carregar os usuários. Os demais filtros continuam disponíveis.</Text>
          ) : null}
        </Campo>
      </View>
    </View>
  );
}
