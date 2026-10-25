from typing import Literal

from pydantic import BaseModel


class ResourcePreview(BaseModel):
    nome: str
    email: str
    telefone: str | None
    ativo: bool
    servicos: list[str]


class ValidationSummary(BaseModel):
    status: Literal["Validado"]
    filename: str
    total_linhas: int
    total_recursos: int
    total_servicos: int
    total_vinculos: int
    amostra_recursos: list[ResourcePreview]


class UploadSummary(BaseModel):
    status: Literal["Concluido"]
    filename: str
    total_linhas: int
    total_recursos: int
    total_servicos: int
    recursos_criados: int
    recursos_reutilizados: int
    servicos_criados: int
    servicos_reutilizados: int
    vinculos_criados: int
    vinculos_existentes: int
