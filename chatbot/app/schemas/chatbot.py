from pydantic import BaseModel
from typing import List, Optional, Literal
from datetime import datetime

class Atalho(BaseModel):
    tipo: Literal["OS", "ORCAMENTO", "CLIENTE"]
    id: str
    titulo: str

class MensagemRequest(BaseModel):
    mensagem: str
    conversaId: Optional[str] = None

class MensagemResponse(BaseModel):
    resposta: str
    conversaId: str
    mensagemId: str
    atalhos: List[Atalho] = []

class MensagemHistorico(BaseModel):
    id: str
    autor: Literal["USUARIO", "IA"]
    conteudo: str
    criadaEm: datetime
    atalhos: List[Atalho] = []

class HistoricoResponse(BaseModel):
    conversaId: str
    mensagens: List[MensagemHistorico]