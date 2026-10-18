// NovaSolicitacaoScreen.tsx
import { useMemo, useRef, useState, type ReactNode } from "react";
import {
  ActivityIndicator,
  Pressable,
  ScrollView,
  Text,
  TextInput,
  useWindowDimensions,
  View,
} from "react-native";
import { Controller, useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { Ionicons } from "@expo/vector-icons";
import { CabecalhoTela } from "../navegacao/CabecalhoTela";
import { getApiErrorMessage } from "../../lib/api";
import { ArquivosField } from "./ArquivosField";
import { SelectField } from "./SelectField";
import { Toast, useToast } from "./Toast";
import {
  CAMPOS_DO_FORMULARIO,
  DESCRICAO_MAX,
  MENSAGEM_IDIOMAS_IGUAIS,
  VALORES_INICIAIS,
  solicitacaoSchema,
  type SolicitacaoFormValues,
} from "./solicitacaoSchema";
import { criarSolicitacao, extrairErrosDeCampo } from "./solicitacoesApi";
import type { ArquivoSelecionado, ClienteOpcao } from "./types";
import { useOpcoesSolicitacao } from "./useOpcoesSolicitacao";

// Abaixo disto os dois idiomas empilham em vez de ficarem lado a lado.
const LARGURA_IDIOMAS_LADO_A_LADO = 480;

function normalizar(texto: string): string {
  return texto
    .normalize("NFD")
    .replace(/[\u0300-\u036f]/g, "")
    .toLowerCase()
    .trim();
}

function isCampoDoFormulario(campo: string): campo is keyof SolicitacaoFormValues {
  return (CAMPOS_DO_FORMULARIO as string[]).includes(campo);
}

function Secao({ titulo, children }: { titulo: string; children: ReactNode }) {
  return (
    <View className="gap-4 rounded-2xl border border-neutral-200 bg-white p-5 shadow-sm">
      <Text className="text-lg font-bold text-neutral-900">{titulo}</Text>
      {children}
    </View>
  );
}

function DadosCliente({ cliente }: { cliente: ClienteOpcao }) {
  const linhas = [
    { rotulo: "Representante", valor: cliente.nomeRepresentante },
    { rotulo: "E-mail", valor: cliente.emailRepresentante ?? cliente.email },
    { rotulo: "Telefone", valor: cliente.telefone },
    { rotulo: "CPF/CNPJ", valor: cliente.cpfCnpj },
  ].filter((linha) => Boolean(linha.valor));

  return (
    <View className="gap-1 rounded-xl border border-neutral-200 bg-neutral-50 p-4">
      <Text className="text-sm font-semibold text-neutral-900">{cliente.nomeEmpresa}</Text>
      {linhas.map((linha) => (
        <Text key={linha.rotulo} className="text-sm text-neutral-600">
          <Text className="text-neutral-400">{linha.rotulo}: </Text>
          {linha.valor}
        </Text>
      ))}
    </View>
  );
}

export function NovaSolicitacaoScreen() {
  const { width: larguraJanela } = useWindowDimensions();
  const { opcoes, carregando, erro: erroOpcoes, recarregar } = useOpcoesSolicitacao();
  const { toast, mostrar, fechar } = useToast();
  const scrollRef = useRef<ScrollView>(null);

  const [arquivos, setArquivos] = useState<ArquivoSelecionado[]>([]);
  const [buscaCliente, setBuscaCliente] = useState("");
  const [erroEnvio, setErroEnvio] = useState("");

  const {
    control,
    handleSubmit,
    watch,
    reset,
    setError,
    formState: { errors, isValid, isSubmitting },
  } = useForm<SolicitacaoFormValues>({
    resolver: zodResolver(solicitacaoSchema),
    defaultValues: VALORES_INICIAIS,
    mode: "onChange",
  });

  const clienteId = watch("clienteId");
  const idiomaOrigemId = watch("idiomaOrigemId");
  const idiomaDestinoId = watch("idiomaDestinoId");
  const descricao = watch("descricao");

  // RN.2: calculado direto dos valores (e não só do erro do RHF) porque o RHF
  // só revalida o campo que mudou — trocar a origem não reavalia o destino.
  const idiomasIguais = Boolean(idiomaOrigemId) && idiomaOrigemId === idiomaDestinoId;

  // RN.1 + RN.2. Arquivos não entram na conta (RN.3: envio opcional).
  const podeRegistrar = isValid && !idiomasIguais && !isSubmitting && !carregando;

  const clienteSelecionado = opcoes.clientes.find((cliente) => cliente.id === clienteId);

  // A busca filtra a lista do select; o cliente já escolhido nunca some dela.
  const opcoesCliente = useMemo(() => {
    const termo = normalizar(buscaCliente);
    return opcoes.clientes
      .filter(
        (cliente) =>
          !termo || normalizar(cliente.nomeEmpresa).includes(termo) || cliente.id === clienteId,
      )
      .map((cliente) => ({ value: cliente.id, label: cliente.nomeEmpresa }));
  }, [opcoes.clientes, buscaCliente, clienteId]);

  const opcoesCategoria = opcoes.categorias.map((categoria) => ({
    value: categoria.id,
    label: categoria.nome,
  }));

  const opcoesIdioma = opcoes.idiomas.map((idioma) => ({
    value: idioma.id,
    label: `${idioma.nome} (${idioma.codigoIso.toUpperCase()})`,
  }));

  const idiomasLadoALado = larguraJanela >= LARGURA_IDIOMAS_LADO_A_LADO;

  const registrar = handleSubmit(async (values) => {
    setErroEnvio("");

    try {
      await criarSolicitacao(values, arquivos);
    } catch (error) {
      // Erros de validação do backend vão para o campo correspondente (setError);
      // o que não casar com nenhum campo vira um aviso geral acima do botão.
      const errosCampo = extrairErrosDeCampo(error);
      const semCampo = errosCampo.filter((item) => !isCampoDoFormulario(item.campo));

      for (const item of errosCampo) {
        if (isCampoDoFormulario(item.campo)) {
          setError(item.campo, { type: "server", message: item.mensagem });
        }
      }

      if (errosCampo.length === 0) {
        setErroEnvio(getApiErrorMessage(error));
      } else if (semCampo.length > 0) {
        setErroEnvio(semCampo.map((item) => item.mensagem).join(" "));
      }
      return;
    }

    // CA.5: sucesso → limpa o formulário, a busca e os anexos, e avisa.
    reset(VALORES_INICIAIS);
    setArquivos([]);
    setBuscaCliente("");
    scrollRef.current?.scrollTo({ y: 0, animated: true });
    mostrar("Solicitação registrada com sucesso");
  });

  function renderConteudo() {
    if (carregando) {
      return (
        <View className="items-center justify-center gap-3 py-20">
          <ActivityIndicator color="#6f4f28" />
          <Text className="text-sm text-neutral-500">Carregando...</Text>
        </View>
      );
    }

    if (erroOpcoes) {
      return (
        <View className="items-center justify-center gap-3 rounded-2xl border border-neutral-200 bg-white px-6 py-16">
          <Text className="text-center text-sm text-red-600">{erroOpcoes}</Text>
          <Pressable
            onPress={recarregar}
            className="rounded-lg border border-neutral-300 px-4 py-2"
          >
            <Text className="text-sm font-medium text-neutral-700">Tentar de novo</Text>
          </Pressable>
        </View>
      );
    }

    return (
      <View className="gap-6">
        <Secao titulo="Cliente">
          <View className="gap-2">
            <Text className="text-sm font-medium text-neutral-700">Busque por nome</Text>
            <View className="flex-row items-center gap-2 rounded-lg border border-neutral-300 bg-neutral-50 px-3 py-2">
              <Ionicons name="search" size={16} color="#a3a3a3" />
              <TextInput
                value={buscaCliente}
                onChangeText={setBuscaCliente}
                placeholder="Digite parte do nome do cliente"
                placeholderTextColor="#a3a3a3"
                className="flex-1 py-0 text-sm text-neutral-800"
                clearButtonMode="while-editing"
              />
            </View>
          </View>

          <Controller
            control={control}
            name="clienteId"
            render={({ field }) => (
              <SelectField
                label="Cliente"
                value={field.value}
                onChange={field.onChange}
                options={opcoesCliente}
                placeholder="Selecione o cliente..."
                error={errors.clienteId?.message}
              />
            )}
          />

          {clienteSelecionado ? <DadosCliente cliente={clienteSelecionado} /> : null}
        </Secao>

        <Secao titulo="Detalhes">
          <Controller
            control={control}
            name="categoriaProjetoId"
            render={({ field }) => (
              <SelectField
                label="Categoria do projeto"
                value={field.value}
                onChange={field.onChange}
                options={opcoesCategoria}
                placeholder="Selecione a categoria..."
                error={errors.categoriaProjetoId?.message}
              />
            )}
          />

          <View className="gap-2">
            <Text className="text-sm font-medium text-neutral-700">Idioma de origem e destino</Text>

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
                    label="Origem"
                    value={field.value}
                    onChange={field.onChange}
                    options={opcoesIdioma}
                    placeholder="Origem..."
                    invalid={idiomasIguais}
                    error={errors.idiomaOrigemId?.message}
                  />
                )}
              />

              {idiomasLadoALado ? (
                <View className="pt-10">
                  <Ionicons name="arrow-forward" size={20} color="#737373" />
                </View>
              ) : null}

              <Controller
                control={control}
                name="idiomaDestinoId"
                render={({ field }) => (
                  <SelectField
                    label="Destino"
                    value={field.value}
                    onChange={field.onChange}
                    options={opcoesIdioma}
                    placeholder="Destino..."
                    invalid={idiomasIguais}
                    error={
                      idiomasIguais ? MENSAGEM_IDIOMAS_IGUAIS : errors.idiomaDestinoId?.message
                    }
                  />
                )}
              />
            </View>
          </View>

          <Controller
            control={control}
            name="descricao"
            render={({ field }) => (
              <View className="gap-2">
                <Text className="text-sm font-medium text-neutral-700">
                  Descrição / instruções (opcional)
                </Text>
                <TextInput
                  value={field.value}
                  onChangeText={field.onChange}
                  onBlur={field.onBlur}
                  placeholder="Informe detalhes e instruções do projeto"
                  placeholderTextColor="#a3a3a3"
                  multiline
                  numberOfLines={5}
                  maxLength={DESCRICAO_MAX}
                  textAlignVertical="top"
                  className="min-h-[120px] rounded-lg border border-neutral-300 bg-white px-4 py-3 text-neutral-900"
                />
                <View className="flex-row items-center justify-between">
                  <Text className="flex-1 text-xs font-medium text-red-600">
                    {errors.descricao?.message ?? ""}
                  </Text>
                  <Text className="text-xs text-neutral-400">
                    {descricao.length}/{DESCRICAO_MAX}
                  </Text>
                </View>
              </View>
            )}
          />
        </Secao>

        <Secao titulo="Arquivos">
          <ArquivosField arquivos={arquivos} onChange={setArquivos} disabled={isSubmitting} />
        </Secao>

        {erroEnvio ? (
          <View className="rounded-lg border border-red-200 bg-red-50 p-3">
            <Text className="text-sm font-medium text-red-600">{erroEnvio}</Text>
          </View>
        ) : null}

        <View className="flex-row justify-end">
          <Pressable
            onPress={registrar}
            disabled={!podeRegistrar}
            accessibilityRole="button"
            accessibilityState={{ disabled: !podeRegistrar, busy: isSubmitting }}
            className={`flex-row items-center gap-2 rounded-lg bg-[#6f4f28] px-5 py-3 ${
              podeRegistrar ? "" : "opacity-50"
            }`}
          >
            {isSubmitting ? (
              <ActivityIndicator color="#ffffff" />
            ) : (
              <Ionicons name="add" size={18} color="#ffffff" />
            )}
            <Text className="font-medium text-white">Registrar Solicitação</Text>
          </Pressable>
        </View>
      </View>
    );
  }

  return (
    <View className="flex-1 bg-[#fbfaf8]">
      <ScrollView ref={scrollRef} className="flex-1" keyboardShouldPersistTaps="handled">
        <View className="mx-auto w-full max-w-3xl px-4 py-8 pb-16 md:px-8">
          <CabecalhoTela
            contexto="Operacional"
            secao="Solicitações"
            titulo="Nova solicitação"
            descricao="Registre manualmente uma solicitação recebida de um cliente."
            rotaFallback="/solicitacoes"
          />
          {renderConteudo()}
        </View>
      </ScrollView>

      <Toast toast={toast} onClose={fechar} />
    </View>
  );
}