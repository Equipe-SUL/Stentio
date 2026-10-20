from sqlalchemy.orm import DeclarativeBase, Mapped, mapped_column


class Base(DeclarativeBase):
    pass


class Recurso(DeclarativeBase):
    __tablename__ = "recursos"
    id: Mapped[str] = mapped_column(primary_key=True)
    ativo: Mapped[bool] = mapped_column(default=True)
    email: Mapped[str] = mapped_column(nullable=False)
    nome: Mapped[str] = mapped_column(nullable=False)
    telefone: Mapped[str | None] = mapped_column(nullable=True)
