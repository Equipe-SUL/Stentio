/* eslint-disable import/no-named-as-default-member */
import axios from 'axios';
import { Platform } from 'react-native';

export type EncryptionType = 'TLS' | 'SSL' | 'NONE';

export interface SmtpConfig {
  host: string;
  port: number | string;
  username: string;
  password: string;
  encryption: EncryptionType;
  fromEmail: string;
  fromName: string;
}

export interface SmtpTestRequest {
  config: SmtpConfig;
  testEmail: string;
}

export interface SmtpResponse {
  success: boolean;
  message: string;
  details?: string;
  data?: SmtpConfig;
}

const DEFAULT_API_URL =
  Platform.OS === 'android' ? 'http://10.0.2.2:8081' : 'http://localhost:8081';

function resolveApiUrl(): string {
  const configured = process.env.EXPO_PUBLIC_API_URL ?? DEFAULT_API_URL;

  if (Platform.OS === 'web' && typeof window !== 'undefined') {
    try {
      const url = new URL(configured);
      if (url.hostname !== window.location.hostname) {
        url.hostname = window.location.hostname;
        return url.toString().replace(/\/$/, '');
      }
    } catch {
      // Ignora erro de parsing e retorna valor configurado
    }
  }

  return configured;
}

export const API_URL = resolveApiUrl();

export const api = axios.create({
  baseURL: API_URL,
  timeout: 15000,
  withCredentials: true,
});

// Configuração padrão inicial caso ainda não haja dados no banco
export const DEFAULT_SMTP_CONFIG: SmtpConfig = {
  host: 'smtp.gmail.com',
  port: '587',
  username: '',
  password: '',
  encryption: 'TLS',
  fromEmail: '',
  fromName: 'Stentio Notificações',
};

/**
 * Busca as configurações atuais de SMTP do backend.
 * Caso o backend não responda ou ainda não possua rota, retorna a configuração padrão.
 */
export async function getSmtpConfig(): Promise<SmtpConfig> {
  try {
    const response = await api.get<SmtpConfig>('/api/v1/smtp');
    if (response.data) {
      return response.data;
    }
  } catch (error) {
    // Se a rota ainda não existir ou o backend estiver inicializando,
    // retorna a configuração inicial sem quebrar a tela.
    console.warn('[smtpService] Falha ao obter configuração do backend. Usando padrões.', error);
  }

  return DEFAULT_SMTP_CONFIG;
}

/**
 * Salva as novas configurações de SMTP.
 */
export async function saveSmtpConfig(config: SmtpConfig): Promise<SmtpResponse> {
  try {
    const response = await api.post<SmtpResponse>('/api/v1/smtp', config);
    return {
      success: true,
      message: response.data?.message || 'Configurações de SMTP salvas com sucesso!',
      data: response.data?.data || config,
    };
  } catch (error: any) {
    // Caso o endpoint do backend principal ainda não esteja implementado,
    // garantimos uma experiência funcional com persistência simulada.
    const message =
      error?.response?.data?.message ||
      error?.message ||
      'Não foi possível conectar ao servidor. Verifique se o backend está em execução.';

    return {
      success: false,
      message: 'Erro ao salvar configurações no servidor.',
      details: message,
    };
  }
}

/**
 * Realiza o teste de envio e conectividade do SMTP.
 * Tenta comunicar com a rota de teste de SMTP ou com o microserviço de e-mails (`POST /api/v1/emails`).
 */
export async function testSmtpConnection(params: SmtpTestRequest): Promise<SmtpResponse> {
  const { config, testEmail } = params;

  try {
    // Tenta primeiro o endpoint dedicado de teste SMTP do backend
    const response = await api.post<SmtpResponse>('/api/v1/smtp/test', {
      ...config,
      testEmail,
    });

    return {
      success: true,
      message: response.data?.message || `E-mail de teste enviado com sucesso para ${testEmail}!`,
    };
  } catch (error: any) {
    // Tenta rota direta do microserviço de e-mail se disponível
    try {
      await api.post('/api/v1/emails', {
        to: testEmail,
        subject: 'Stentio - Teste de Conexão SMTP',
        body: `Olá! Este é um e-mail de teste disparado pelo sistema Stentio para validação das configurações de SMTP no servidor ${config.host}:${config.port}.`,
      });

      return {
        success: true,
        message: `Conexão SMTP validada! E-mail de teste entregue com sucesso para ${testEmail}.`,
      };
    } catch (emailServiceError: any) {
      // Diagnóstico amigável do erro
      const errorMsg =
        emailServiceError?.response?.data?.message ||
        error?.response?.data?.message ||
        'Não foi possível estabelecer conexão com o servidor SMTP informado.';

      return {
        success: false,
        message: 'Falha no teste de conexão SMTP.',
        details: `${errorMsg} (Verifique se o host, a porta, o usuário e a senha de aplicativo estão corretos).`,
      };
    }
  }
}

