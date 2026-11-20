import uuid

from db.session import Base
from sqlalchemy import (
    BigInteger,
    Boolean,
    Column,
    DateTime,
    ForeignKey,
    Integer,
    Numeric,
    String,
)
from sqlalchemy.dialects.postgresql import UUID
from sqlalchemy.orm import Mapped, mapped_column


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


class Usuario(Base):
    __tablename__ = "usuarios"

    id: Mapped[uuid.UUID] = mapped_column(
        UUID(as_uuid=True), primary_key=True, default=uuid.uuid4
    )
    ativo: Mapped[bool] = mapped_column(Boolean, default=True)
    email: Mapped[str | None] = mapped_column(String(255))
    nome: Mapped[str | None] = mapped_column(String(255))
    role: Mapped[str | None] = mapped_column(String(255))
    senha: Mapped[str | None] = mapped_column(String(255))
