import React, { useEffect, useState } from "react";
import {
  ActivityIndicator,
  Pressable,
  ScrollView,
  Text,
  TextInput,
  TouchableOpacity,
  useWindowDimensions,
  View,
} from "react-native";
import { Controller, useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { Ionicons } from "@expo/vector-icons";
import { router, type Href } from "expo-router";

import { ArquivosField } from "../components/solicitacoes/ArquivosField";
import { SelectField } from "../components/solicitacoes/SelectField";
import type { ArquivoSelecionado } from "../components/solicitacoes/types";
import {
  DESCRICAO_MAX,
  MENSAGEM_IDIOMAS_IGUAIS,
  VALORES_INICIAIS_PUBLICO,
  solicitacaoPublicaSchema,
  type SolicitacaoPublicaFormInput,
} from "../lib/solicitacaoPublicaSchema";
import {
  carregarOpcoesPublicas,
  enviarSolicitacaoPublica,
  extrairErrosPublicos,
  formatarCpfCnpjInput,
  formatarTelefoneInput,
  getApiErrorMessage,
  type CategoriaPublicaOpcao,
  type IdiomaPublicoOpcao,
  type SolicitacaoPublicaCriadaResponse,
} from "../lib/solicitacaoPublicaService";

const LARGURA_IDIOMAS_LADO_A_LADO = 540;

function SecaoCard({
  titulo,
  subtitulo,
  children,
}: {
  titulo: string;
  subtitulo?: string;
  children: React.ReactNode;
}) {
  return (
    <View className="gap-5 rounded-2xl border border-zinc-200 bg-white p-6 md:p-8 shadow-sm">
      <View>
        <Text className="text-xl font-serif font-bold text-zinc-900">{titulo}</Text>
        {subtitulo ? <Text className="text-sm text-zinc-500 mt-1">{subtitulo}</Text> : null}
      </View>
      {children}
    </View>
  );
}

export default function SolicitacaoPublicaScreen() {
  const { width: larguraJanela } = useWindowDimensions();

  const [categorias, setCategorias] = useState<CategoriaPublicaOpcao[]>([]);
  const [idiomas, setIdiomas] = useState<IdiomaPublicoOpcao[]>([]);
  const [carregandoOpcoes, setCarregandoOpcoes] = useState(true);
  const [erroOpcoes, setErroOpcoes] = useState("");

  const [arquivos, setArquivos] = useState<ArquivoSelecionado[]>([]);
  const [erroEnvio, setErroEnvio] = useState("");
  const [solicitacaoSucesso, setSolicitacaoSucesso] =
    useState<SolicitacaoPublicaCriadaResponse | null>(null);

  const {
    control,
    handleSubmit,
    watch,
    reset,
    setValue,
    setError,
    formState: { errors, isValid, isSubmitting },
  } = useForm<SolicitacaoPublicaFormInput>({
    resolver: zodResolver(solicitacaoPublicaSchema),
    defaultValues: VALORES_INICIAIS_PUBLICO,
    mode: "onChange",
  });

  const idiomaOrigemId = watch("idiomaOrigemId");
  const idiomaDestinoId = watch("idiomaDestinoId");
  const descricao = watch("descricao") ?? "";

  const idiomasIguais =
    Boolean(idiomaOrigemId) &&
    Boolean(idiomaDestinoId) &&
    idiomaOrigemId === idiomaDestinoId;

  const idiomasLadoALado = larguraJanela >= LARGURA_IDIOMAS_LADO_A_LADO;

  async function carregarDadosIniciais() {
    setCarregandoOpcoes(true);
    setErroOpcoes("");
    try {
      const dados = await carregarOpcoesPublicas();
      setCategorias(dados.categoriasProjeto);
      setIdiomas(dados.idiomas);
    } catch (err) {
      setErroOpcoes(
        "Não foi possível carregar as opções disponíveis. Verifique sua conexão e tente novamente."
      );
    } finally {
      setCarregandoOpcoes(false);
    }
  }

  useEffect(() => {
    carregarDadosIniciais();
  }, []);

  const opcoesCategoria = categorias.map((cat) => ({
    value: cat.id,
    label: cat.nome,
  }));

  const opcoesIdioma = idiomas.map((idioma) => ({
    value: idioma.id,
    label: `${idioma.nome} (${idioma.codigoIso.toUpperCase()})`,
  }));

  const podeRegistrar = isValid && !idiomasIguais && !isSubmitting && !carregandoOpcoes;

  const onSubmit = handleSubmit(async (values) => {
    setErroEnvio("");
    try {
      const response = await enviarSolicitacaoPublica(values, arquivos);
      setSolicitacaoSucesso(response);
    } catch (error) {
      const errosBack = extrairErrosPublicos(error);
      if (errosBack.length > 0) {
        errosBack.forEach((item) => {
          if (item.campo in VALORES_INICIAIS_PUBLICO) {
            setError(item.campo as keyof SolicitacaoPublicaFormInput, {
              type: "server",
              message: item.mensagem,
            });
          }
        });
        const semCampo = errosBack.filter(
          (item) => !(item.campo in VALORES_INICIAIS_PUBLICO)
        );
        if (semCampo.length > 0) {
          setErroEnvio(semCampo.map((e) => e.mensagem).join(". "));
        }
      } else {
        setErroEnvio(getApiErrorMessage(error));
      }
    }
  });

  function reiniciarFormulario() {
    reset(VALORES_INICIAIS_PUBLICO);
    setArquivos([]);
    setErroEnvio("");
    setSolicitacaoSucesso(null);
  }

  return (
    <View className="flex-1 bg-[#fbfaf8]">
      {/* Barra de Navegação Superior Pública */}
      <View className="border-b border-zinc-200 bg-white">
        <View className="mx-auto flex-row w-full max-w-5xl items-center justify-between px-6 py-4">
          <TouchableOpacity
            onPress={() => router.push("/" as Href)}
            className="flex-row items-center gap-2 active:opacity-70"
          >
            <Text className="text-2xl font-bold tracking-widest text-[#8c5230]">
              STENTIO
            </Text>
            <View className="hidden sm:flex rounded-full bg-orange-100 px-2.5 py-0.5">
              <Text className="text-xs font-semibold text-[#8c5230]">AUTOATENDIMENTO</Text>
            </View>
          </TouchableOpacity>

          <TouchableOpacity
            onPress={() => router.push("/" as Href)}
            className="flex-row items-center gap-1.5 rounded-xl border border-zinc-200 px-4 py-2 hover:bg-zinc-50 active:bg-zinc-100"
          >
            <Ionicons name="arrow-back-outline" size={16} color="#8c5230" />
            <Text className="text-sm font-semibold text-[#8c5230]">Já possui login? Entrar</Text>
          </TouchableOpacity>
        </View>
      </View>

      <ScrollView className="flex-1" contentContainerStyle={{ flexGrow: 1, paddingBottom: 60 }}>
        <View className="mx-auto w-full max-w-3xl px-4 py-8 md:px-8">
          {solicitacaoSucesso ? (
            /* Tela de Confirmação de Sucesso */
            <View className="items-center justify-center rounded-3xl border border-zinc-200 bg-white p-8 md:p-12 shadow-md shadow-orange-900/10">
              <View className="mb-6 h-20 w-20 items-center justify-center rounded-full bg-green-100">
                <Ionicons name="checkmark-circle" size={56} color="#15803d" />
              </View>

              <Text className="mb-3 text-center text-2xl md:text-3xl font-serif font-bold text-zinc-900">
                Solicitação enviada com sucesso!
              </Text>

              <Text className="mb-6 text-center text-base text-zinc-600 max-w-xl">
                Recebemos sua solicitação de serviço de tradução. Nossa equipe de
                atendimento foi notificada e já está preparando o orçamento sob medida.
              </Text>

              <View className="mb-8 w-full max-w-md rounded-2xl border border-zinc-200 bg-zinc-50 p-5 gap-3">
                <View className="flex-row items-center justify-between">
                  <Text className="text-xs uppercase tracking-wider text-zinc-500 font-semibold">
                    Status da Solicitação
                  </Text>
                  <View className="rounded-full bg-amber-100 px-3 py-1">
                    <Text className="text-xs font-bold text-amber-800">
                      {solicitacaoSucesso.status ?? "PENDENTE_ORCAMENTO"}
                    </Text>
                  </View>
                </View>

                {solicitacaoSucesso.id ? (
                  <View className="border-t border-zinc-200 pt-3">
                    <Text className="text-xs uppercase tracking-wider text-zinc-500 font-semibold mb-1">
                      Identificador / Protocolo
                    </Text>
                    <Text className="text-xs md:text-sm font-mono text-zinc-800 select-all font-semibold">
                      {solicitacaoSucesso.id}
                    </Text>
                  </View>
                ) : null}

                <View className="border-t border-zinc-200 pt-3">
                  <Text className="text-xs text-zinc-500">
                    Você receberá uma cópia e o link para aprovação da proposta no e-mail informado.
                  </Text>
                </View>
              </View>

              <View className="flex-col sm:flex-row gap-3 w-full max-w-md">
                <TouchableOpacity
                  onPress={reiniciarFormulario}
                  className="flex-1 rounded-2xl border border-[#8c5230] py-3.5 items-center justify-center active:bg-orange-50"
                >
                  <Text className="font-semibold text-[#8c5230]">Nova Solicitação</Text>
                </TouchableOpacity>

                <TouchableOpacity
                  onPress={() => router.push("/" as Href)}
                  className="flex-1 rounded-2xl bg-[#8c5230] py-3.5 items-center justify-center shadow-sm shadow-orange-900/20 active:opacity-85"
                >
                  <Text className="font-semibold text-white">Voltar ao Início</Text>
                </TouchableOpacity>
              </View>
            </View>
          ) : (
            /* Formulário Público de Solicitação */
            <View className="gap-8">
              {/* Cabeçalho do formulário */}
              <View className="gap-2">
                <View className="flex-row items-center gap-2">
                  <View className="h-2 w-2 rounded-full bg-[#8c5230]" />
                  <Text className="text-xs font-bold uppercase tracking-widest text-[#8c5230]">
                    Solicitação Aberta ao Cliente
                  </Text>
                </View>
                <Text className="text-3xl md:text-4xl font-serif font-bold text-zinc-900">
                  Solicite um Orçamento de Tradução
                </Text>
                <Text className="text-base text-zinc-600">
                  Preencha os dados da sua empresa e a descrição da demanda. Nossa equipe de
                  especialistas irá analisar o material e elaborar uma proposta comercial sem compromisso.
                </Text>
              </View>

              {erroOpcoes ? (
                <View className="items-center justify-center gap-3 rounded-2xl border border-red-200 bg-red-50 p-6">
                  <Text className="text-center text-sm font-medium text-red-600">
                    {erroOpcoes}
                  </Text>
                  <TouchableOpacity
                    onPress={carregarDadosIniciais}
                    className="rounded-xl border border-red-300 bg-white px-4 py-2 active:bg-red-100"
                  >
                    <Text className="text-sm font-semibold text-red-700">Tentar novamente</Text>
                  </TouchableOpacity>
                </View>
              ) : null}

              {carregandoOpcoes ? (
                <View className="items-center justify-center gap-3 py-20">
                  <ActivityIndicator size="large" color="#8c5230" />
                  <Text className="text-sm text-zinc-500">
                    Carregando opções de idiomas e categorias...
                  </Text>
                </View>
              ) : (
                <View className="gap-6">
                  {/* Seção 1: Dados da Empresa e Contato */}
                  <SecaoCard
                    titulo="Dados da Empresa e Representante"
                    subtitulo="Informe os dados para identificação e emissão do orçamento."
                  >
                    <View className="gap-4">
                      {/* Nome da Empresa */}
                      <Controller
                        control={control}
                        name="nomeEmpresa"
                        render={({ field }) => (
                          <View className="gap-1.5">
                            <Text className="text-sm font-medium text-zinc-700">
                              Nome da Empresa <Text className="text-red-500">*</Text>
                            </Text>
                            <TextInput
                              value={field.value}
                              onChangeText={field.onChange}
                              onBlur={field.onBlur}
                              placeholder="Razão Social ou Nome Fantasia"
                              placeholderTextColor="#a1a1aa"
                              maxLength={150}
                              className={`rounded-xl border px-4 py-3 text-base text-zinc-900 bg-zinc-50 ${
                                errors.nomeEmpresa ? "border-red-400 bg-red-50/20" : "border-zinc-300"
                              }`}
                            />
                            {errors.nomeEmpresa ? (
                              <Text className="text-xs font-medium text-red-600">
                                {errors.nomeEmpresa.message}
                              </Text>
                            ) : null}
                          </View>
                        )}
                      />

                      {/* CPF ou CNPJ */}
                      <Controller
                        control={control}
                        name="cpfCnpj"
                        render={({ field }) => (
                          <View className="gap-1.5">
                            <Text className="text-sm font-medium text-zinc-700">
                              CPF ou CNPJ <Text className="text-red-500">*</Text>
                            </Text>
                            <TextInput
                              value={field.value}
                              onChangeText={(texto) => {
                                const formatado = formatarCpfCnpjInput(texto);
                                field.onChange(formatado);
                              }}
                              onBlur={field.onBlur}
                              placeholder="000.000.000-00 ou 00.000.000/0000-00"
                              placeholderTextColor="#a1a1aa"
                              maxLength={18}
                              keyboardType="numeric"
                              className={`rounded-xl border px-4 py-3 text-base text-zinc-900 bg-zinc-50 ${
                                errors.cpfCnpj ? "border-red-400 bg-red-50/20" : "border-zinc-300"
                              }`}
                            />
                            {errors.cpfCnpj ? (
                              <Text className="text-xs font-medium text-red-600">
                                {errors.cpfCnpj.message}
                              </Text>
                            ) : (
                              <Text className="text-xs text-zinc-400">
                                Se sua empresa já possui cadastro, a solicitação será vinculada automaticamente.
                              </Text>
                            )}
                          </View>
                        )}
                      />

                      {/* Nome do Representante */}
                      <Controller
                        control={control}
                        name="nomeRepresentante"
                        render={({ field }) => (
                          <View className="gap-1.5">
                            <Text className="text-sm font-medium text-zinc-700">
                              Nome do Representante / Solicitante <Text className="text-red-500">*</Text>
                            </Text>
                            <TextInput
                              value={field.value}
                              onChangeText={field.onChange}
                              onBlur={field.onBlur}
                              placeholder="Nome completo do responsável"
                              placeholderTextColor="#a1a1aa"
                              maxLength={100}
                              className={`rounded-xl border px-4 py-3 text-base text-zinc-900 bg-zinc-50 ${
                                errors.nomeRepresentante
                                  ? "border-red-400 bg-red-50/20"
                                  : "border-zinc-300"
                              }`}
                            />
                            {errors.nomeRepresentante ? (
                              <Text className="text-xs font-medium text-red-600">
                                {errors.nomeRepresentante.message}
                              </Text>
                            ) : null}
                          </View>
                        )}
                      />

                      {/* Linha dupla: E-mail e Telefone */}
                      <View className="flex-col sm:flex-row gap-4">
                        <View className="flex-1">
                          <Controller
                            control={control}
                            name="emailRepresentante"
                            render={({ field }) => (
                              <View className="gap-1.5">
                                <Text className="text-sm font-medium text-zinc-700">
                                  E-mail de Contato <Text className="text-red-500">*</Text>
                                </Text>
                                <TextInput
                                  value={field.value}
                                  onChangeText={field.onChange}
                                  onBlur={field.onBlur}
                                  placeholder="contato@empresa.com"
                                  placeholderTextColor="#a1a1aa"
                                  keyboardType="email-address"
                                  autoCapitalize="none"
                                  maxLength={150}
                                  className={`rounded-xl border px-4 py-3 text-base text-zinc-900 bg-zinc-50 ${
                                    errors.emailRepresentante
                                      ? "border-red-400 bg-red-50/20"
                                      : "border-zinc-300"
                                  }`}
                                />
                                {errors.emailRepresentante ? (
                                  <Text className="text-xs font-medium text-red-600">
                                    {errors.emailRepresentante.message}
                                  </Text>
                                ) : null}
                              </View>
                            )}
                          />
                        </View>

                        <View className="flex-1">
                          <Controller
                            control={control}
                            name="telefone"
                            render={({ field }) => (
                              <View className="gap-1.5">
                                <Text className="text-sm font-medium text-zinc-700">
                                  Telefone / WhatsApp (opcional)
                                </Text>
                                <TextInput
                                  value={field.value}
                                  onChangeText={(texto) => {
                                    field.onChange(formatarTelefoneInput(texto));
                                  }}
                                  onBlur={field.onBlur}
                                  placeholder="(11) 98765-4321"
                                  placeholderTextColor="#a1a1aa"
                                  keyboardType="phone-pad"
                                  maxLength={20}
                                  className={`rounded-xl border px-4 py-3 text-base text-zinc-900 bg-zinc-50 ${
                                    errors.telefone ? "border-red-400 bg-red-50/20" : "border-zinc-300"
                                  }`}
                                />
                                {errors.telefone ? (
                                  <Text className="text-xs font-medium text-red-600">
                                    {errors.telefone.message}
                                  </Text>
                                ) : null}
                              </View>
                            )}
                          />
                        </View>
                      </View>
                    </View>
                  </SecaoCard>

                  {/* Seção 2: Detalhes da Tradução */}
                  <SecaoCard
                    titulo="Detalhes do Projeto de Tradução"
                    subtitulo="Especifique a categoria do projeto, o par de idiomas e orientações especiais."
                  >
                    <View className="gap-4">
                      {/* Categoria do Projeto */}
                      <Controller
                        control={control}
                        name="categoriaProjetoId"
                        render={({ field }) => (
                          <SelectField
                            label="Categoria do Projeto *"
                            value={field.value}
                            onChange={field.onChange}
                            options={opcoesCategoria}
                            placeholder="Selecione a categoria..."
                            error={errors.categoriaProjetoId?.message}
                          />
                        )}
                      />

                      {/* Par de Idiomas */}
                      <View className="gap-2">
                        <Text className="text-sm font-medium text-zinc-700">
                          Par de Idiomas <Text className="text-red-500">*</Text>
                        </Text>

                        <View
                          className={
                            idiomasLadoALado ? "flex-row items-start gap-3" : "gap-3"
                          }
                        >
                          <Controller
                            control={control}
                            name="idiomaOrigemId"
                            render={({ field }) => (
                              <SelectField
                                label="Idioma de Origem"
                                value={field.value}
                                onChange={field.onChange}
                                options={opcoesIdioma}
                                placeholder="Selecione a origem..."
                                invalid={idiomasIguais}
                                error={errors.idiomaOrigemId?.message}
                              />
                            )}
                          />

                          {idiomasLadoALado ? (
                            <View className="pt-9">
                              <Ionicons name="arrow-forward" size={22} color="#8c5230" />
                            </View>
                          ) : null}

                          <Controller
                            control={control}
                            name="idiomaDestinoId"
                            render={({ field }) => (
                              <SelectField
                                label="Idioma de Destino"
                                value={field.value}
                                onChange={field.onChange}
                                options={opcoesIdioma}
                                placeholder="Selecione o destino..."
                                invalid={idiomasIguais}
                                error={
                                  idiomasIguais
                                    ? MENSAGEM_IDIOMAS_IGUAIS
                                    : errors.idiomaDestinoId?.message
                                }
                              />
                            )}
                          />
                        </View>
                      </View>

                      {/* Descrição / Instruções */}
                      <Controller
                        control={control}
                        name="descricao"
                        render={({ field }) => (
                          <View className="gap-1.5">
                            <Text className="text-sm font-medium text-zinc-700">
                              Descrição / Instruções do Projeto (opcional)
                            </Text>
                            <TextInput
                              value={field.value}
                              onChangeText={field.onChange}
                              onBlur={field.onBlur}
                              placeholder="Descreva detalhes como prazos desejados, público-alvo, formatação esperada ou termos técnicos específicos..."
                              placeholderTextColor="#a1a1aa"
                              multiline
                              numberOfLines={5}
                              maxLength={DESCRICAO_MAX}
                              textAlignVertical="top"
                              className={`min-h-[120px] rounded-xl border px-4 py-3 text-base text-zinc-900 bg-zinc-50 ${
                                errors.descricao ? "border-red-400 bg-red-50/20" : "border-zinc-300"
                              }`}
                            />
                            <View className="flex-row items-center justify-between">
                              <Text className="flex-1 text-xs font-medium text-red-600">
                                {errors.descricao?.message ?? ""}
                              </Text>
                              <Text className="text-xs text-zinc-400">
                                {descricao.length}/{DESCRICAO_MAX}
                              </Text>
                            </View>
                          </View>
                        )}
                      />
                    </View>
                  </SecaoCard>

                  {/* Seção 3: Anexos de Arquivos */}
                  <SecaoCard
                    titulo="Arquivos Base para Tradução"
                    subtitulo="Envie os documentos originais para que possamos analisar a contagem de palavras e formato."
                  >
                    <ArquivosField
                      arquivos={arquivos}
                      onChange={setArquivos}
                      disabled={isSubmitting}
                    />
                  </SecaoCard>

                  {/* Mensagem de Erro Geral de Envio */}
                  {erroEnvio ? (
                    <View className="rounded-2xl border border-red-200 bg-red-50 p-4">
                      <View className="flex-row items-center gap-2">
                        <Ionicons name="alert-circle" size={20} color="#b91c1c" />
                        <Text className="flex-1 text-sm font-medium text-red-700">
                          {erroEnvio}
                        </Text>
                      </View>
                    </View>
                  ) : null}

                  {/* Botão de Envio */}
                  <View className="flex-row items-center justify-end gap-4 pt-2">
                    <TouchableOpacity
                      onPress={() => router.push("/" as Href)}
                      disabled={isSubmitting}
                      className="px-6 py-4 rounded-2xl border border-zinc-300 bg-white active:bg-zinc-100"
                    >
                      <Text className="font-semibold text-zinc-700">Cancelar</Text>
                    </TouchableOpacity>

                    <TouchableOpacity
                      onPress={onSubmit}
                      disabled={!podeRegistrar}
                      className={`flex-row items-center justify-center gap-2 rounded-2xl bg-[#8c5230] px-8 py-4 shadow-md shadow-orange-900/20 active:opacity-85 ${
                        podeRegistrar ? "" : "opacity-50"
                      }`}
                    >
                      {isSubmitting ? (
                        <>
                          <ActivityIndicator color="#ffffff" />
                          <Text className="font-bold text-white text-base">
                            Enviando solicitação...
                          </Text>
                        </>
                      ) : (
                        <>
                          <Ionicons name="send" size={18} color="#ffffff" />
                          <Text className="font-bold text-white text-base">
                            Enviar Solicitação
                          </Text>
                        </>
                      )}
                    </TouchableOpacity>
                  </View>
                </View>
              )}
            </View>
          )}
        </View>
      </ScrollView>

      {/* Rodapé discreto */}
      <View className="border-t border-zinc-200 bg-white py-4 px-6 items-center">
        <Text className="text-xs text-zinc-400">
          STENTIO · Plataforma de Gestão de Serviços de Tradução
        </Text>
      </View>
    </View>
  );
}
