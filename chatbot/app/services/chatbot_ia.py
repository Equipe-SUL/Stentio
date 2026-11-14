import json
from groq import Groq
from sqlalchemy.orm import Session
from typing import Tuple, List

from core.config import settings
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

    system_prompt = f"""
    Você é o assistente virtual inteligente do sistema TMS (Stentio).
    O usuário logado possui o perfil de acesso (RBAC): {perfil_usuario}.

    Regras importantes:
    1. Responda sempre em português de forma clara, prestativa e amigável.
    2. Se o usuário perguntar sobre OS (Ordem de Serviço), Orçamentos ou Faturamento, utilize as ferramentas (tools) disponíveis.
    3. REGRA DE SEGURANÇA (RBAC): Se o perfil for 'TRADUTOR' e ele perguntar sobre faturamento, valores financeiros ou dados restritos de clientes, informe educadamente que o perfil dele não possui permissão para visualizar dados financeiros consolidados.
    4. Nunca exponha erros técnicos, nomes de tabelas ou detalhes de banco de dados.
    """

    mensagens = [{"role": "system", "content": system_prompt}]

    # Injeta o histórico recente da conversa para o Groq manter o contexto
    if historico_conversa:
        for msg in historico_conversa:
            role = "user" if msg.autor == "USUARIO" else "assistant"
            mensagens.append({"role": role, "content": msg.conteudo})

    mensagens.append({"role": "user", "content": mensagem_usuario})

    try:
        # 1ª Chamada ao Groq: decide se responde direto ou se chama uma Tool
        response = groq_client.chat.completions.create(
            model=MODELO_GROQ,
            messages=mensagens,
            tools=TOOLS_SPEC,
            tool_choice="auto",
            temperature=0.2
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
                temperature=0.2
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