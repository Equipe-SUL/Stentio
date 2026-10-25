from uuid import UUID

from sqlalchemy.orm import Mapped, mapped_column, relationship

from src.models.base import Base

from typing import TYPE_CHECKING

from src.models.recurso_tipos_servicos import recurso_tipos_servicos

if TYPE_CHECKING:
    from src.models.tipos_servicos import TipoServico


class Recurso(Base):
    __tablename__ = "recursos"
    id: Mapped[UUID] = mapped_column(primary_key=True)
    ativo: Mapped[bool] = mapped_column(default=True)
    email: Mapped[str] = mapped_column(nullable=False)
    nome: Mapped[str] = mapped_column(nullable=False)
    telefone: Mapped[str | None] = mapped_column(nullable=True)
    tipos_servicos: Mapped[list["TipoServico"]] = relationship(
    "TipoServico",
    secondary=recurso_tipos_servicos,
    back_populates="recursos",
    )
