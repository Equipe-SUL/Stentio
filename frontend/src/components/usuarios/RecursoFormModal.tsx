// RecursoFormModal.tsx
import { useEffect, useState } from "react";
import { ActivityIndicator, Modal, View, Text, TextInput, Pressable, ScrollView } from "react-native";
import { Ionicons } from "@expo/vector-icons";

type UnidadeCobranca = "POR_PALAVRA" | "POR_HORA" | "POR_PAGINA" | "POR_PROJETO";

const UNIDADES: { value: UnidadeCobranca; label: string }[] = [
  { value: "POR_PALAVRA", label: "Por palavra" },
  { value: "POR_HORA", label: "Por hora" },
  { value: "POR_PAGINA", label: "Por página" },
  { value: "POR_PROJETO", label: "Por projeto" },
];

interface TipoServicoResumo {
  id: string;
  nome: string;
}

interface IdiomaResumo {
  id: string;
  nome: string;
  codigoIso: string;
}

interface PrecoForm {
  idiomaOrigemId: string;
  idiomaDestinoId: string;
  unidade: UnidadeCobranca;
  valor: string; // texto no form, convertido ao enviar
}

export interface RecursoFormValues {
  nome: string;
  email: string;
  telefone: string;
  tiposServicoIds: string[];
  precos: PrecoForm[];
}

interface RecursoFormModalProps {
  visible: boolean;
  title: string;
  initialValues: RecursoFormValues;
  tiposServicoDisponiveis: TipoServicoResumo[];
  idiomasDisponiveis: IdiomaResumo[];
  onSubmit: (values: RecursoFormValues) => Promise<boolean>;
  onCancel: () => void;
  onToggleStatus?: () => void; // não há exclusão real, só ativar/desativar
  toggleStatusLabel?: "Ativar" | "Desativar";
  submitLabel?: string;
  erro?: string;
}

const PRECO_VAZIO: PrecoForm = {
  idiomaOrigemId: "",
  idiomaDestinoId: "",
  unidade: "POR_PALAVRA",
  valor: "",
};

export function RecursoFormModal({
  visible,
  title,
  initialValues,
  tiposServicoDisponiveis,
  idiomasDisponiveis,
  onSubmit,
  onCancel,
  onToggleStatus,
  toggleStatusLabel,
  submitLabel = "Salvar",
  erro,
}: RecursoFormModalProps) {
  const [nome, setNome] = useState("");
  const [email, setEmail] = useState("");
  const [telefone, setTelefone] = useState("");
  const [tiposServicoIds, setTiposServicoIds] = useState<string[]>([]);
  const [precos, setPrecos] = useState<PrecoForm[]>([{ ...PRECO_VAZIO }]);
  const [enviando, setEnviando] = useState(false);
  const [erroValidacao, setErroValidacao] = useState("");

  useEffect(() => {
    if (visible) {
      setNome(initialValues.nome);
      setEmail(initialValues.email);
      setTelefone(initialValues.telefone);
      setTiposServicoIds(initialValues.tiposServicoIds);
      setPrecos(initialValues.precos.length > 0 ? initialValues.precos : [{ ...PRECO_VAZIO }]);
      setErroValidacao("");
    }
  }, [visible, initialValues]);

  function toggleTipoServico(id: string) {
    setTiposServicoIds((atual) =>
      atual.includes(id) ? atual.filter((item) => item !== id) : [...atual, id]
    );
  }

  function atualizarPreco(index: number, patch: Partial<PrecoForm>) {
    setPrecos((atual) => atual.map((preco, i) => (i === index ? { ...preco, ...patch } : preco)));
  }

  function adicionarPreco() {
    setPrecos((atual) => [...atual, { ...PRECO_VAZIO }]);
  }

  function removerPreco(index: number) {
    setPrecos((atual) => atual.filter((_, i) => i !== index));
  }

  function validar(): string | null {
    if (!nome.trim()) return "Informe o nome.";
    if (!email.trim()) return "Informe o e-mail.";
    if (tiposServicoIds.length === 0) return "Selecione ao menos um tipo de serviço.";
    if (precos.length === 0) return "Adicione ao menos um par de idioma e valor.";

    for (const preco of precos) {
      if (!preco.idiomaOrigemId || !preco.idiomaDestinoId) {
        return "Selecione idioma de origem e destino em todos os preços.";
      }
      const valorNumerico = Number(preco.valor.replace(",", "."));
      if (!preco.valor || Number.isNaN(valorNumerico) || valorNumerico <= 0) {
        return "Informe um valor válido (maior que zero) em todos os preços.";
      }
    }

    return null;
  }

  async function handleSubmit() {
    const mensagemErro = validar();
    if (mensagemErro) {
      setErroValidacao(mensagemErro);
      return;
    }
    setErroValidacao("");

    setEnviando(true);
    try {
      await onSubmit({ nome: nome.trim(), email: email.trim(), telefone: telefone.trim(), tiposServicoIds, precos });
    } finally {
      setEnviando(false);
    }
  }

  return (
    <Modal visible={visible} transparent animationType="fade" onRequestClose={onCancel}>
      <View className="flex-1 items-center justify-center bg-black/40 px-6">
        <View className="w-full max-w-lg max-h-[85%] gap-4 rounded-2xl bg-white p-6">
          <Text className="text-lg font-semibold text-neutral-900">{title}</Text>

          {(erro || erroValidacao) ? (
            <View className="rounded-lg border border-red-200 bg-red-50 p-3">
              <Text className="text-sm font-medium text-red-600">{erro || erroValidacao}</Text>
            </View>
          ) : null}

          <ScrollView className="gap-4" showsVerticalScrollIndicator={false}>
            <View className="gap-4">
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
                <Text className="text-sm font-medium text-neutral-700">Telefone</Text>
                <TextInput
                  value={telefone}
                  onChangeText={setTelefone}
                  placeholder="(11) 91234-5678"
                  keyboardType="phone-pad"
                  className="rounded-lg border border-neutral-300 px-4 py-3 text-neutral-900"
                />
              </View>

              <View className="gap-2">
                <Text className="text-sm font-medium text-neutral-700">Tipos de serviço</Text>
                <View className="flex-row flex-wrap gap-2">
                  {tiposServicoDisponiveis.map((tipo) => {
                    const selecionado = tiposServicoIds.includes(tipo.id);
                    return (
                      <Pressable
                        key={tipo.id}
                        onPress={() => toggleTipoServico(tipo.id)}
                        className={`rounded-lg border px-3 py-2 ${
                          selecionado ? "border-neutral-900 bg-neutral-900" : "border-neutral-300 bg-white"
                        }`}
                      >
                        <Text className={selecionado ? "text-white" : "text-neutral-700"}>{tipo.nome}</Text>
                      </Pressable>
                    );
                  })}
                  {tiposServicoDisponiveis.length === 0 ? (
                    <Text className="text-sm text-neutral-400">Nenhum tipo de serviço ativo cadastrado.</Text>
                  ) : null}
                </View>
              </View>

              <View className="gap-3">
                <View className="flex-row items-center justify-between">
                  <Text className="text-sm font-medium text-neutral-700">Preços por par de idioma</Text>
                  <Pressable onPress={adicionarPreco} className="flex-row items-center gap-1">
                    <Ionicons name="add-circle-outline" size={18} color="#6f4f28" />
                    <Text className="text-sm font-medium text-[#6f4f28]">Adicionar</Text>
                  </Pressable>
                </View>

                {precos.map((preco, index) => (
                  <View key={index} className="gap-3 rounded-xl border border-neutral-200 p-3">
                    <View className="flex-row items-center justify-between">
                      <Text className="text-xs font-medium uppercase text-neutral-400">
                        Par {index + 1}
                      </Text>
                      {precos.length > 1 ? (
                        <Pressable onPress={() => removerPreco(index)} hitSlop={8}>
                          <Ionicons name="trash-outline" size={16} color="#dc2626" />
                        </Pressable>
                      ) : null}
                    </View>

                    <View className="gap-2">
                      <Text className="text-xs text-neutral-500">Idioma de origem</Text>
                      <View className="flex-row flex-wrap gap-2">
                        {idiomasDisponiveis.map((idioma) => {
                          const selecionado = preco.idiomaOrigemId === idioma.id;
                          return (
                            <Pressable
                              key={idioma.id}
                              onPress={() => atualizarPreco(index, { idiomaOrigemId: idioma.id })}
                              className={`rounded-md border px-2.5 py-1.5 ${
                                selecionado ? "border-neutral-900 bg-neutral-900" : "border-neutral-300 bg-white"
                              }`}
                            >
                              <Text
                                className={`text-xs ${selecionado ? "text-white" : "text-neutral-700"}`}
                              >
                                {idioma.nome}
                              </Text>
                            </Pressable>
                          );
                        })}
                      </View>
                    </View>

                    <View className="gap-2">
                      <Text className="text-xs text-neutral-500">Idioma de destino</Text>
                      <View className="flex-row flex-wrap gap-2">
                        {idiomasDisponiveis.map((idioma) => {
                          const selecionado = preco.idiomaDestinoId === idioma.id;
                          return (
                            <Pressable
                              key={idioma.id}
                              onPress={() => atualizarPreco(index, { idiomaDestinoId: idioma.id })}
                              className={`rounded-md border px-2.5 py-1.5 ${
                                selecionado ? "border-neutral-900 bg-neutral-900" : "border-neutral-300 bg-white"
                              }`}
                            >
                              <Text
                                className={`text-xs ${selecionado ? "text-white" : "text-neutral-700"}`}
                              >
                                {idioma.nome}
                              </Text>
                            </Pressable>
                          );
                        })}
                      </View>
                    </View>

                    <View className="flex-row gap-3">
                      <View className="flex-1 gap-2">
                        <Text className="text-xs text-neutral-500">Unidade</Text>
                        <View className="flex-row flex-wrap gap-2">
                          {UNIDADES.map((unidade) => {
                            const selecionado = preco.unidade === unidade.value;
                            return (
                              <Pressable
                                key={unidade.value}
                                onPress={() => atualizarPreco(index, { unidade: unidade.value })}
                                className={`rounded-md border px-2.5 py-1.5 ${
                                  selecionado ? "border-neutral-900 bg-neutral-900" : "border-neutral-300 bg-white"
                                }`}
                              >
                                <Text className={`text-xs ${selecionado ? "text-white" : "text-neutral-700"}`}>
                                  {unidade.label}
                                </Text>
                              </Pressable>
                            );
                          })}
                        </View>
                      </View>
                    </View>

                    <View className="gap-2">
                      <Text className="text-xs text-neutral-500">Valor (R$)</Text>
                      <TextInput
                        value={preco.valor}
                        onChangeText={(texto) => atualizarPreco(index, { valor: texto })}
                        placeholder="0,00"
                        keyboardType="decimal-pad"
                        className="rounded-lg border border-neutral-300 px-4 py-3 text-neutral-900"
                      />
                    </View>
                  </View>
                ))}
              </View>
            </View>
          </ScrollView>

          <View className="flex-row items-center justify-between pt-2">
            {onToggleStatus && toggleStatusLabel ? (
              <Pressable onPress={onToggleStatus} className="flex-row items-center gap-2">
                <Ionicons
                  name={toggleStatusLabel === "Ativar" ? "checkmark-circle-outline" : "close-circle-outline"}
                  size={18}
                  color={toggleStatusLabel === "Ativar" ? "#16a34a" : "#dc2626"}
                />
                <Text
                  className={`text-sm font-medium ${
                    toggleStatusLabel === "Ativar" ? "text-emerald-600" : "text-red-600"
                  }`}
                >
                  {toggleStatusLabel}
                </Text>
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