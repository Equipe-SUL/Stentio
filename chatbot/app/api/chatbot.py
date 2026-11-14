from fastapi import APIRouter, Depends, Security, HTTPException, Query
from fastapi.security import APIKeyHeader
from sqlalchemy.orm import Session
from typing import Optional
import uuid
from datetime import datetime, timezone

from db.session import get_db
from db.models import ChatbotMensagem
from services.chatbot_ia import processar_pergunta_ia
from schemas.chatbot import (
    MensagemRequest, 
    MensagemResponse, 
    HistoricoResponse, 
    MensagemHistorico,
    Atalho
)

router = APIRouter(prefix="/api/v1/ia/chatbot", tags=["Chatbot"])

api_key_header = APIKeyHeader(name="Authorization", auto_error=False)

def obter_usuario_logado(authorization: Optional[str] = Security(api_key_header)):
    if not authorization:
        raise HTTPException(status_code=401, detail="Não autorizado")
    
    perfil = "ADMIN" if "ADMIN" in authorization.upper() else "TRADUTOR"
    return {"usuario_id": "123", "perfil": perfil}


@router.post("/mensagens", response_model=MensagemResponse)
async def processar_mensagem(
    req: MensagemRequest, 
    usuario: dict = Depends(obter_usuario_logado),
    db: Session = Depends(get_db)
):
    conversa_id = req.conversaId if req.conversaId else str(uuid.uuid4())
    usuario_id = str(usuario["usuario_id"])

    # 1. Busca as últimas 10 mensagens dessa conversa para dar contexto à IA
    historico_recente = (
        db.query(ChatbotMensagem)
        .filter(
            ChatbotMensagem.conversa_id == conversa_id,
            ChatbotMensagem.usuario_id == usuario_id
        )
        .order_by(ChatbotMensagem.criada_em.asc())
        .limit(10)
        .all()
    )

    # 2. Salva a pergunta do USUARIO no banco
    msg_usuario = ChatbotMensagem(
        id=uuid.uuid4(),
        conversa_id=conversa_id,
        usuario_id=usuario_id,
        autor="USUARIO",
        conteudo=req.mensagem,
        atalhos=[],
        criada_em=datetime.now(timezone.utc)
    )
    db.add(msg_usuario)

    # 3. Processa a resposta com o Groq
    resposta_texto, atalhos = processar_pergunta_ia(
        mensagem_usuario=req.mensagem,
        perfil_usuario=usuario["perfil"],
        db=db,
        historico_conversa=historico_recente
    )

    # 4. Salva a resposta da IA no banco (convertendo os objetos Atalho para dict/JSON)
    mensagem_ia_uuid = uuid.uuid4()
    atalhos_json = [a.model_dump() for a in atalhos]

    msg_ia = ChatbotMensagem(
        id=mensagem_ia_uuid,
        conversa_id=conversa_id,
        usuario_id=usuario_id,
        autor="IA",
        conteudo=resposta_texto,
        atalhos=atalhos_json,
        criada_em=datetime.now(timezone.utc)
    )
    db.add(msg_ia)
    db.commit()

    return MensagemResponse(
        resposta=resposta_texto,
        conversaId=conversa_id,
        mensagemId=str(mensagem_ia_uuid),
        atalhos=atalhos
    )


@router.get("/historico", response_model=HistoricoResponse)
async def obter_historico(
    conversaId: Optional[str] = Query(None, description="ID da conversa (opcional)"), 
    usuario: dict = Depends(obter_usuario_logado),
    db: Session = Depends(get_db)
):
    usuario_id = str(usuario["usuario_id"])

    # Se o front-end não mandar conversaId, pega a conversa mais recente do usuário logado
    if not conversaId:
        ultima_msg = (
            db.query(ChatbotMensagem)
            .filter(ChatbotMensagem.usuario_id == usuario_id)
            .order_by(ChatbotMensagem.criada_em.desc())
            .first()
        )
        conversaId = ultima_msg.conversa_id if ultima_msg else str(uuid.uuid4())

    # Busca todas as mensagens da conversa em ordem cronológica
    mensagens_db = (
        db.query(ChatbotMensagem)
        .filter(
            ChatbotMensagem.conversa_id == conversaId,
            ChatbotMensagem.usuario_id == usuario_id
        )
        .order_by(ChatbotMensagem.criada_em.asc())
        .all()
    )

    mensagens_formatadas = [
        MensagemHistorico(
            id=str(m.id),
            autor=m.autor,
            conteudo=m.conteudo,
            criadaEm=m.criada_em,
            atalhos=[Atalho(**a) for a in (m.atalhos or [])]
        )
        for m in mensagens_db
    ]

    return HistoricoResponse(
        conversaId=conversaId,
        mensagens=mensagens_formatadas
    )