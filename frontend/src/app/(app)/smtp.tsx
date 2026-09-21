import React, { useEffect, useState } from 'react';
import {
  View,
  Text,
  TextInput,
  TouchableOpacity,
  ScrollView,
  ActivityIndicator,
} from 'react-native';
import { Ionicons } from '@expo/vector-icons';
import { useRouter } from 'expo-router';
import {
  SmtpConfig,
  EncryptionType,
  DEFAULT_SMTP_CONFIG,
  getSmtpConfig,
  saveSmtpConfig,
  testSmtpConnection,
} from '../../lib/smtpService';

export default function SmtpConfigScreen() {
  const router = useRouter();

  // Estados dos parâmetros SMTP
  const [host, setHost] = useState(DEFAULT_SMTP_CONFIG.host);
  const [port, setPort] = useState(String(DEFAULT_SMTP_CONFIG.port));
  const [username, setUsername] = useState(DEFAULT_SMTP_CONFIG.username);
  const [password, setPassword] = useState(DEFAULT_SMTP_CONFIG.password);
  const [encryption, setEncryption] = useState<EncryptionType>(DEFAULT_SMTP_CONFIG.encryption);
  const [fromEmail, setFromEmail] = useState(DEFAULT_SMTP_CONFIG.fromEmail);
  const [fromName, setFromName] = useState(DEFAULT_SMTP_CONFIG.fromName);

  // Estados de controle de interface
  const [showPassword, setShowPassword] = useState(false);
  const [isLoadingData, setIsLoadingData] = useState(true);
  const [isSaving, setIsSaving] = useState(false);
  const [isTesting, setIsTesting] = useState(false);

  // Estados do teste de envio
  const [testEmail, setTestEmail] = useState('');
  const [testResult, setTestResult] = useState<{
    type: 'success' | 'error' | null;
    message: string;
    details?: string;
  }>({ type: null, message: '' });

  // Feedback geral de salvamento
  const [saveFeedback, setSaveFeedback] = useState<{
    type: 'success' | 'error' | null;
    message: string;
  }>({ type: null, message: '' });

  // Erros de validação
  const [formErrors, setFormErrors] = useState<Record<string, string>>({});

  // Carrega configuração inicial da API ao montar
  useEffect(() => {
    let isMounted = true;
    getSmtpConfig()
      .then((config) => {
        if (isMounted && config) {
          setHost(config.host || '');
          setPort(String(config.port || '587'));
          setUsername(config.username || '');
          setPassword(config.password || '');
          setEncryption(config.encryption || 'TLS');
          setFromEmail(config.fromEmail || '');
          setFromName(config.fromName || '');
        }
      })
      .finally(() => {
        if (isMounted) setIsLoadingData(false);
      });

    return () => {
      isMounted = false;
    };
  }, []);

  const validateForm = () => {
    const errors: Record<string, string> = {};

    if (!host.trim()) {
      errors.host = 'O host do servidor é obrigatório.';
    }

    if (!port.trim()) {
      errors.port = 'A porta é obrigatória.';
    } else if (isNaN(Number(port)) || Number(port) <= 0 || Number(port) > 65535) {
      errors.port = 'Informe uma porta válida (1 - 65535).';
    }

    if (fromEmail.trim() && !/\S+@\S+\.\S+/.test(fromEmail)) {
      errors.fromEmail = 'Informe um e-mail de remetente válido.';
    }

    setFormErrors(errors);
    return Object.keys(errors).length === 0;
  };

  const handleSave = async () => {
    setSaveFeedback({ type: null, message: '' });
    if (!validateForm()) return;

    setIsSaving(true);
    const configPayload: SmtpConfig = {
      host: host.trim(),
      port: Number(port.trim()),
      username: username.trim(),
      password,
      encryption,
      fromEmail: fromEmail.trim(),
      fromName: fromName.trim(),
    };

    try {
      const response = await saveSmtpConfig(configPayload);
      if (response.success) {
        setSaveFeedback({
          type: 'success',
          message: response.message || 'Configurações de SMTP salvas com sucesso!',
        });
      } else {
        setSaveFeedback({
          type: 'error',
          message: response.details || response.message,
        });
      }
    } catch {
      setSaveFeedback({
        type: 'error',
        message: 'Erro inesperado ao salvar configurações.',
      });
    } finally {
      setIsSaving(false);
    }
  };

  const handleTestConnection = async () => {
    setTestResult({ type: null, message: '' });

    if (!host.trim() || !port.trim()) {
      setTestResult({
        type: 'error',
        message: 'Preencha ao menos o Host e a Porta antes de realizar o teste.',
      });
      return;
    }

    if (!testEmail.trim() || !/\S+@\S+\.\S+/.test(testEmail)) {
      setTestResult({
        type: 'error',
        message: 'Informe um e-mail de destino válido para receber o teste.',
      });
      return;
    }

    setIsTesting(true);
    const configPayload: SmtpConfig = {
      host: host.trim(),
      port: Number(port.trim()),
      username: username.trim(),
      password,
      encryption,
      fromEmail: fromEmail.trim() || testEmail.trim(),
      fromName: fromName.trim() || 'Stentio Teste',
    };

    try {
      const result = await testSmtpConnection({
        config: configPayload,
        testEmail: testEmail.trim(),
      });

      if (result.success) {
        setTestResult({
          type: 'success',
          message: result.message,
        });
      } else {
        setTestResult({
          type: 'error',
          message: result.message,
          details: result.details,
        });
      }
    } catch (error: any) {
      setTestResult({
        type: 'error',
        message: 'Falha na comunicação com o serviço de envio.',
        details: error?.message || 'Verifique se o backend está ativo.',
      });
    } finally {
      setIsTesting(false);
    }
  };

  const handleResetDefaults = () => {
    setHost(DEFAULT_SMTP_CONFIG.host);
    setPort(String(DEFAULT_SMTP_CONFIG.port));
    setUsername(DEFAULT_SMTP_CONFIG.username);
    setPassword(DEFAULT_SMTP_CONFIG.password);
    setEncryption(DEFAULT_SMTP_CONFIG.encryption);
    setFromEmail(DEFAULT_SMTP_CONFIG.fromEmail);
    setFromName(DEFAULT_SMTP_CONFIG.fromName);
    setFormErrors({});
    setSaveFeedback({ type: null, message: '' });
    setTestResult({ type: null, message: '' });
  };

  const handleSetPortPreset = (presetPort: string, presetEnc: EncryptionType) => {
    setPort(presetPort);
    setEncryption(presetEnc);
  };

  if (isLoadingData) {
    return (
      <View className="flex-1 items-center justify-center bg-zinc-50 p-6">
        <ActivityIndicator size="large" color="#8c5230" />
        <Text className="text-zinc-600 font-medium mt-4">Carregando configurações...</Text>
      </View>
    );
  }

  return (
    <ScrollView
      className="flex-1 bg-[#fbfaf8]"
      contentContainerStyle={{ flexGrow: 1, paddingBottom: 40 }}
      keyboardShouldPersistTaps="handled"
    >
      <View className="w-full max-w-5xl mx-auto px-4 py-8 md:px-8">
        {/* Cabeçalho de Navegação e Título */}
        <View className="flex-row items-center justify-between mb-8 pb-4 border-b border-zinc-200">
          <View className="flex-row items-center gap-3">
            <TouchableOpacity
              onPress={() => router.canGoBack() ? router.back() : router.push('/')}
              className="p-2.5 rounded-xl bg-white border border-zinc-200 shadow-sm active:bg-zinc-100"
              accessibilityLabel="Voltar"
            >
              <Ionicons name="arrow-back" size={20} color="#8c5230" />
            </TouchableOpacity>
            <View>
              <View className="flex-row items-center gap-2">
                <Text className="text-xs font-bold text-[#8c5230] uppercase tracking-wider">
                  Configurações do Sistema
                </Text>
                <Text className="text-xs text-zinc-400">•</Text>
                <Text className="text-xs text-zinc-500 font-medium">Serviço de E-mail</Text>
              </View>
              <Text className="text-2xl md:text-3xl font-serif font-bold text-zinc-900 mt-1">
                Servidor SMTP
              </Text>
            </View>
          </View>

          <View className="hidden md:flex flex-row items-center gap-2 bg-[#8c5230]/10 px-3 py-1.5 rounded-full">
            <Ionicons name="mail" size={16} color="#8c5230" />
            <Text className="text-xs font-semibold text-[#8c5230]">Microserviço Ativo</Text>
          </View>
        </View>

        {/* Banner de Feedback de Salvamento */}
        {saveFeedback.type && (
          <View
            className={`w-full rounded-2xl p-4 mb-6 flex-row items-center gap-3 border ${
              saveFeedback.type === 'success'
                ? 'bg-emerald-50 border-emerald-200'
                : 'bg-red-50 border-red-200'
            }`}
          >
            <Ionicons
              name={saveFeedback.type === 'success' ? 'checkmark-circle' : 'alert-circle'}
              size={24}
              color={saveFeedback.type === 'success' ? '#059669' : '#dc2626'}
            />
            <Text
              className={`flex-1 text-sm font-medium ${
                saveFeedback.type === 'success' ? 'text-emerald-800' : 'text-red-700'
              }`}
            >
              {saveFeedback.message}
            </Text>
            <TouchableOpacity onPress={() => setSaveFeedback({ type: null, message: '' })}>
              <Ionicons name="close" size={18} color="#71717a" />
            </TouchableOpacity>
          </View>
        )}

        <View className="flex-col lg:flex-row gap-8">
          {/* Coluna Principal: Formulário de Configuração */}
          <View className="flex-1 bg-white rounded-3xl p-6 md:p-8 border border-zinc-200/80 shadow-sm">
            <View className="flex-row items-center gap-2.5 mb-6">
              <View className="w-8 h-8 rounded-lg bg-[#8c5230]/10 items-center justify-center">
                <Ionicons name="server-outline" size={18} color="#8c5230" />
              </View>
              <Text className="text-lg font-bold text-zinc-900">Credenciais e Conexão</Text>
            </View>

            {/* Host e Porta */}
            <View className="flex-col md:flex-row gap-4 mb-5">
              <View className="flex-[3]">
                <Text className="text-zinc-700 font-semibold text-sm mb-2">Servidor SMTP (Host) *</Text>
                <TextInput
                  placeholder="ex: smtp.gmail.com"
                  placeholderTextColor="#a1a1aa"
                  value={host}
                  onChangeText={setHost}
                  autoCapitalize="none"
                  className={`w-full border rounded-2xl px-4 py-3.5 bg-zinc-50 text-zinc-900 text-base ${
                    formErrors.host ? 'border-red-400 bg-red-50/20' : 'border-zinc-200'
                  }`}
                />
                {formErrors.host && (
                  <Text className="text-xs text-red-500 font-medium mt-1">{formErrors.host}</Text>
                )}
              </View>

              <View className="flex-[1] min-w-[120px]">
                <Text className="text-zinc-700 font-semibold text-sm mb-2">Porta *</Text>
                <TextInput
                  placeholder="587"
                  placeholderTextColor="#a1a1aa"
                  value={port}
                  onChangeText={setPort}
                  keyboardType="numeric"
                  className={`w-full border rounded-2xl px-4 py-3.5 bg-zinc-50 text-zinc-900 text-base ${
                    formErrors.port ? 'border-red-400 bg-red-50/20' : 'border-zinc-200'
                  }`}
                />
                {formErrors.port && (
                  <Text className="text-xs text-red-500 font-medium mt-1">{formErrors.port}</Text>
                )}
              </View>
            </View>

            {/* Atalhos rápidos de porta */}
            <View className="mb-6">
              <Text className="text-xs text-zinc-500 mb-2 font-medium">Predefinições de porta:</Text>
              <View className="flex-row flex-wrap gap-2">
                <TouchableOpacity
                  onPress={() => handleSetPortPreset('587', 'TLS')}
                  className={`px-3 py-1.5 rounded-xl border text-xs font-semibold ${
                    port === '587' && encryption === 'TLS'
                      ? 'bg-[#8c5230] border-[#8c5230]'
                      : 'bg-zinc-100 border-zinc-200'
                  }`}
                >
                  <Text
                    className={`text-xs font-medium ${
                      port === '587' && encryption === 'TLS' ? 'text-white' : 'text-zinc-700'
                    }`}
                  >
                    587 (TLS/STARTTLS)
                  </Text>
                </TouchableOpacity>

                <TouchableOpacity
                  onPress={() => handleSetPortPreset('465', 'SSL')}
                  className={`px-3 py-1.5 rounded-xl border text-xs font-semibold ${
                    port === '465' && encryption === 'SSL'
                      ? 'bg-[#8c5230] border-[#8c5230]'
                      : 'bg-zinc-100 border-zinc-200'
                  }`}
                >
                  <Text
                    className={`text-xs font-medium ${
                      port === '465' && encryption === 'SSL' ? 'text-white' : 'text-zinc-700'
                    }`}
                  >
                    465 (SSL)
                  </Text>
                </TouchableOpacity>

                <TouchableOpacity
                  onPress={() => handleSetPortPreset('1025', 'NONE')}
                  className={`px-3 py-1.5 rounded-xl border text-xs font-semibold ${
                    port === '1025'
                      ? 'bg-[#8c5230] border-[#8c5230]'
                      : 'bg-zinc-100 border-zinc-200'
                  }`}
                >
                  <Text
                    className={`text-xs font-medium ${
                      port === '1025' ? 'text-white' : 'text-zinc-700'
                    }`}
                  >
                    1025 (Mailhog Local)
                  </Text>
                </TouchableOpacity>
              </View>
            </View>

            {/* Protocolo de Criptografia */}
            <View className="mb-6">
              <Text className="text-zinc-700 font-semibold text-sm mb-2.5">Criptografia / Segurança</Text>
              <View className="flex-row gap-3">
                {(['TLS', 'SSL', 'NONE'] as EncryptionType[]).map((type) => {
                  const isSelected = encryption === type;
                  const label = type === 'TLS' ? 'TLS / STARTTLS' : type === 'SSL' ? 'SSL' : 'Nenhuma';
                  return (
                    <TouchableOpacity
                      key={type}
                      onPress={() => setEncryption(type)}
                      className={`flex-1 py-3 px-2 rounded-2xl border items-center justify-center ${
                        isSelected
                          ? 'bg-[#8c5230] border-[#8c5230] shadow-sm shadow-orange-900/20'
                          : 'bg-zinc-50 border-zinc-200'
                      }`}
                    >
                      <Text
                        className={`text-xs md:text-sm font-semibold ${
                          isSelected ? 'text-white' : 'text-zinc-700'
                        }`}
                      >
                        {label}
                      </Text>
                    </TouchableOpacity>
                  );
                })}
              </View>
            </View>

            {/* Usuário e Senha */}
            <View className="mb-5">
              <Text className="text-zinc-700 font-semibold text-sm mb-2">Usuário / E-mail de Autenticação</Text>
              <TextInput
                placeholder="usuario@empresa.com"
                placeholderTextColor="#a1a1aa"
                value={username}
                onChangeText={setUsername}
                autoCapitalize="none"
                className="w-full border border-zinc-200 rounded-2xl px-4 py-3.5 bg-zinc-50 text-zinc-900 text-base"
              />
            </View>

            <View className="mb-6">
              <Text className="text-zinc-700 font-semibold text-sm mb-2">Senha do Servidor / App Password</Text>
              <View className="flex-row items-center border border-zinc-200 rounded-2xl bg-zinc-50 px-4">
                <TextInput
                  placeholder="••••••••••••"
                  placeholderTextColor="#a1a1aa"
                  value={password}
                  onChangeText={setPassword}
                  secureTextEntry={!showPassword}
                  autoCapitalize="none"
                  className="flex-1 py-3.5 text-zinc-900 text-base"
                />
                <TouchableOpacity onPress={() => setShowPassword(!showPassword)} className="p-2">
                  <Ionicons
                    name={showPassword ? 'eye-off-outline' : 'eye-outline'}
                    size={20}
                    color="#8c5230"
                  />
                </TouchableOpacity>
              </View>
              <Text className="text-xs text-zinc-400 mt-1.5">
                Para Gmail ou Outlook corporativo, use uma senha de aplicativo gerada no provedor.
              </Text>
            </View>

            <View className="h-px bg-zinc-100 my-4" />

            {/* Configurações do Remetente */}
            <View className="flex-row items-center gap-2.5 mb-5 mt-2">
              <View className="w-8 h-8 rounded-lg bg-[#8c5230]/10 items-center justify-center">
                <Ionicons name="person-outline" size={18} color="#8c5230" />
              </View>
              <Text className="text-lg font-bold text-zinc-900">Remetente Padrão</Text>
            </View>

            <View className="flex-col md:flex-row gap-4 mb-6">
              <View className="flex-1">
                <Text className="text-zinc-700 font-semibold text-sm mb-2">E-mail do Remetente (From)</Text>
                <TextInput
                  placeholder="notificacoes@stentio.com.br"
                  placeholderTextColor="#a1a1aa"
                  value={fromEmail}
                  onChangeText={setFromEmail}
                  keyboardType="email-address"
                  autoCapitalize="none"
                  className={`w-full border rounded-2xl px-4 py-3.5 bg-zinc-50 text-zinc-900 text-base ${
                    formErrors.fromEmail ? 'border-red-400 bg-red-50/20' : 'border-zinc-200'
                  }`}
                />
                {formErrors.fromEmail && (
                  <Text className="text-xs text-red-500 font-medium mt-1">{formErrors.fromEmail}</Text>
                )}
              </View>

              <View className="flex-1">
                <Text className="text-zinc-700 font-semibold text-sm mb-2">Nome de Exibição</Text>
                <TextInput
                  placeholder="Stentio Notificações"
                  placeholderTextColor="#a1a1aa"
                  value={fromName}
                  onChangeText={setFromName}
                  className="w-full border border-zinc-200 rounded-2xl px-4 py-3.5 bg-zinc-50 text-zinc-900 text-base"
                />
              </View>
            </View>

            {/* Ações Inferiores */}
            <View className="flex-row items-center justify-between pt-4 border-t border-zinc-100 gap-3">
              <TouchableOpacity
                onPress={handleResetDefaults}
                className="px-4 py-3.5 rounded-2xl border border-zinc-300 active:bg-zinc-100"
              >
                <Text className="text-zinc-700 font-semibold text-sm">Restaurar Padrões</Text>
              </TouchableOpacity>

              <TouchableOpacity
                onPress={handleSave}
                disabled={isSaving}
                className="flex-row items-center gap-2 bg-[#8c5230] px-6 py-3.5 rounded-2xl shadow-md shadow-orange-900/20 active:opacity-90"
              >
                {isSaving ? (
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

          {/* Coluna Lateral: Teste de Conexão SMTP */}
          <View className="w-full lg:w-96 flex-col gap-6">
            <View className="bg-white rounded-3xl p-6 md:p-8 border border-zinc-200/80 shadow-sm">
              <View className="flex-row items-center gap-2.5 mb-4">
                <View className="w-8 h-8 rounded-lg bg-[#8c5230]/10 items-center justify-center">
                  <Ionicons name="paper-plane-outline" size={18} color="#8c5230" />
                </View>
                <Text className="text-lg font-bold text-zinc-900">Testar Conexão</Text>
              </View>

              <Text className="text-sm text-zinc-600 leading-relaxed mb-6">
                Envie um e-mail de teste para verificar se as credenciais e o servidor estão respondendo
                adequadamente antes de salvar para produção.
              </Text>

              <View className="mb-4">
                <Text className="text-zinc-700 font-semibold text-sm mb-2">Enviar e-mail de teste para</Text>
                <TextInput
                  placeholder="seu-email@dominio.com"
                  placeholderTextColor="#a1a1aa"
                  value={testEmail}
                  onChangeText={setTestEmail}
                  keyboardType="email-address"
                  autoCapitalize="none"
                  className="w-full border border-zinc-200 rounded-2xl px-4 py-3.5 bg-zinc-50 text-zinc-900 text-base"
                />
              </View>

              <TouchableOpacity
                onPress={handleTestConnection}
                disabled={isTesting}
                className="w-full flex-row items-center justify-center gap-2 bg-zinc-900 py-3.5 rounded-2xl active:bg-zinc-800 shadow-sm"
              >
                {isTesting ? (
                  <ActivityIndicator color="#fff" size="small" />
                ) : (
                  <>
                    <Ionicons name="send" size={16} color="#fff" />
                    <Text className="text-white font-bold text-sm">Disparar Teste Agora</Text>
                  </>
                )}
              </TouchableOpacity>

              {/* Resultado do Teste */}
              {testResult.type && (
                <View
                  className={`mt-6 rounded-2xl p-4 border ${
                    testResult.type === 'success'
                      ? 'bg-emerald-50 border-emerald-200'
                      : 'bg-red-50 border-red-200'
                  }`}
                >
                  <View className="flex-row items-start gap-2.5">
                    <Ionicons
                      name={testResult.type === 'success' ? 'checkmark-circle' : 'alert-circle'}
                      size={20}
                      color={testResult.type === 'success' ? '#059669' : '#dc2626'}
                      style={{ marginTop: 2 }}
                    />
                    <View className="flex-1">
                      <Text
                        className={`text-sm font-bold ${
                          testResult.type === 'success' ? 'text-emerald-900' : 'text-red-900'
                        }`}
                      >
                        {testResult.type === 'success' ? 'Teste Bem-Sucedido' : 'Falha no Teste'}
                      </Text>
                      <Text
                        className={`text-xs mt-1 leading-relaxed ${
                          testResult.type === 'success' ? 'text-emerald-700' : 'text-red-700'
                        }`}
                      >
                        {testResult.message}
                      </Text>
                      {testResult.details && (
                        <Text className="text-[11px] text-zinc-500 mt-2 bg-white/60 p-2 rounded-lg font-mono">
                          {testResult.details}
                        </Text>
                      )}
                    </View>
                  </View>
                </View>
              )}
            </View>

            {/* Card Informativo / Boas Práticas */}
            <View className="bg-amber-50/70 border border-amber-200/70 rounded-3xl p-6">
              <View className="flex-row items-center gap-2 mb-2">
                <Ionicons name="information-circle-outline" size={20} color="#b45309" />
                <Text className="font-bold text-amber-900 text-sm">Ambiente de Desenvolvimento</Text>
              </View>
              <Text className="text-xs text-amber-800 leading-relaxed">
                Durante os testes locais com o Docker Compose, utilize o <Text className="font-bold">Mailhog</Text>{' '}
                definindo o host como <Text className="font-mono font-semibold">localhost</Text> ou{' '}
                <Text className="font-mono font-semibold">mailhog</Text> na porta{' '}
                <Text className="font-mono font-semibold">1025</Text> sem autenticação.
              </Text>
            </View>
          </View>
        </View>
      </View>
    </ScrollView>
  );
}
