import { coreApi } from "../../lib/api";
import type {
  EnviarMensagemChatbotRequest,
  EnviarMensagemChatbotResponse,
  HistoricoChatbotResponse,
} from "./types";

const ENDPOINT = "/api/v1/ia/chatbot";

export async function enviarMensagemChatbot(
  request: EnviarMensagemChatbotRequest,
): Promise<EnviarMensagemChatbotResponse> {
  const { data } = await coreApi.post<EnviarMensagemChatbotResponse>(
    `${ENDPOINT}/mensagens`,
    request,
  );
  return data;
}

export async function buscarHistoricoChatbot(): Promise<HistoricoChatbotResponse> {
  const { data } = await coreApi.get<HistoricoChatbotResponse>(
    `${ENDPOINT}/historico`,
  );
  return data;
}
