from sqlalchemy import Column, String, Numeric, BigInteger, Integer, DateTime, ForeignKey
from sqlalchemy.dialects.postgresql import UUID
from db.session import Base
import uuid

class Cliente(Base):
    __tablename__ = "clientes"

    id = Column(UUID(as_uuid=True), primary_key=True, default=uuid.uuid4)
    cpf_cnpj = Column(String(14))
    data_cadastro = Column(DateTime(timezone=False))
    data_modificacao = Column(DateTime(timezone=False))
    email_representante = Column(String(150))
    nome_empresa = Column(String(150))
    nome_representante = Column(String(100))
    telefone = Column(String(20))

class Orcamento(Base):
    __tablename__ = "orcamentos"

    id = Column(UUID(as_uuid=True), primary_key=True, default=uuid.uuid4)
    data_expiracao = Column(DateTime(timezone=True))
    status = Column(String(30))
    token_hash = Column(String(64))
    token_utilizado_em = Column(DateTime(timezone=True))
    valor_total = Column(Numeric(19, 2))
    versao = Column(BigInteger)
    cliente_id = Column(UUID(as_uuid=True), ForeignKey("clientes.id"))
    atualizado_em = Column(DateTime(timezone=True))
    criado_em = Column(DateTime(timezone=True))
    numero_versao = Column(Integer)
    solicitacao_id = Column(UUID(as_uuid=True))

class OrdemServico(Base):
    __tablename__ = "ordens_servico"

    id = Column(UUID(as_uuid=True), primary_key=True, default=uuid.uuid4)
    criado_em = Column(DateTime(timezone=True))
    status = Column(String(30))
    orcamento_id = Column(UUID(as_uuid=True), ForeignKey("orcamentos.id"))
    solicitacao_id = Column(UUID(as_uuid=True))