from datetime import datetime
from typing import Literal

from pydantic import BaseModel

# --- Modelos de Resposta ---


class Atalho(BaseModel):
    tipo: Literal["OS", "ORCAMENTO", "CLIENTE"]
    id: str
    titulo: str


class MensagemRequest(BaseModel):
    mensagem: str
    conversaId: str | None = None


class MensagemResponse(BaseModel):
    resposta: str
    conversaId: str
    mensagemId: str
    atalhos: list[Atalho] = []


class MensagemHistorico(BaseModel):
    id: str
    autor: Literal["USUARIO", "IA"]
    conteudo: str
    criadaEm: datetime
    atalhos: list[Atalho] = []


class HistoricoResponse(BaseModel):
    conversaId: str
    mensagens: list[MensagemHistorico]
