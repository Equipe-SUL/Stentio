from fastapi import APIRouter, Depends, Query
from sqlalchemy.orm import Session
from pymongo import ASCENDING, DESCENDING
from typing import Optional
import uuid
from datetime import datetime, timezone

from core.security import obter_usuario_logado
from db.session import get_db
from db.mongo import mensagens_collection
from services.chatbot_ia import processar_pergunta_ia
from schemas.chatbot import (
    MensagemRequest, 
    MensagemResponse, 
    HistoricoResponse, 
    MensagemHistorico,
    Atalho
)

router = APIRouter(prefix="/api/v1/ia/chatbot", tags=["Chatbot"])


@router.post("/mensagens", response_model=MensagemResponse)
async def processar_mensagem(
    req: MensagemRequest, 
    usuario: dict = Depends(obter_usuario_logado),
    db: Session = Depends(get_db)
):
    conversa_id = req.conversaId if req.conversaId else str(uuid.uuid4())
    usuario_id = str(usuario["usuario_id"])

    # 1. Busca as últimas 10 mensagens dessa conversa no MongoDB
    historico_recente = list(
        mensagens_collection.find(
            {"conversa_id": conversa_id, "usuario_id": usuario_id}
        )
        .sort("criada_em", ASCENDING)
        .limit(10)
    )

    # 2. Salva a pergunta do USUARIO no MongoDB
    doc_usuario = {
        "_id": str(uuid.uuid4()),
        "conversa_id": conversa_id,
        "usuario_id": usuario_id,
        "autor": "USUARIO",
        "conteudo": req.mensagem,
        "atalhos": [],
        "criada_em": datetime.now(timezone.utc)
    }
    mensagens_collection.insert_one(doc_usuario)

    # 3. Processa a resposta com o Groq (passando o 'db' do Postgres para consultar OS/Orçamentos)
    resposta_texto, atalhos = processar_pergunta_ia(
        mensagem_usuario=req.mensagem,
        perfil_usuario=usuario["perfil"],
        db=db,
        historico_conversa=historico_recente
    )

    # 4. Salva a resposta da IA no MongoDB
    mensagem_ia_id = str(uuid.uuid4())
    doc_ia = {
        "_id": mensagem_ia_id,
        "conversa_id": conversa_id,
        "usuario_id": usuario_id,
        "autor": "IA",
        "conteudo": resposta_texto,
        "atalhos": [a.model_dump() for a in atalhos],
        "criada_em": datetime.now(timezone.utc)
    }
    mensagens_collection.insert_one(doc_ia)

    return MensagemResponse(
        resposta=resposta_texto,
        conversaId=conversa_id,
        mensagemId=mensagem_ia_id,
        atalhos=atalhos
    )


@router.get("/historico", response_model=HistoricoResponse)
async def obter_historico(
    conversaId: Optional[str] = Query(None, description="ID da conversa (opcional)"), 
    usuario: dict = Depends(obter_usuario_logado)
):
    usuario_id = str(usuario["usuario_id"])

    # Se não vier conversaId, busca a conversa mais recente desse usuário no MongoDB
    if not conversaId:
        ultima_msg = mensagens_collection.find_one(
            {"usuario_id": usuario_id},
            sort=[("criada_em", DESCENDING)]
        )
        conversaId = ultima_msg["conversa_id"] if ultima_msg else str(uuid.uuid4())

    # Busca todas as mensagens da conversa em ordem cronológica
    docs = mensagens_collection.find(
        {"conversa_id": conversaId, "usuario_id": usuario_id}
    ).sort("criada_em", ASCENDING)

    mensagens_formatadas = [
        MensagemHistorico(
            id=str(doc["_id"]),
            autor=doc["autor"],
            conteudo=doc["conteudo"],
            criadaEm=doc["criada_em"],
            atalhos=[Atalho(**a) for a in doc.get("atalhos", [])]
        )
        for doc in docs
    ]

    return HistoricoResponse(
        conversaId=conversaId,
        mensagens=mensagens_formatadas
    )