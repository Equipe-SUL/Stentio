import { useState } from "react";
import { Modal, View, Text, TextInput, Pressable } from "react-native";

type Cargo = "Gestor_Projeto" | "Atendente" | "Admin" | "Financeiro";
type Status = "Ativo" | "Inativo";

export interface NovoUsuario {
  nome: string;
  email: string;
  cargo: Cargo;
  status: Status;
  senha: string;
}

interface FormularioUsuarioProps {
  visible: boolean;
  onSubmit: (usuario: NovoUsuario) => void;
  onCancel: () => void;
}

const cargos: Cargo[] = ["Gestor_Projeto", "Atendente", "Admin", "Financeiro"];
const statusOptions: Status[] = ["Ativo", "Inativo"];

export function FormularioUsuario({
  visible,
  onSubmit,
  onCancel,
}: FormularioUsuarioProps) {
  const [nome, setNome] = useState("");
  const [email, setEmail] = useState("");
  const [cargo, setCargo] = useState<Cargo>("Atendente");
  const [status, setStatus] = useState<Status>("Ativo");
  const [senha, setSenha] = useState("");

  function handleSubmit() {
    if (!nome.trim() || !email.trim() || !senha.trim()) {
      return;
    }

    onSubmit({
      nome: nome.trim(),
      email: email.trim(),
      cargo,
      status,
      senha,
    });

    setNome("");
    setEmail("");
    setCargo("Atendente");
    setStatus("Ativo");
    setSenha("");
  }

  return (
    <Modal visible={visible} transparent animationType="fade" onRequestClose={onCancel}>
      <View className="flex-1 items-center justify-center bg-black/40 px-6">
        <View className="w-full max-w-sm gap-4 rounded-2xl bg-white p-6">
          <Text className="text-lg font-semibold text-neutral-900">Novo usuário</Text>

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
            <Text className="text-sm font-medium text-neutral-700">Senha</Text>
            <TextInput
              value={senha}
              onChangeText={setSenha}
              placeholder="Digite a senha"
              secureTextEntry
              autoCapitalize="none"
              className="rounded-lg border border-neutral-300 px-4 py-3 text-neutral-900"
            />
          </View>

          <View className="flex-row justify-end gap-2 pt-2">
            <Pressable onPress={onCancel} className="rounded-lg border border-neutral-300 px-5 py-3">
              <Text className="font-medium text-neutral-700">Cancelar</Text>
            </Pressable>

            <Pressable onPress={handleSubmit} className="rounded-lg bg-neutral-900 px-5 py-3">
              <Text className="font-medium text-white">Cadastrar</Text>
            </Pressable>
          </View>
        </View>
      </View>
    </Modal>
  );
}