from uuid import UUID

from sqlalchemy import String
from sqlalchemy.orm import Mapped, mapped_column, relationship
from typing import TYPE_CHECKING
from src.models.recurso_tipos_servicos import recurso_tipos_servicos

from src.models.base import Base

if TYPE_CHECKING:
    from src.models.recursos import Recurso


class TipoServico(Base):
    __tablename__ = "tipos_servico"
    id: Mapped[UUID] = mapped_column(primary_key=True)
    ativo: Mapped[bool] = mapped_column(nullable=False)
    descricao: Mapped[str | None] = mapped_column(String(500), nullable=True)
    nome: Mapped[str] = mapped_column(String(255), nullable=False)
    recursos: Mapped[list["Recurso"]] = relationship(
        "Recurso",
        secondary=recurso_tipos_servicos,
        back_populates="tipos_servicos",
    )
