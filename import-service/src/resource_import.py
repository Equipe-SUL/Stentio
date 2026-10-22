"""Provisional CSV layout mapped to the existing Core resource API contract."""

import re
from dataclasses import dataclass, field
from decimal import Decimal
from typing import Annotated, Literal
from uuid import UUID

from pydantic import (
    BaseModel,
    EmailStr,
    Field,
    ValidationError,
    field_validator,
    model_validator,
)

from csv_validation import CsvUploadError, iter_csv_records

REQUIRED_COLUMNS = {
    "nome",
    "email",
    "tipos_servico_ids",
    "idioma_origem_id",
    "idioma_destino_id",
    "unidade",
    "valor",
}
ALLOWED_COLUMNS = REQUIRED_COLUMNS | {"telefone"}


class PricePayload(BaseModel):
    idiomaOrigemId: UUID
    idiomaDestinoId: UUID
    unidade: Literal["PALAVRA", "HORA"]
    valor: Annotated[Decimal, Field(gt=0, lt=Decimal(100000000), decimal_places=4)]

    @model_validator(mode="after")
    def different_languages(self):
        if self.idiomaOrigemId == self.idiomaDestinoId:
            raise ValueError("Os idiomas de origem e destino devem ser diferentes.")
        return self

    def key(self):
        return self.idiomaOrigemId, self.idiomaDestinoId, self.unidade


class ResourcePayload(BaseModel):
    nome: Annotated[str, Field(min_length=1, max_length=100)]
    email: Annotated[EmailStr, Field(max_length=150)]
    telefone: str | None = None
    tiposServicoIds: Annotated[list[UUID], Field(min_length=1, max_length=20)]
    precos: Annotated[list[PricePayload], Field(min_length=1, max_length=50)]

    @field_validator("nome", "email", "telefone", mode="before")
    @classmethod
    def trim(cls, value):
        return value.strip() if isinstance(value, str) else value

    @field_validator("email")
    @classmethod
    def normalize_email(cls, value):
        return str(value).lower()

    @field_validator("telefone")
    @classmethod
    def validate_phone(cls, value):
        if not value:
            return None
        if not re.fullmatch(r"[0-9+()\-\s]{8,20}", value):
            raise ValueError(
                "Telefone deve ter de 8 a 20 caracteres: números, espaços, +, (), -."
            )
        return value

    @field_validator("tiposServicoIds")
    @classmethod
    def unique_services(cls, value):
        if len(set(value)) != len(value):
            raise ValueError("Não repita tipos de serviço.")
        return sorted(value, key=str)


@dataclass
class PlannedResource:
    payload: ResourcePayload
    linhas: list[int] = field(default_factory=list)


@dataclass
class ImportPlan:
    resources: list[PlannedResource]
    total_rows: int

    def preview(self):
        return {
            "total_linhas": self.total_rows,
            "total_recursos": len(self.resources),
            "amostra_recursos": [
                {
                    "linhas": resource.linhas,
                    "dados": resource.payload.model_dump(mode="json"),
                }
                for resource in self.resources[:5]
            ],
            "validacao": "LOCAL",
        }


def _parse_resource(row: dict[str, str], line: int) -> ResourcePayload:
    value = row["valor"].strip()
    if not re.fullmatch(r"\d{1,8}(?:[.,]\d{1,4})?", value):
        raise CsvUploadError(
            422,
            "VALOR_INVALIDO",
            "Use valor positivo com até 8 dígitos inteiros e 4 decimais, sem separador de milhar.",
            linha=line,
        )
    try:
        return ResourcePayload(
            nome=row["nome"],
            email=row["email"],
            telefone=row.get("telefone"),
            tiposServicoIds=[
                item.strip() for item in row["tipos_servico_ids"].split("|")
            ],
            precos=[
                PricePayload(
                    idiomaOrigemId=row["idioma_origem_id"].strip(),
                    idiomaDestinoId=row["idioma_destino_id"].strip(),
                    unidade=row["unidade"].strip().upper(),
                    valor=Decimal(value.replace(",", ".")),
                )
            ],
        )
    except ValidationError as exc:
        first = exc.errors(include_input=False)[0]
        error = CsvUploadError(
            422,
            "DADOS_RECURSO_INVALIDOS",
            "Recurso inválido. Verifique campos obrigatórios, e-mail, telefone, UUIDs, unidade e limites.",
            linha=line,
        )
        error.detail["campo"] = ".".join(map(str, first["loc"])) or "precos"
        raise error from exc


def build_import_plan(
    contents: bytes,
    separator: str | None = None,
    max_resources: int = 100,
    max_rows: int = 5000,
) -> ImportPlan:
    grouped: dict[str, PlannedResource] = {}
    total = 0
    for line, row in iter_csv_records(contents, separator):
        total += 1
        if total == 1:
            missing = REQUIRED_COLUMNS - row.keys()
            unknown = row.keys() - ALLOWED_COLUMNS
            if missing or unknown:
                raise CsvUploadError(
                    422,
                    "LAYOUT_RECURSOS_INVALIDO",
                    f"Colunas ausentes: {', '.join(sorted(missing)) or 'nenhuma'}. "
                    f"Colunas não suportadas: {', '.join(sorted(unknown)) or 'nenhuma'}.",
                )
        if total > max_rows:
            raise CsvUploadError(
                422,
                "LIMITE_REGISTROS",
                f"O limite é {max_rows} registros por arquivo.",
                linha=line,
            )
        parsed = _parse_resource(row, line)
        email = parsed.email
        if email not in grouped:
            if len(grouped) >= max_resources:
                raise CsvUploadError(
                    422,
                    "LIMITE_RECURSOS",
                    f"O limite é {max_resources} recursos por importação.",
                    linha=line,
                )
            grouped[email] = PlannedResource(parsed, [line])
            continue

        resource = grouped[email]
        previous = resource.payload
        if (parsed.nome, parsed.telefone, parsed.tiposServicoIds) != (
            previous.nome,
            previous.telefone,
            previous.tiposServicoIds,
        ):
            raise CsvUploadError(
                422,
                "DADOS_RECURSO_DIVERGENTES",
                "Linhas do mesmo e-mail precisam ter o mesmo nome, telefone e tipos de serviço.",
                linha=line,
            )
        price = parsed.precos[0]
        if any(item.key() == price.key() for item in previous.precos):
            raise CsvUploadError(
                422,
                "PRECO_DUPLICADO",
                "Par de idiomas e unidade repetidos para o mesmo recurso.",
                linha=line,
            )
        if len(previous.precos) >= 50:
            raise CsvUploadError(
                422,
                "LIMITE_PRECOS",
                "Informe no máximo 50 preços por recurso.",
                linha=line,
            )
        previous.precos.append(price)
        resource.linhas.append(line)

    return ImportPlan(list(grouped.values()), total)
