"""Leitura estrutural e plano validado, sem acesso ao banco."""

import csv
import io
import unicodedata
from dataclasses import dataclass, field

import polars as pl
from pydantic import EmailStr, TypeAdapter, ValidationError

EMAIL = TypeAdapter(EmailStr)
COLUMNS = ["ativo", "e-mail", "nome", "telefone", "tipo_servico", "descricao_servico"]


class ImportErrorDetail(Exception):
    def __init__(self, message: str, *, code="CSV_INVALIDO", line=None, status=422):
        super().__init__(message)
        self.status = status
        self.detail = {"codigo": code, "mensagem": message}
        if line is not None:
            self.detail["linha"] = line


def clean(value):
    return unicodedata.normalize("NFC", (value or "").strip())


def service_key(value):
    return " ".join(clean(value).split()).casefold()


@dataclass
class ServiceData:
    nome: str
    descricao: str | None


@dataclass
class ResourceData:
    nome: str
    email: str
    telefone: str | None
    ativo: bool
    services: set[str] = field(default_factory=set)


@dataclass
class ImportPlan:
    resources: dict[str, ResourceData]
    services: dict[str, ServiceData]
    lines: int

    def preview(self):
        return {
            "total_linhas": self.lines,
            "total_recursos": len(self.resources),
            "total_servicos": len(self.services),
            "total_vinculos": sum(len(r.services) for r in self.resources.values()),
            "amostra_recursos": [
                {
                    "nome": r.nome,
                    "email": r.email,
                    "ativo": r.ativo,
                    "telefone": r.telefone,
                    "servicos": [self.services[k].nome for k in sorted(r.services)],
                }
                for r in list(self.resources.values())[:5]
            ],
        }


def read_dataframe(contents: bytes, separator: str = ";", max_rows=5000):
    if separator not in (";", ","):
        raise ImportErrorDetail("Use separador ponto e vírgula ou vírgula.")
    try:
        text = contents.decode("utf-8-sig")
    except UnicodeDecodeError as exc:
        raise ImportErrorDetail(
            "Salve o arquivo em UTF-8.", code="CODIFICACAO_INVALIDA"
        ) from exc
    if not text.strip():
        raise ImportErrorDetail("O arquivo está vazio.")
    if any(ord(c) < 32 and c not in "\r\n\t" for c in text):
        raise ImportErrorDetail("O arquivo contém caracteres de controle inválidos.")
    try:
        reader = csv.reader(
            io.StringIO(text, newline=""), delimiter=separator, strict=True
        )
        header = next(reader)
        if len(header) != len(set(header)) or set(header) != set(COLUMNS):
            raise ImportErrorDetail(
                "Cabeçalho esperado: " + separator.join(COLUMNS),
                code="CABECALHO_INVALIDO",
            )
        rows = 0
        for row in reader:
            if not row:
                continue
            if len(row) != len(header):
                raise ImportErrorDetail(
                    "Quantidade de campos diferente do cabeçalho.", line=reader.line_num
                )
            rows += 1
            if rows > max_rows:
                raise ImportErrorDetail(
                    f"O limite é {max_rows} registros.", code="LIMITE_LINHAS"
                )
        if not rows:
            raise ImportErrorDetail("O CSV precisa conter pelo menos um registro.")
        # Inferência desativada para preservar telefones e todos os valores como texto.
        df = pl.read_csv(text.encode("utf-8"), separator=separator, infer_schema=False)
        return df.rename({"e-mail": "email"})
    except ImportErrorDetail:
        raise
    except (csv.Error, StopIteration, pl.exceptions.PolarsError) as exc:
        raise ImportErrorDetail(
            "Estrutura CSV inválida; confira separador e aspas."
        ) from exc


def build_plan(df: pl.DataFrame, max_resources=1000) -> ImportPlan:
    resources, services = {}, {}
    for line, row in enumerate(df.to_dicts(), start=2):
        if set(row) != {
            "ativo",
            "email",
            "nome",
            "telefone",
            "tipo_servico",
            "descricao_servico",
        }:
            raise ImportErrorDetail("DataFrame fora do layout contratado.")
        nome, telefone = clean(row["nome"]), clean(row["telefone"]) or None
        if not nome or len(nome) > 255:
            raise ImportErrorDetail(
                "Nome obrigatório, com até 255 caracteres.", line=line
            )
        try:
            email = str(EMAIL.validate_python(clean(row["email"]))).lower()
        except ValidationError as exc:
            raise ImportErrorDetail("E-mail inválido.", line=line) from exc
        if len(email) > 254 or (telefone and len(telefone) > 20):
            raise ImportErrorDetail(
                "E-mail deve ter até 254 caracteres; telefone, até 20.", line=line
            )
        if clean(row["ativo"]) not in ("0", "1"):
            raise ImportErrorDetail("Ativo deve ser 0 ou 1.", line=line)
        ativo = clean(row["ativo"]) == "1"
        service_name = " ".join(clean(row["tipo_servico"]).split())
        description = clean(row["descricao_servico"]) or None
        if not service_name or len(service_name) > 255:
            raise ImportErrorDetail(
                "Tipo de serviço obrigatório, com até 255 caracteres.", line=line
            )
        if description and len(description) > 500:
            raise ImportErrorDetail(
                "Descrição do serviço deve ter até 500 caracteres.", line=line
            )
        key = service_key(service_name)
        if key in services:
            old = services[key]
            if old.descricao and description and old.descricao != description:
                raise ImportErrorDetail(
                    "Descrições divergentes para o mesmo serviço no arquivo.", line=line
                )
            if description and not old.descricao:
                old.descricao = description
        else:
            services[key] = ServiceData(service_name, description)
        if email in resources:
            resource = resources[email]
            if (resource.nome, resource.telefone, resource.ativo) != (
                nome,
                telefone,
                ativo,
            ):
                raise ImportErrorDetail(
                    "Dados divergentes para o mesmo e-mail no arquivo.", line=line
                )
        else:
            if len(resources) >= max_resources:
                raise ImportErrorDetail(
                    f"O limite é {max_resources} recursos.", code="LIMITE_RECURSOS"
                )
            resource = resources[email] = ResourceData(nome, email, telefone, ativo)
        resource.services.add(key)
    if not resources:
        raise ImportErrorDetail("Não há recursos para importar.")
    return ImportPlan(resources, services, df.height)
