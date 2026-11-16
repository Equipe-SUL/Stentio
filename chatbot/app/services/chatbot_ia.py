import json
from groq import Groq
from sqlalchemy.orm import Session
from typing import Tuple, List

from core.config import settings
from services.rag_service import buscar_contexto_manual
from schemas.chatbot import Atalho
from tools.os_tools import (
    consultar_os_no_banco,
    consultar_orcamentos_no_banco,
    consultar_faturamento
)

groq_client = Groq(api_key=settings.GROQ_API_KEY)
MODELO_GROQ = "qwen/qwen3.8-27b"

# Definição das ferramentas que o Groq pode decidir chamar
TOOLS_SPEC = [
    {
        "type": "function",
        "function": {
            "name": "consultar_os_no_banco",
            "description": "Consulta o status de uma Ordem de Serviço (OS) pelo nome do cliente/empresa ou pelo ID da OS.",
            "parameters": {
                "type": "object",
                "properties": {
                    "termo_busca": {
                        "type": "string",
                        "description": "Nome da empresa/cliente (ex: 'XPTO') ou UUID da OS."
                    }
                },
                "required": ["termo_busca"]
            }
        }
    },
    {
        "type": "function",
        "function": {
            "name": "consultar_orcamentos_no_banco",
            "description": "Consulta o status de um Orçamento pelo nome do cliente ou ID.",
            "parameters": {
                "type": "object",
                "properties": {
                    "termo_busca": {
                        "type": "string",
                        "description": "Nome da empresa/cliente ou UUID do orçamento."
                    }
                },
                "required": ["termo_busca"]
            }
        }
    },
    {
        "type": "function",
        "function": {
            "name": "consultar_faturamento",
            "description": "Consulta dados financeiros, faturamento total do mês ou valores consolidados.",
            "parameters": {
                "type": "object",
                "properties": {}
            }
        }
    }
]

def processar_pergunta_ia(
    mensagem_usuario: str, 
    perfil_usuario: str, 
    db: Session,
    historico_conversa: list = None
) -> Tuple[str, List[Atalho]]:
    atalhos_acumulados: List[Atalho] = []

    contexto_rag = buscar_contexto_manual(mensagem_usuario)
    bloco_manual = ""
    if contexto_rag:
        bloco_manual = f"""

    ### DOCUMENTAÇÃO OFICIAL DO SISTEMA (USE PARA RESPONDER DÚVIDAS OPERACIONAIS):
    {contexto_rag}
    """

    system_prompt = f"""
    Você é o assistente virtual oficial do sistema Stentio (TMS de tradução).
    Perfil do usuário atual: {perfil_usuario}.

    REGRAS OBRIGATÓRIAS:
    1. Para consultas de dados reais (status de OS, orçamentos, faturamento), use as ferramentas disponíveis.
    2. Para dúvidas sobre como usar o sistema, fluxos, permissões ou erros (ex: SMTP, aprovação de orçamento, alocação), responda com base na DOCUMENTAÇÃO OFICIAL abaixo.
    3. Nunca invente telas ou funcionalidades que não estejam na documentação (ex: emissão de notas fiscais ou faturas ainda não existem no sistema).
    4. Respeite estritamente o RBAC: se o perfil for TRADUTOR ou RECURSO, nunca revele valores financeiros de orçamentos ou faturamento.
    5. Seja direto, claro e objetivo em português.
    {bloco_manual}
    """

    mensagens = [{"role": "system", "content": system_prompt}]

    # Injeta o histórico recente da conversa para o Groq manter o contexto
    # Injeta o histórico recente da conversa vinda do MongoDB
    if historico_conversa:
        for msg in historico_conversa:
            role = "user" if msg["autor"] == "USUARIO" else "assistant"
            mensagens.append({"role": role, "content": msg["conteudo"]})

    mensagens.append({"role": "user", "content": mensagem_usuario})

    try:
        # 1ª Chamada ao Groq: decide se responde direto ou se chama uma Tool
        response = groq_client.chat.completions.create(
            model=MODELO_GROQ,
            messages=mensagens,
            tools=TOOLS_SPEC,
            tool_choice="auto",
            temperature=0.2,
            max_tokens=500
        )

        resposta_msg = response.choices[0].message
        tool_calls = resposta_msg.tool_calls

        # Se a IA decidiu consultar o banco de dados:
        if tool_calls:
            mensagens.append(resposta_msg)

            for tool_call in tool_calls:
                nome_funcao = tool_call.function.name
                argumentos = json.loads(tool_call.function.arguments or "{}")

                if nome_funcao == "consultar_os_no_banco":
                    resultado_texto, novos_atalhos = consultar_os_no_banco(
                        db, argumentos.get("termo_busca", "")
                    )
                elif nome_funcao == "consultar_orcamentos_no_banco":
                    resultado_texto, novos_atalhos = consultar_orcamentos_no_banco(
                        db, argumentos.get("termo_busca", ""), perfil_usuario
                    )
                elif nome_funcao == "consultar_faturamento":
                    resultado_texto, novos_atalhos = consultar_faturamento(db, perfil_usuario)
                else:
                    resultado_texto, novos_atalhos = "Função não reconhecida.", []

                atalhos_acumulados.extend(novos_atalhos)

                mensagens.append({
                    "tool_call_id": tool_call.id,
                    "role": "tool",
                    "name": nome_funcao,
                    "content": resultado_texto,
                })

            # 2ª Chamada ao Groq: formula a resposta final em linguagem natural com os dados do banco
            segunda_resposta = groq_client.chat.completions.create(
                model=MODELO_GROQ,
                messages=mensagens,
                temperature=0.2,
                max_tokens=500
            )
            return segunda_resposta.choices[0].message.content, atalhos_acumulados

        return resposta_msg.content, atalhos_acumulados

    except Exception as e:
        # RN.2 - Retornar mensagem amigável sem expor problemas técnicos
        print(f"Erro interno na IA/DB: {e}")
        return (
            "Desculpe, não consegui processar a sua solicitação no momento. "
            "Pode tentar reformular a pergunta?",
            []
        )