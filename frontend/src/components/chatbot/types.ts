export type ChatbotAtalhoTipo = "OS" | "ORCAMENTO" | "CLIENTE";

export interface ChatbotAtalho {
  tipo: ChatbotAtalhoTipo;
  id: string;
  titulo: string;
  rota?: string;
}

export interface ChatbotMensagem {
  id: string;
  autor: "USUARIO" | "ASSISTENTE";
  conteudo: string;
  criadaEm?: string;
  atalhos?: ChatbotAtalho[];
}

export interface EnviarMensagemChatbotRequest {
  mensagem: string;
  conversaId?: string;
}

export interface EnviarMensagemChatbotResponse {
  resposta: string;
  conversaId?: string;
  mensagemId?: string;
  atalhos?: ChatbotAtalho[];
}

export interface HistoricoChatbotResponse {
  conversaId?: string;
  mensagens: ChatbotMensagem[];
}
