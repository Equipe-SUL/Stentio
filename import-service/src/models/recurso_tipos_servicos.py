from sqlalchemy import Column, ForeignKey, Table, Uuid

from src.models.base import Base


recurso_tipos_servicos = Table(
    "recurso_tipos_servicos",
    Base.metadata,
    Column(
        "recurso_id",
        Uuid(as_uuid=True),
        ForeignKey("recursos.id"),
        primary_key=True,
        nullable=False,
    ), Column(
            "tipo_servico_id",
            Uuid(as_uuid=True),
            ForeignKey("tipos_servico.id"),
            primary_key=True,
            nullable=False,
        )
)