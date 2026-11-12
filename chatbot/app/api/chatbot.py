from fastapi import APIRouter, Depends, Header, HTTPException
from sqlalchemy.orm import Session
from typing import Optional
import uuid
from datetime import datetime, timezone

from db.session import get_db
from services.chatbot_ia import processar_pergunta_ia
from schemas.chatbot import (
    MensagemRequest, 
    MensagemResponse, 
    HistoricoResponse, 
    MensagemHistorico
)

router = APIRouter(prefix="/api/v1/ia/chatbot", tags=["Chatbot"])

def obter_usuario_logado(authorization: Optional[str] = Header(None)):
    if not authorization:
         raise HTTPException(status_code=401, detail="Não autorizado")
    return {"usuario_id": "123", "perfil": "TRADUTOR"}

@router.post("/mensagens", response_model=MensagemResponse)
async def processar_mensagem(
    req: MensagemRequest, 
    usuario: dict = Depends(obter_usuario_logado),
    db: Session = Depends(get_db)
):
    conversa_id = req.conversaId if req.conversaId else str(uuid.uuid4())
    mensagem_id = str(uuid.uuid4())
    
    resposta_texto, atalhos = processar_pergunta_ia(
        mensagem_usuario=req.mensagem,
        perfil_usuario=usuario["perfil"],
        db=db
    )
    
    return MensagemResponse(
        resposta=resposta_texto,
        conversaId=conversa_id,
        mensagemId=mensagem_id,
        atalhos=atalhos
    )