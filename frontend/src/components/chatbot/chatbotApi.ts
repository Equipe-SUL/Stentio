import { coreApi } from "../../lib/api";
import { Platform } from "react-native";
import type {
  EnviarMensagemChatbotRequest,
  EnviarMensagemChatbotResponse,
  HistoricoChatbotResponse,
} from "./types";

const ENDPOINT = "/api/v1/ia/chatbot";
const CHATBOT_API_BASE_URL =
  Platform.OS === "android"
    ? process.env.EXPO_PUBLIC_CHATBOT_API_URL ?? "http://10.0.2.2:8000"
    : "";

export async function enviarMensagemChatbot(
  request: EnviarMensagemChatbotRequest,
): Promise<EnviarMensagemChatbotResponse> {
  const { data } = await coreApi.post<EnviarMensagemChatbotResponse>(
    `${ENDPOINT}/mensagens`,
    request,
    { baseURL: CHATBOT_API_BASE_URL },
  );
  return data;
}

export async function buscarHistoricoChatbot(): Promise<HistoricoChatbotResponse> {
  const { data } = await coreApi.get<HistoricoChatbotResponse>(
    `${ENDPOINT}/historico`,
    { baseURL: CHATBOT_API_BASE_URL },
  );
  return data;
}