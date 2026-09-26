import React, { useEffect, useState } from 'react';
import {
  ActivityIndicator,
  ScrollView,
  Text,
  TextInput,
  TouchableOpacity,
  View,
  Platform,
  useWindowDimensions,
} from 'react-native';
import { Image } from 'expo-image';
import { Ionicons } from '@expo/vector-icons';
import { useRouter } from 'expo-router';
import * as DocumentPicker from 'expo-document-picker';
import {
  buscarEmpresa,
  carregarLogo,
  salvarEmpresa,
  salvarLogo,
  type EmpresaRequest,
  type EmpresaResponse,
  type LogoSource,
} from '../../../../lib/empresaService';
import { getApiErrorMessage } from '../../../../lib/api';
import { registrarLog } from '../../../../lib/httpDebug';

// ---------------------------------------------------------------------------
// Tipos
// ---------------------------------------------------------------------------

type FormErrors = Partial<Record<keyof EmpresaRequest, string>>;

type Feedback = {
  tipo: 'sucesso' | 'erro' | null;
  mensagem: string;
};

// ---------------------------------------------------------------------------
// Helpers de validação
// ---------------------------------------------------------------------------

function validarCnpj(cnpj: string): boolean {
  const limpo = cnpj.replace(/\D/g, '');
  if (limpo.length !== 14 || /^(\d)\1+$/.test(limpo)) return false;

  const calcularDigito = (tamanho: number) => {
    let soma = 0;
    let peso = tamanho - 7;
    for (let indice = 0; indice < tamanho; indice += 1) {
      soma += Number(limpo[indice]) * peso;
      peso -= 1;
      if (peso < 2) peso = 9;
    }
    const resto = soma % 11;
    return resto < 2 ? 0 : 11 - resto;
  };

  return (
    calcularDigito(12) === Number(limpo[12]) &&
    calcularDigito(13) === Number(limpo[13])
  );
}

function validarForm(form: EmpresaRequest): FormErrors {
  const erros: FormErrors = {};

  if (!form.nome.trim()) {
    erros.nome = 'Nome é obrigatório.';
  }

  if (!form.cnpj.trim()) {
    erros.cnpj = 'CNPJ é obrigatório.';
  } else if (!validarCnpj(form.cnpj.trim())) {
    erros.cnpj = 'CNPJ inválido. Informe 14 dígitos.';
  }

  if (!form.endereco.trim()) {
    erros.endereco = 'Endereço é obrigatório.';
  }

  if (form.telefone.trim() && !/^[0-9+()\-\s]{8,20}$/.test(form.telefone.trim())) {
    erros.telefone = 'Telefone inválido.';
  }

  return erros;
}

// ---------------------------------------------------------------------------
// Componente principal
// ---------------------------------------------------------------------------

const FORM_VAZIO: EmpresaRequest = {
  nome: '',
  cnpj: '',
  endereco: '',
  telefone: '',
};

export default function ConfiguracaoEmpresaScreen() {
  const router = useRouter();

  // Regra de layout decided em JS, e não por lg:/md:.
  //
  // No web o uniwind compila para CSS real e o breakpoint funciona. No nativo
  // ele resolve o breakpoint em runtime, e na prática o lg: está vencendo em
  // celular Android em retrato — daí as duas colunas lado a lado. Como o layout
  // mobile é requisito (não preferência), a gente deixa de depender do
  // breakpoint no nativo: duas colunas só no web, e acima de 1024px.
  const { width } = useWindowDimensions();
  const duasColunas = Platform.OS === 'web' && width >= 1024;

  // Estados do formulário
  const [form, setForm] = useState<EmpresaRequest>(FORM_VAZIO);
  const [errosForm, setErrosForm] = useState<FormErrors>({});

  // Estado dos dados carregados
  const [dadosEmpresa, setDadosEmpresa] = useState<EmpresaResponse | null>(null);

  // Estados de controle de UI
  const [carregando, setCarregando] = useState(true);
  const [salvando, setSalvando] = useState(false);
  const [enviandoLogo, setEnviandoLogo] = useState(false);
  // A prévia tem ciclo próprio: só vale mostrar "carregando" enquanto a
  // requisição está em voo. Sem separar, uma falha de rede deixava o spinner
  // girando para sempre, já que o <Image> resolve a carga por conta própria e
  // nunca rejecta a promise do carregarLogo().
  const [carregandoLogo, setCarregandoLogo] = useState(false);
  const [falhaLogo, setFalhaLogo] = useState(false);

  // Feedback geral
  const [feedback, setFeedback] = useState<Feedback>({ tipo: null, mensagem: '' });

  // Chave para forçar re-render da imagem da logo após upload
  const [logoKey, setLogoKey] = useState(() => Date.now());
  // Origem da logo já autenticada. No web é um object URL (string), no nativo é
  // { uri, headers } para o expo-image buscar com o token — ver carregarLogo.
  const [logo, setLogo] = useState<LogoSource | string | null>(null);

  // ---------------------------------------------------------------------------
  // Carregamento inicial
  // ---------------------------------------------------------------------------

  useEffect(() => {
    let ativo = true;

    buscarEmpresa()
      .then(async (dados) => {
        if (!ativo) return;
        setDadosEmpresa(dados);
        if (dados) {
          setForm({
            nome: dados.nome ?? '',
            cnpj: dados.cnpj ?? '',
            endereco: dados.endereco ?? '',
            telefone: dados.telefone ?? '',
          });
          if (dados.logoDisponivel) {
            // Versão também na carga inicial, não só depois de upload.
            //
            // O expo-image tem cache próprio em memória, chaveado pela URL, e
            // ele ignora o Cache-Control do servidor. Sem a query de versão, sair
            // e voltar para a tela redisplay da URL /api/v1/empresa/logo e ele
            // devolve a imagem antiga sem consultar a rede.
            if (ativo) setCarregandoLogo(true);
            const origem = await carregarLogo(Date.now());
            if (!ativo) return;
            setFalhaLogo(origem === null);
            if (origem) setLogo(origem);
          }
        }
      })
      .catch(() => {
        if (ativo) {
          setFeedback({
            tipo: 'erro',
            mensagem: 'Não foi possível carregar os dados da empresa.',
          });
        }
      })
      .finally(() => {
        if (ativo) {
          setCarregando(false);
          setCarregandoLogo(false);
        }
      });

    return () => {
      ativo = false;
    };
  }, []);

  // Object URLs não são liberados pelo garbage collector. Revogar a anterior a cada
  // troca e no unmount evita segurar a blob da logo em memória para sempre.
  // Só existe no web: no nativo a origem é { uri, headers } e não há blob.
  useEffect(() => {
    return () => {
      if (Platform.OS === 'web' && typeof logo === 'string') {
        URL.revokeObjectURL(logo);
      }
    };
  }, [logo]);

  // ---------------------------------------------------------------------------
  // Handlers
  // ---------------------------------------------------------------------------

  function setcampo<K extends keyof EmpresaRequest>(campo: K, valor: string) {
    setForm((prev) => ({ ...prev, [campo]: valor }));
    // Limpa o erro do campo ao editar
    if (errosForm[campo]) {
      setErrosForm((prev) => ({ ...prev, [campo]: undefined }));
    }
  }

  async function handleSalvar() {
    setFeedback({ tipo: null, mensagem: '' });

    const erros = validarForm(form);
    setErrosForm(erros);
    if (Object.keys(erros).length > 0) return;

    setSalvando(true);
    try {
      const dados = await salvarEmpresa({
        nome: form.nome.trim(),
        cnpj: form.cnpj.trim(),
        endereco: form.endereco.trim(),
        telefone: form.telefone.trim(),
      });
      setDadosEmpresa(dados);
      setFeedback({ tipo: 'sucesso', mensagem: 'Configurações salvas com sucesso' });
    } catch (error) {
      setFeedback({ tipo: 'erro', mensagem: getApiErrorMessage(error) });
    } finally {
      setSalvando(false);
    }
  }

  async function handleEscolherLogo() {
    try {
      const resultado = await DocumentPicker.getDocumentAsync({
        type: ['image/png', 'image/jpeg', 'image/webp'],
        copyToCacheDirectory: true,
      });

      if (resultado.canceled || !resultado.assets?.length) return;

      const arquivo = resultado.assets[0];
      const tamanhoMaxMb = 2 * 1024 * 1024;

      if (arquivo.size && arquivo.size > tamanhoMaxMb) {
        setFeedback({ tipo: 'erro', mensagem: 'A logo deve ter no máximo 2 MB.' });
        return;
      }

      setEnviandoLogo(true);
      setFeedback({ tipo: null, mensagem: '' });

      try {
        const dados = await salvarLogo(
          arquivo.uri,
          arquivo.name,
          arquivo.mimeType ?? 'image/png'
        );
        setDadosEmpresa(dados);
        setLogoKey(Date.now()); // força reload da imagem
        setFalhaLogo(false);
        const origem = await carregarLogo(Date.now());
        setFalhaLogo(origem === null);
        if (origem) setLogo(origem);
        setFeedback({ tipo: 'sucesso', mensagem: 'Logo atualizada com sucesso.' });
      } catch (error) {
        setFeedback({ tipo: 'erro', mensagem: getApiErrorMessage(error) });
      } finally {
        setEnviandoLogo(false);
      }
    } catch (error) {
      setFeedback({ tipo: 'erro', mensagem: getApiErrorMessage(error) });
    }
  }

  // ---------------------------------------------------------------------------
  // Render: estado de carregamento inicial
  // ---------------------------------------------------------------------------

  if (carregando) {
    return (
      <View className="flex-1 items-center justify-center bg-zinc-50 p-6">
        <ActivityIndicator size="large" color="#8c5230" />
        <Text className="text-zinc-600 font-medium mt-4">Carregando dados da empresa...</Text>
      </View>
    );
  }

  // ---------------------------------------------------------------------------
  // Render principal
  // ---------------------------------------------------------------------------

  const temLogo = dadosEmpresa?.logoDisponivel ?? false;

  return (
    <ScrollView
      className="flex-1 bg-[#fbfaf8]"
      contentContainerStyle={{ flexGrow: 1, paddingBottom: 40 }}
      keyboardShouldPersistTaps="handled"
    >
      <View className="w-full max-w-5xl mx-auto px-4 py-8 md:px-8">

        {/* Cabeçalho de Navegação e Título */}
        <View className="flex-row items-center justify-between mb-8 pb-4 border-b border-zinc-200">
          {/* flex-1 + min-w-0: no React Native o flexShrink padrão é 0, diferente do
              CSS, então sem isto o bloco de título não encolhe e estoura a largura
              do celular. min-w-0 é o que permite o texto quebrar. */}
          <View className="flex-1 min-w-0 flex-row items-center gap-3">
            <TouchableOpacity
              onPress={() => (router.canGoBack() ? router.back() : router.push('/usuarios'))}
              className="p-2.5 rounded-xl bg-white border border-zinc-200 shadow-sm active:bg-zinc-100"
              accessibilityLabel="Voltar"
            >
              <Ionicons name="arrow-back" size={20} color="#8c5230" />
            </TouchableOpacity>

            <View className="flex-1 min-w-0">
              <View className="flex-row items-center gap-2 flex-wrap">
                <Text className="text-xs font-bold text-[#8c5230] uppercase tracking-wider">
                  Configurações do Sistema
                </Text>
                <Text className="text-xs text-zinc-400">•</Text>
                <Text className="text-xs text-zinc-500 font-medium">Empresa</Text>
              </View>
              <Text className="text-2xl md:text-3xl font-serif font-bold text-zinc-900 mt-1">
                Dados da Empresa
              </Text>
            </View>
          </View>

          {/* Só no web: no celular essa badge competia com o título por espaço e
              era o que estourava o cabeçalho. hidden md:flex dependia do mesmo
              breakpoint que falhava no nativo. */}
          {duasColunas && (
            <View className="flex-row items-center gap-2 bg-[#8c5230]/10 px-3 py-1.5 rounded-full">
              <Ionicons name="business" size={16} color="#8c5230" />
              <Text className="text-xs font-semibold text-[#8c5230]">
                {dadosEmpresa ? 'Cadastro existente' : 'Novo cadastro'}
              </Text>
            </View>
          )}
        </View>

        {/* Banner de Feedback */}
        {feedback.tipo && (
          <View
            className={`w-full rounded-2xl p-4 mb-6 flex-row items-center gap-3 border ${
              feedback.tipo === 'sucesso'
                ? 'bg-emerald-50 border-emerald-200'
                : 'bg-red-50 border-red-200'
            }`}
          >
            <Ionicons
              name={feedback.tipo === 'sucesso' ? 'checkmark-circle' : 'alert-circle'}
              size={24}
              color={feedback.tipo === 'sucesso' ? '#059669' : '#dc2626'}
            />
            <Text
              className={`flex-1 text-sm font-medium ${
                feedback.tipo === 'sucesso' ? 'text-emerald-800' : 'text-red-700'
              }`}
            >
              {feedback.mensagem}
            </Text>
            <TouchableOpacity onPress={() => setFeedback({ tipo: null, mensagem: '' })}>
              <Ionicons name="close" size={18} color="#71717a" />
            </TouchableOpacity>
          </View>
        )}

        {/* Duas colunas só no web, e acima de 1024px.
            No nativo a sidebar SEMPRE desce abaixo do formulário, decidido em JS
            (ver `duasColunas`): o uniwind não Apply lg:/md: de forma confiável
            no Android, e o layout mobile tem que ser uma coluna só. */}
        <View className={duasColunas ? 'flex-row gap-8' : 'flex-col gap-6'}>

          {/* Coluna Principal: Formulário */}
          <View
            className={
              duasColunas
                ? 'flex-1 min-w-0 bg-white rounded-3xl p-6 md:p-8 border border-zinc-200/80 shadow-sm'
                : 'w-full bg-white rounded-3xl p-5 border border-zinc-200/80 shadow-sm'
            }
          >
            <View className="flex-row items-center gap-2.5 mb-6">
              <View className="w-8 h-8 rounded-lg bg-[#8c5230]/10 items-center justify-center">
                <Ionicons name="document-text-outline" size={18} color="#8c5230" />
              </View>
              <Text className="text-lg font-bold text-zinc-900">Informações da Empresa</Text>
            </View>

            {/* Nome */}
            <View className="mb-5">
              <Text className="text-zinc-700 font-semibold text-sm mb-2">
                Nome da Empresa *
              </Text>
              <TextInput
                placeholder="Ex: Stentio Traduções Ltda."
                placeholderTextColor="#a1a1aa"
                value={form.nome}
                onChangeText={(v) => setcampo('nome', v)}
                className={`w-full border rounded-2xl px-4 py-3.5 bg-zinc-50 text-zinc-900 text-base ${
                  errosForm.nome ? 'border-red-400 bg-red-50/20' : 'border-zinc-200'
                }`}
              />
              {errosForm.nome && (
                <Text className="text-xs text-red-500 font-medium mt-1">{errosForm.nome}</Text>
              )}
            </View>

            {/* CNPJ */}
            <View className="mb-5">
              <Text className="text-zinc-700 font-semibold text-sm mb-2">CNPJ *</Text>
              <TextInput
                placeholder="Ex: 00.000.000/0001-00"
                placeholderTextColor="#a1a1aa"
                value={form.cnpj}
                onChangeText={(v) => setcampo('cnpj', v)}
                autoCapitalize="characters"
                className={`w-full border rounded-2xl px-4 py-3.5 bg-zinc-50 text-zinc-900 text-base ${
                  errosForm.cnpj ? 'border-red-400 bg-red-50/20' : 'border-zinc-200'
                }`}
              />
              {errosForm.cnpj && (
                <Text className="text-xs text-red-500 font-medium mt-1">{errosForm.cnpj}</Text>
              )}
            </View>

            {/* Endereço */}
            <View className="mb-5">
              <Text className="text-zinc-700 font-semibold text-sm mb-2">Endereço *</Text>
              <TextInput
                placeholder="Ex: Rua das Flores, 123 – São Paulo/SP"
                placeholderTextColor="#a1a1aa"
                value={form.endereco}
                onChangeText={(v) => setcampo('endereco', v)}
                multiline
                numberOfLines={2}
                className={`w-full border rounded-2xl px-4 py-3.5 bg-zinc-50 text-zinc-900 text-base ${
                  errosForm.endereco ? 'border-red-400 bg-red-50/20' : 'border-zinc-200'
                }`}
              />
              {errosForm.endereco && (
                <Text className="text-xs text-red-500 font-medium mt-1">
                  {errosForm.endereco}
                </Text>
              )}
            </View>

            {/* Telefone */}
            <View className="mb-6">
              <Text className="text-zinc-700 font-semibold text-sm mb-2">Telefone</Text>
              <TextInput
                placeholder="Ex: +55 (11) 99999-9999"
                placeholderTextColor="#a1a1aa"
                value={form.telefone}
                onChangeText={(v) => setcampo('telefone', v)}
                keyboardType="phone-pad"
                className={`w-full border rounded-2xl px-4 py-3.5 bg-zinc-50 text-zinc-900 text-base ${
                  errosForm.telefone ? 'border-red-400 bg-red-50/20' : 'border-zinc-200'
                }`}
              />
              {errosForm.telefone && (
                <Text className="text-xs text-red-500 font-medium mt-1">
                  {errosForm.telefone}
                </Text>
              )}
              <Text className="text-xs text-zinc-400 mt-1.5">
                Campo opcional. Será exibido nos documentos gerados.
              </Text>
            </View>

            {/* Botão Salvar */}
            <View className="pt-4 border-t border-zinc-100">
              <TouchableOpacity
                onPress={handleSalvar}
                disabled={salvando}
                className="flex-row items-center justify-center gap-2 bg-[#8c5230] px-6 py-3.5 rounded-2xl shadow-md shadow-orange-900/20 active:opacity-90"
              >
                {salvando ? (
                  <ActivityIndicator color="#fff" size="small" />
                ) : (
                  <>
                    <Ionicons name="save-outline" size={18} color="#fff" />
                    <Text className="text-white font-bold text-base">Salvar Configurações</Text>
                  </>
                )}
              </TouchableOpacity>
            </View>
          </View>

          {/* Coluna Lateral: Logo */}
          <View className={duasColunas ? 'w-80 flex-col gap-6' : 'w-full flex-col gap-6'}>
            <View className="bg-white rounded-3xl p-6 md:p-8 border border-zinc-200/80 shadow-sm">
              <View className="flex-row items-center gap-2.5 mb-4">
                <View className="w-8 h-8 rounded-lg bg-[#8c5230]/10 items-center justify-center">
                  <Ionicons name="image-outline" size={18} color="#8c5230" />
                </View>
                <Text className="text-lg font-bold text-zinc-900">Logo</Text>
              </View>

              <Text className="text-sm text-zinc-600 leading-relaxed mb-5">
                A logo será exibida nos documentos e relatórios gerados pelo sistema. Formatos
                aceitos: PNG, JPEG ou WebP. Tamanho máximo: 2 MB.
              </Text>

                {/* Preview da logo */}
                <View className="items-center mb-5">
                  {temLogo && logo && !falhaLogo ? (
                    <View className="w-40 h-40 rounded-2xl border border-zinc-200 overflow-hidden bg-zinc-50 items-center justify-center">
                      <Image
                        key={logoKey}
                        source={logo}
                        style={{ width: 160, height: 160 }}
                        contentFit="contain"
                        accessibilityLabel="Logo da empresa"
                        onLoad={() => registrarLog('logo', 'imagem carregada')}
                        onError={(erro) => {
                          registrarLog(
                            'logo',
                            `falha ao carregar a imagem (${erro.error ?? 'erro desconhecido'})`,
                            'erro',
                          );
                          setFalhaLogo(true);
                        }}
                      />
                    </View>
                  ) : temLogo && carregandoLogo ? (
                    <View className="w-40 h-40 rounded-2xl border border-zinc-200 bg-zinc-100 items-center justify-center gap-2">
                      <ActivityIndicator color="#a1a1aa" size="small" />
                      <Text className="text-xs text-zinc-400 text-center px-2">
                        Carregando logo...
                      </Text>
                    </View>
                  ) : temLogo && falhaLogo ? (
                    <View className="w-40 h-40 rounded-2xl border border-amber-200 bg-amber-50 items-center justify-center gap-2 px-3">
                      <Ionicons name="alert-circle-outline" size={30} color="#b45309" />
                      <Text className="text-xs text-amber-800 text-center">
                        Não foi possível carregar a logo
                      </Text>
                    </View>
                  ) : (
                    <View className="w-40 h-40 rounded-2xl border border-dashed border-zinc-300 bg-zinc-50 items-center justify-center gap-2">
                      <Ionicons name="image-outline" size={36} color="#a1a1aa" />
                      <Text className="text-xs text-zinc-400 text-center px-2">
                        Nenhuma logo cadastrada
                      </Text>
                    </View>
                  )}
                </View>

              {/* Botão de upload */}
              {dadosEmpresa ? (
                <TouchableOpacity
                  onPress={handleEscolherLogo}
                  disabled={enviandoLogo}
                  className="w-full flex-row items-center justify-center gap-2 border border-zinc-300 rounded-2xl py-3.5 px-4 active:bg-zinc-50"
                >
                  {enviandoLogo ? (
                    <ActivityIndicator color="#8c5230" size="small" />
                  ) : (
                    <>
                      <Ionicons name="cloud-upload-outline" size={18} color="#8c5230" />
                      <Text className="text-sm font-semibold text-zinc-700">
                        {temLogo ? 'Alterar logo' : 'Enviar logo'}
                      </Text>
                    </>
                  )}
                </TouchableOpacity>
              ) : (
                <View className="w-full rounded-2xl border border-amber-200 bg-amber-50/70 p-3">
                  <View className="flex-row items-center gap-2">
                    <Ionicons name="information-circle-outline" size={16} color="#b45309" />
                    <Text className="text-xs text-amber-800 flex-1">
                      Salve os dados da empresa antes de enviar a logo.
                    </Text>
                  </View>
                </View>
              )}
            </View>

            {/* Card informativo */}
            <View className="bg-amber-50/70 border border-amber-200/70 rounded-3xl p-6">
              <View className="flex-row items-center gap-2 mb-2">
                <Ionicons name="information-circle-outline" size={20} color="#b45309" />
                <Text className="font-bold text-amber-900 text-sm">Sobre os dados da empresa</Text>
              </View>
              <Text className="text-xs text-amber-800 leading-relaxed">
                As informações cadastradas aqui serão utilizadas automaticamente nos documentos
                gerados pelo sistema, como contratos, propostas e e-mails enviados aos clientes.
              </Text>
            </View>
          </View>

        </View>
      </View>
    </ScrollView>
  );
}
