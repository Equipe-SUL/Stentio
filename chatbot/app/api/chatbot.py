from fastapi import APIRouter, Depends, Header, HTTPException
from typing import Optional
import uuid
from datetime import datetime, timezone

from schemas.chatbot import (
    MensagemRequest, 
    MensagemResponse, 
    HistoricoResponse, 
    MensagemHistorico, 
    Atalho
)

router = APIRouter(
    prefix="/api/v1/ia/chatbot",
    tags=["Chatbot"]
)

# Dependência simulada de segurança (depois será movida para app/core/security.py)
def obter_usuario_logado(authorization: Optional[str] = Header(None)):
    if not authorization:
         raise HTTPException(status_code=401, detail="Não autorizado")
    # Mock do token descodificado:
    return {"usuario_id": "123", "perfil": "TRADUTOR"}

@router.post("/mensagens", response_model=MensagemResponse)
async def processar_mensagem(
    req: MensagemRequest, 
    usuario: dict = Depends(obter_usuario_logado)
):
    # Gera IDs caso seja o início da conversa
    conversa_id = req.conversaId if req.conversaId else str(uuid.uuid4())
    mensagem_id = str(uuid.uuid4())
    
    # TODO: Passar a mensagem para a camada app/services/chatbot_ia.py processar no Groq
    
    # Resposta estática para o Front-end testar
    return MensagemResponse(
        resposta=f"Olá! Vi que o seu perfil é {usuario['perfil']}. A OS está em andamento.",
        conversaId=conversa_id,
        mensagemId=mensagem_id,
        atalhos=[Atalho(tipo="OS", id="123", titulo="Abrir OS 123")]
    )

@router.get("/historico", response_model=HistoricoResponse)
async def obter_historico(
    conversaId: str, 
    usuario: dict = Depends(obter_usuario_logado)
):
    # TODO: Consultar histórico na base de dados em app/db/
    
    return HistoricoResponse(
        conversaId=conversaId,
        mensagens=[
            MensagemHistorico(
                id=str(uuid.uuid4()),
                autor="USUARIO",
                conteudo="Qual o status da OS 123?",
                criadaEm=datetime.now(timezone.utc),
                atalhos=[]
            ),
            MensagemHistorico(
                id=str(uuid.uuid4()),
                autor="IA",
                conteudo="A OS está em andamento.",
                criadaEm=datetime.now(timezone.utc),
                atalhos=[Atalho(tipo="OS", id="123", titulo="Abrir OS 123")]
            )
        ]
    )