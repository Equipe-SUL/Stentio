import React, { useEffect, useState } from "react";
import { ActivityIndicator, Pressable, ScrollView, Text, TextInput, View } from "react-native";
import { Picker } from "@react-native-picker/picker";
import { Ionicons } from "@expo/vector-icons";
import { Redirect, router, useLocalSearchParams } from "expo-router";
import { useSession } from "../../lib/session";
import { coreApi } from "../../lib/api";
import {
  carregarCatalogosCadastroRecurso,
  criarContaRecurso,
  enviarPerfilRecurso,
  errosDeCadastro,
  perfilRecursoAusente,
  type OpcaoCatalogoRecurso,
  type PrecoCadastroRecurso,
  type UnidadePrecoRecurso,
} from "./cadastroRecursoApi";

type EtapaCadastro = "conta" | "perfil";
type CamposConta = { nome: string; email: string; senha: string; telefone: string };
type PrecoForm = Omit<PrecoCadastroRecurso, "valor"> & { valor: string };

const UNIDADES: { value: UnidadePrecoRecurso; label: string }[] = [
  { value: "PALAVRA", label: "Por palavra" },
  { value: "HORA", label: "Por hora" },
  { value: "PAGINA", label: "Por página" },
];

const PRECO_VAZIO: PrecoForm = {
  idiomaOrigemId: "",
  idiomaDestinoId: "",
  unidade: "PALAVRA",
  valor: "",
};

function Campo({
  label,
  value,
  onChangeText,
  erro,
  ...props
}: {
  label: string;
  value: string;
  onChangeText: (value: string) => void;
  erro?: string;
  placeholder?: string;
  secureTextEntry?: boolean;
  keyboardType?: "default" | "email-address" | "phone-pad";
  autoCapitalize?: "none" | "sentences" | "words";
}) {
  return (
    <View className="gap-1.5">
      <Text className="text-sm font-medium text-neutral-700">{label}</Text>
      <TextInput
        value={value}
        onChangeText={onChangeText}
        placeholder={props.placeholder}
        placeholderTextColor="#a1a1aa"
        secureTextEntry={props.secureTextEntry}
        keyboardType={props.keyboardType}
        autoCapitalize={props.autoCapitalize}
        className={`rounded-xl border bg-white px-4 py-3.5 text-neutral-900 ${erro ? "border-red-400" : "border-neutral-300"}`}
      />
      {erro ? <Text className="text-xs text-red-600">{erro}</Text> : null}
    </View>
  );
}

function SeletorCatalogo({
  label,
  value,
  options,
  onChange,
}: {
  label: string;
  value: string;
  options: OpcaoCatalogoRecurso[];
  onChange: (value: string) => void;
}) {
  return (
    <View className="flex-1 gap-1.5">
      <Text className="text-xs font-medium text-neutral-600">{label}</Text>
      <View className="min-w-0 overflow-hidden rounded-lg border border-neutral-300 bg-white">
        <Picker selectedValue={value} onValueChange={(novo) => onChange(String(novo))}>
          <Picker.Item label={`Selecione ${label.toLowerCase()}`} value="" />
          {options.map((option) => (
            <Picker.Item key={option.id} label={option.nome} value={option.id} />
          ))}
        </Picker>
      </View>
    </View>
  );
}

export function CadastroRecursoScreen() {
  const { completarPerfil } = useLocalSearchParams<{ completarPerfil?: string }>();
  const { usuario, carregando, entrar } = useSession();
  const [etapa, setEtapa] = useState<EtapaCadastro>(completarPerfil ? "perfil" : "conta");
  const [conta, setConta] = useState<CamposConta>({ nome: "", email: "", senha: "", telefone: "" });
  const [tipos, setTipos] = useState<OpcaoCatalogoRecurso[]>([]);
  const [idiomas, setIdiomas] = useState<OpcaoCatalogoRecurso[]>([]);
  const [tiposSelecionados, setTiposSelecionados] = useState<string[]>([]);
  const [precos, setPrecos] = useState<PrecoForm[]>([{ ...PRECO_VAZIO }]);
  const [camposErro, setCamposErro] = useState<Record<string, string>>({});
  const [erroGeral, setErroGeral] = useState("");
  const [carregandoCatalogos, setCarregandoCatalogos] = useState(Boolean(completarPerfil));
  const [enviando, setEnviando] = useState(false);
  const [perfilValidado, setPerfilValidado] = useState(Boolean(completarPerfil));

  useEffect(() => {
    if (carregando) return;

    if (completarPerfil) {
      if (!usuario) {
        router.replace("/");
      } else if (usuario.role !== "RECURSO") {
        router.replace("/" as never);
      }
      return;
    }

    if (usuario) {
      if (usuario.role !== "RECURSO") {
        router.replace("/" as never);
        return;
      }
      coreApi
        .get("/api/v1/recurso/perfil")
        .then(() => router.replace("/portal"))
        .catch((error) => {
          if (perfilRecursoAusente(error)) {
            setCarregandoCatalogos(true);
            setEtapa("perfil");
          } else {
            setErroGeral("Não foi possível validar seu perfil. Tente novamente.");
          }
          setPerfilValidado(true);
        })
        .finally(() => setPerfilValidado(true));
    }
  }, [carregando, completarPerfil, usuario]);

  useEffect(() => {
    if (etapa !== "perfil" || usuario?.role !== "RECURSO") return;
    let ativo = true;
    carregarCatalogosCadastroRecurso()
      .then((catalogos) => {
        if (!ativo) return;
        setTipos(catalogos.tiposServico.filter((tipo) => tipo.id && tipo.nome));
        setIdiomas(catalogos.idiomas.filter((idioma) => idioma.id && idioma.nome));
      })
      .catch((error) => {
        if (ativo) setErroGeral(errosDeCadastro(error).geral);
      })
      .finally(() => {
        if (ativo) setCarregandoCatalogos(false);
      });
    return () => {
      ativo = false;
    };
  }, [etapa, usuario?.role]);

  if (completarPerfil && !carregando && !usuario) return <Redirect href="/" />;
  const aguardandoPerfilExistente = usuario?.role === "RECURSO" && !completarPerfil && !perfilValidado;
  if (aguardandoPerfilExistente || (completarPerfil && carregando)) {
    return (
      <View className="flex-1 items-center justify-center bg-neutral-50">
        <ActivityIndicator size="large" color="#8c5230" />
      </View>
    );
  }

  function atualizarConta(chave: keyof CamposConta, valor: string) {
    setConta((atual) => ({ ...atual, [chave]: valor }));
    setCamposErro((atual) => ({ ...atual, [chave]: "" }));
  }

  function atualizarPreco(index: number, patch: Partial<PrecoForm>) {
    setPrecos((atual) => atual.map((preco, i) => (i === index ? { ...preco, ...patch } : preco)));
  }

  function validarConta(): boolean {
    const erros: Record<string, string> = {};
    if (conta.nome.trim().length < 3) erros.nome = "Informe seu nome completo (mínimo 3 caracteres).";
    if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(conta.email.trim())) erros.email = "Informe um e-mail válido.";
    if (conta.senha.length < 8 || !/\d/.test(conta.senha) || !/[@#$%^&+=!?*]/.test(conta.senha)) {
      erros.senha = "Use ao menos 8 caracteres, um número e um caractere especial.";
    }
    if (!/^[0-9+()\-\s]{8,20}$/.test(conta.telefone.trim())) erros.telefone = "Informe um telefone válido.";
    setCamposErro(erros);
    return Object.keys(erros).length === 0;
  }

  async function criarConta() {
    setErroGeral("");
    if (!validarConta()) return;
    setEnviando(true);
    try {
      // O telefone pertence ao perfil Core; o Auth recebe somente nome, e-mail e senha.
      await criarContaRecurso({ nome: conta.nome.trim(), email: conta.email.trim(), senha: conta.senha });
      const perfil = await entrar(conta.email.trim(), conta.senha, true);
      if (perfil.role !== "RECURSO") {
        setErroGeral("A conta criada não recebeu o perfil de recurso. Entre em contato com o suporte.");
        return;
      }
      setCarregandoCatalogos(true);
      setEtapa("perfil");
    } catch (error) {
      const falha = errosDeCadastro(error);
      setCamposErro((atuais) => ({ ...atuais, ...falha.campos }));
      setErroGeral(falha.geral);
    } finally {
      setEnviando(false);
    }
  }

  async function concluirPerfil() {
    setErroGeral("");
    const erros: Record<string, string> = {};
    if (!/^[0-9+()\-\s]{8,20}$/.test(conta.telefone.trim())) {
      erros.telefone = "Informe um telefone válido.";
    }
    if (tiposSelecionados.length === 0) erros.tiposServicoIds = "Selecione ao menos um tipo de serviço.";
    if (precos.length === 0) erros.precos = "Adicione ao menos um par de idioma e valor.";
    precos.forEach((preco, index) => {
      if (!preco.idiomaOrigemId) erros[`precos.${index}.idiomaOrigemId`] = "Selecione o idioma de origem.";
      if (!preco.idiomaDestinoId) erros[`precos.${index}.idiomaDestinoId`] = "Selecione o idioma de destino.";
      const valor = Number(preco.valor.replace(",", "."));
      if (!preco.valor || !Number.isFinite(valor) || valor <= 0) erros[`precos.${index}.valor`] = "O valor deve ser maior que zero.";
    });
    setCamposErro(erros);
    if (Object.keys(erros).length) return;

    setEnviando(true);
    try {
      await enviarPerfilRecurso({
        nome: conta.nome.trim() || usuario?.nome || "",
        email: conta.email.trim() || usuario?.email || "",
        telefone: conta.telefone.trim(),
        tiposServicoIds: tiposSelecionados,
        precos: precos.map((preco) => ({
          idiomaOrigemId: preco.idiomaOrigemId,
          idiomaDestinoId: preco.idiomaDestinoId,
          unidade: preco.unidade,
          valor: Number(preco.valor.replace(",", ".")),
        })),
      });
      router.replace("/portal");
    } catch (error) {
      const falha = errosDeCadastro(error);
      setCamposErro((atuais) => ({ ...atuais, ...falha.campos }));
      setErroGeral(falha.geral);
    } finally {
      setEnviando(false);
    }
  }

  const perfilExistente = etapa === "perfil" && usuario?.role === "RECURSO";

  return (
    <ScrollView className="flex-1 bg-neutral-50" contentContainerClassName="min-h-full items-center justify-center p-5 py-10">
      <View className="w-full max-w-2xl gap-6 rounded-2xl border border-neutral-200 bg-white p-6 shadow-sm md:p-9">
        <View className="gap-2">
          <Text className="text-sm font-semibold uppercase tracking-widest text-[#8c5230]">Stentio · Profissionais</Text>
          <Text className="text-2xl font-bold text-neutral-900">
            {etapa === "conta" ? "Crie sua conta" : "Complete seu perfil profissional"}
          </Text>
          <Text className="text-sm leading-6 text-neutral-600">
            {etapa === "conta"
              ? "Cadastre sua conta de acesso. Em seguida, informe seus serviços, idiomas e valores."
              : "Escolha os serviços que oferece e informe ao menos um par de idiomas com seu valor."}
          </Text>
        </View>

        <View className="flex-row items-center gap-2">
          {(["conta", "perfil"] as EtapaCadastro[]).map((nomeEtapa, index) => {
            const ativa = etapa === nomeEtapa;
            const concluida = etapa === "perfil" && nomeEtapa === "conta";
            return (
              <View key={nomeEtapa} className="flex-1 flex-row items-center gap-2">
                <View className={`h-8 w-8 items-center justify-center rounded-full ${ativa || concluida ? "bg-[#8c5230]" : "bg-neutral-200"}`}>
                  {concluida ? <Ionicons name="checkmark" size={17} color="#fff" /> : <Text className={ativa ? "font-bold text-white" : "font-semibold text-neutral-500"}>{index + 1}</Text>}
                </View>
                <Text className={`text-xs font-medium ${ativa ? "text-neutral-900" : "text-neutral-500"}`}>{nomeEtapa === "conta" ? "Conta" : "Competências"}</Text>
                {index === 0 ? <View className="h-px flex-1 bg-neutral-200" /> : null}
              </View>
            );
          })}
        </View>

        {erroGeral ? (
          <View className="flex-row gap-2 rounded-xl border border-red-200 bg-red-50 p-3">
            <Ionicons name="alert-circle-outline" size={19} color="#dc2626" />
            <Text accessibilityRole="alert" className="flex-1 text-sm text-red-700">{erroGeral}</Text>
          </View>
        ) : null}

        {etapa === "conta" ? (
          <View className="gap-4">
            <Campo label="Nome completo" value={conta.nome} onChangeText={(value) => atualizarConta("nome", value)} erro={camposErro.nome} placeholder="Seu nome" autoCapitalize="words" />
            <Campo label="E-mail" value={conta.email} onChangeText={(value) => atualizarConta("email", value)} erro={camposErro.email} placeholder="voce@email.com" keyboardType="email-address" autoCapitalize="none" />
            <Campo label="Senha" value={conta.senha} onChangeText={(value) => atualizarConta("senha", value)} erro={camposErro.senha} placeholder="Mínimo 8 caracteres" secureTextEntry />
            <Campo label="Telefone" value={conta.telefone} onChangeText={(value) => atualizarConta("telefone", value)} erro={camposErro.telefone} placeholder="(11) 99999-0000" keyboardType="phone-pad" />
            <Pressable disabled={enviando} onPress={() => void criarConta()} className="mt-2 min-h-12 flex-row items-center justify-center gap-2 rounded-xl bg-[#8c5230] px-5 py-3">
              {enviando ? <ActivityIndicator color="#fff" /> : <Text className="font-semibold text-white">Criar conta e continuar</Text>}
            </Pressable>
            <Pressable onPress={() => router.replace("/")} className="items-center py-2">
              <Text className="text-sm text-neutral-600">Já tem conta? <Text className="font-semibold text-[#8c5230]">Entrar</Text></Text>
            </Pressable>
          </View>
        ) : perfilExistente ? (
          <View className="gap-5">
            <View className="rounded-xl bg-neutral-50 p-4">
              <Text className="font-semibold text-neutral-900">{conta.nome || usuario?.nome}</Text>
              <Text className="mt-1 text-sm text-neutral-600">{conta.email || usuario?.email}</Text>
            </View>
            <Campo label="Telefone" value={conta.telefone} onChangeText={(value) => atualizarConta("telefone", value)} erro={camposErro.telefone} placeholder="(11) 99999-0000" keyboardType="phone-pad" />
            {carregandoCatalogos ? (
              <View className="items-center gap-2 py-8"><ActivityIndicator color="#8c5230" /><Text className="text-sm text-neutral-500">Carregando serviços e idiomas...</Text></View>
            ) : (
              <>
                <View className="gap-2">
                  <Text className="text-sm font-semibold text-neutral-800">Tipos de serviço</Text>
                  <View className="flex-row flex-wrap gap-2">
                    {tipos.map((tipo) => {
                      const marcado = tiposSelecionados.includes(tipo.id);
                      return (
                        <Pressable key={tipo.id} onPress={() => setTiposSelecionados((atuais) => marcado ? atuais.filter((id) => id !== tipo.id) : [...atuais, tipo.id])} className={`rounded-lg border px-3 py-2 ${marcado ? "border-[#8c5230] bg-[#8c5230]" : "border-neutral-300 bg-white"}`}>
                          <Text className={`text-sm ${marcado ? "font-medium text-white" : "text-neutral-700"}`}>{tipo.nome}</Text>
                        </Pressable>
                      );
                    })}
                    {tipos.length === 0 ? <Text className="text-sm text-neutral-500">Nenhum tipo de serviço disponível.</Text> : null}
                  </View>
                  {camposErro.tiposServicoIds ? <Text className="text-xs text-red-600">{camposErro.tiposServicoIds}</Text> : null}
                </View>

                <View className="gap-3">
                  <View className="flex-row items-center justify-between">
                    <Text className="text-sm font-semibold text-neutral-800">Pares de idiomas e valores</Text>
                    <Pressable onPress={() => setPrecos((atuais) => [...atuais, { ...PRECO_VAZIO }])} className="flex-row items-center gap-1">
                      <Ionicons name="add-circle-outline" size={18} color="#8c5230" />
                      <Text className="text-sm font-medium text-[#8c5230]">Adicionar par</Text>
                    </Pressable>
                  </View>
                  {precos.map((preco, index) => (
                    <View key={index} className="gap-3 rounded-xl border border-neutral-200 p-3">
                      <View className="flex-row items-center justify-between">
                        <Text className="text-xs font-semibold uppercase text-neutral-500">Par {index + 1}</Text>
                        {precos.length > 1 ? <Pressable accessibilityLabel={`Remover par ${index + 1}`} onPress={() => setPrecos((atuais) => atuais.filter((_, i) => i !== index))}><Ionicons name="trash-outline" size={17} color="#dc2626" /></Pressable> : null}
                      </View>
                      <View className="flex-row gap-3">
                        <SeletorCatalogo label="idioma de origem" value={preco.idiomaOrigemId} options={idiomas} onChange={(value) => atualizarPreco(index, { idiomaOrigemId: value })} />
                        <SeletorCatalogo label="idioma de destino" value={preco.idiomaDestinoId} options={idiomas} onChange={(value) => atualizarPreco(index, { idiomaDestinoId: value })} />
                      </View>
                      {(camposErro[`precos.${index}.idiomaOrigemId`] || camposErro[`precos.${index}.idiomaDestinoId`]) ? <Text className="text-xs text-red-600">{camposErro[`precos.${index}.idiomaOrigemId`] || camposErro[`precos.${index}.idiomaDestinoId`]}</Text> : null}
                      <View className="flex-row items-end gap-3">
                        <View className="flex-1 gap-1.5">
                          <Text className="text-xs font-medium text-neutral-600">Unidade</Text>
                          <View className="overflow-hidden rounded-lg border border-neutral-300 bg-white">
                            <Picker selectedValue={preco.unidade} onValueChange={(value) => atualizarPreco(index, { unidade: value as UnidadePrecoRecurso })}>
                              {UNIDADES.map((unidade) => <Picker.Item key={unidade.value} label={unidade.label} value={unidade.value} />)}
                            </Picker>
                          </View>
                        </View>
                        <View className="flex-1 gap-1.5">
                          <Text className="text-xs font-medium text-neutral-600">Valor (R$)</Text>
                          <TextInput value={preco.valor} onChangeText={(value) => atualizarPreco(index, { valor: value })} keyboardType="decimal-pad" placeholder="0,00" className="rounded-lg border border-neutral-300 px-3 py-3 text-neutral-900" />
                        </View>
                      </View>
                      {camposErro[`precos.${index}.valor`] ? <Text className="text-xs text-red-600">{camposErro[`precos.${index}.valor`]}</Text> : null}
                    </View>
                  ))}
                  {camposErro.precos ? <Text className="text-xs text-red-600">{camposErro.precos}</Text> : null}
                </View>

                <View className="flex-row gap-3">
                  {!perfilExistente ? <Pressable onPress={() => setEtapa("conta")} className="flex-1 rounded-xl border border-neutral-300 px-4 py-3"><Text className="text-center font-medium text-neutral-700">Voltar</Text></Pressable> : null}
                  <Pressable disabled={enviando || carregandoCatalogos} onPress={() => void concluirPerfil()} className="flex-1 min-h-12 flex-row items-center justify-center gap-2 rounded-xl bg-[#8c5230] px-4 py-3">
                    {enviando ? <ActivityIndicator color="#fff" /> : <Text className="font-semibold text-white">Concluir cadastro</Text>}
                  </Pressable>
                </View>
              </>
            )}
          </View>
        ) : (
          <View className="items-center gap-3 py-8"><ActivityIndicator color="#8c5230" /><Text className="text-sm text-neutral-500">Validando sua sessão...</Text></View>
        )}
      </View>
    </ScrollView>
  );
}
