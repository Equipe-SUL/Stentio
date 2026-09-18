import { useEffect, useState } from "react";
import { ActivityIndicator, Modal, View, Text, TextInput, Pressable } from "react-native";
import { Ionicons } from "@expo/vector-icons";

type Cargo = "Gestor_Projeto" | "Atendente" | "Admin" | "Financeiro";
type Status = "Ativo" | "Inativo";

interface Usuario {
  id: string;
  nome: string;
  email: string;
  cargo: Cargo;
  status: Status;
  senha: string;
}

export interface UsuarioAtualizado {
  nome: string;
  email: string;
  cargo: Cargo;
  status: Status;
  senha: string;
}

interface FormularioEdicaoUsuarioProps {
  visible: boolean;
  usuario: Usuario | null;
  onSubmit: (dados: UsuarioAtualizado) => Promise<boolean>;
  onCancel: () => void;
  onDelete: () => void;
  erro?: string;
}

const cargos: Cargo[] = ["Gestor_Projeto", "Atendente", "Admin", "Financeiro"];
const statusOptions: Status[] = ["Ativo", "Inativo"];

export function FormularioEdicaoUsuario({
  visible,
  usuario,
  onSubmit,
  onCancel,
  onDelete,
  erro,
}: FormularioEdicaoUsuarioProps) {
  const [nome, setNome] = useState("");
  const [email, setEmail] = useState("");
  const [cargo, setCargo] = useState<Cargo>("Atendente");
  const [status, setStatus] = useState<Status>("Ativo");
  const [senha, setSenha] = useState("");
  const [enviando, setEnviando] = useState(false);

  // Preenche os campos sempre que o usuário selecionado para edição mudar
  useEffect(() => {
    if (usuario) {
      setNome(usuario.nome);
      setEmail(usuario.email);
      setCargo(usuario.cargo);
      setStatus(usuario.status);
      setSenha(usuario.senha);
    }
  }, [usuario]);

  async function handleSubmit() {
    if (!nome.trim() || !email.trim()) {
      return;
    }

    setEnviando(true);
    try {
      await onSubmit({
        nome: nome.trim(),
        email: email.trim(),
        cargo,
        status,
        senha,
      });
    } finally {
      setEnviando(false);
    }
  }

  return (
    <Modal visible={visible} transparent animationType="fade" onRequestClose={onCancel}>
      <View className="flex-1 items-center justify-center bg-black/40 px-6">
        <View className="w-full max-w-sm gap-4 rounded-2xl bg-white p-6">
          <Text className="text-lg font-semibold text-neutral-900">Editar usuário</Text>

          {erro ? (
            <View className="rounded-lg border border-red-200 bg-red-50 p-3">
              <Text className="text-sm font-medium text-red-600">{erro}</Text>
            </View>
          ) : null}

          <View className="gap-2">
            <Text className="text-sm font-medium text-neutral-700">Nome</Text>
            <TextInput
              value={nome}
              onChangeText={setNome}
              placeholder="Digite o nome"
              className="rounded-lg border border-neutral-300 px-4 py-3 text-neutral-900"
            />
          </View>

          <View className="gap-2">
            <Text className="text-sm font-medium text-neutral-700">Email</Text>
            <TextInput
              value={email}
              onChangeText={setEmail}
              placeholder="Digite o email"
              keyboardType="email-address"
              autoCapitalize="none"
              className="rounded-lg border border-neutral-300 px-4 py-3 text-neutral-900"
            />
          </View>

          <View className="gap-2">
            <Text className="text-sm font-medium text-neutral-700">Cargo</Text>
            <View className="flex-row flex-wrap gap-2">
              {cargos.map((cargoOption) => (
                <Pressable
                  key={cargoOption}
                  onPress={() => setCargo(cargoOption)}
                  className={`rounded-lg border px-3 py-2 ${
                    cargo === cargoOption
                      ? "border-neutral-900 bg-neutral-900"
                      : "border-neutral-300 bg-white"
                  }`}
                >
                  <Text className={cargo === cargoOption ? "text-white" : "text-neutral-700"}>
                    {cargoOption}
                  </Text>
                </Pressable>
              ))}
            </View>
          </View>

          <View className="gap-2">
            <Text className="text-sm font-medium text-neutral-700">Status</Text>
            <View className="flex-row gap-2">
              {statusOptions.map((statusOption) => (
                <Pressable
                  key={statusOption}
                  onPress={() => setStatus(statusOption)}
                  className={`rounded-lg border px-4 py-2 ${
                    status === statusOption
                      ? "border-neutral-900 bg-neutral-900"
                      : "border-neutral-300 bg-white"
                  }`}
                >
                  <Text className={status === statusOption ? "text-white" : "text-neutral-700"}>
                    {statusOption}
                  </Text>
                </Pressable>
              ))}
            </View>
          </View>

          <View className="gap-2">
            <Text className="text-sm font-medium text-neutral-700">
              Senha (em branco para manter)
            </Text>
            <TextInput
              value={senha}
              onChangeText={setSenha}
              placeholder="Nova senha (opcional)"
              secureTextEntry
              autoCapitalize="none"
              className="rounded-lg border border-neutral-300 px-4 py-3 text-neutral-900"
            />
          </View>

          <View className="flex-row items-center justify-between pt-2">
            <Pressable
              onPress={onDelete}
              className="flex-row items-center gap-2"
              accessibilityLabel="Excluir usuário"
            >
              <Ionicons name="trash-outline" size={18} color="#dc2626" />
              <Text className="text-sm font-medium text-red-600">Excluir usuário</Text>
            </Pressable>

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
                  <Text className="font-medium text-white">Salvar</Text>
                )}
              </Pressable>
            </View>
          </View>
        </View>
      </View>
    </Modal>
  );
}