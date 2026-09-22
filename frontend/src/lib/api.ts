import axios from "axios";
import { Platform } from "react-native";
import { getToken } from "./auth";

const DEFAULT_API_URL =
  Platform.OS === "android" ? "http://10.0.2.2:8081" : "http://localhost:8081";

function resolveApiUrl(): string {
  const configured = process.env.EXPO_PUBLIC_API_URL ?? DEFAULT_API_URL;

  // No web o cookie de sessão é SameSite=Lax: página e API precisam estar no
  // mesmo host. Alinha a URL da API ao host atual da página, mantendo a porta.
  if (Platform.OS === "web" && typeof window !== "undefined") {
    try {
      const url = new URL(configured);
      if (url.hostname !== window.location.hostname) {
        url.hostname = window.location.hostname;
        return url.toString().replace(/\/$/, "");
      }
    } catch {
      // URL inválida: usa como configurada.
    }
  }

  return configured;
}

export const API_URL = resolveApiUrl();

export const api = axios.create({
  baseURL: API_URL,
  timeout: 15000,
  // Necessário no web para o browser guardar/enviar o cookie HttpOnly (cross-origin).
  withCredentials: true,
});

api.interceptors.request.use(async (config) => {
  const token = await getToken();
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

export function getApiErrorMessage(error: unknown): string {
  if (axios.isAxiosError(error)) {
    const data = error.response?.data as
      | { message?: string | string[]; error?: string }
      | undefined;

    if (data?.message) {
      return Array.isArray(data.message) ? data.message.join("\n") : data.message;
    }

    if (error.response) {
      switch (error.response.status) {
        case 400:
          return "Dados inválidos. Verifique os campos e tente de novo.";
        case 401:
          return "E-mail ou senha inválidos.";
        case 403:
          return "Você não tem permissão para essa ação.";
        case 404:
          return "Registro não encontrado.";
        case 409:
          return "Já existe um usuário com esse e-mail.";
        case 500:
          return "Erro interno no servidor. Tente novamente.";
        default:
          return `Erro inesperado (${error.response.status}).`;
      }
    }

    return "Não foi possível conectar ao servidor. Verifique sua conexão.";
  }

  return "Erro inesperado. Tente novamente.";
}

    const DEFAULT_CORE_API_URL =
      Platform.OS === "android" ? "http://10.0.2.2:8082" : "http://localhost:8082";
    
    export const CORE_API_URL = process.env.EXPO_PUBLIC_CORE_API_URL ?? DEFAULT_CORE_API_URL;
    
    export const coreApi = axios.create({
      baseURL: CORE_API_URL,
      timeout: 15000,
      withCredentials: true,
    });
    
    coreApi.interceptors.request.use(async (config) => {
      const token = await getToken();
      if (token) {
        config.headers.Authorization = `Bearer ${token}`;
      }
      return config;
    });