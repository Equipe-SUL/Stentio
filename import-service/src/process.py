"""Persistência ORM em uma transação: recursos, catálogo e associações."""

from uuid import uuid4

from sqlalchemy import func, select, text
from sqlalchemy.orm import selectinload

from src.database import get_db
from src.import_plan import (
    ImportErrorDetail,
    ImportPlan,
    build_plan,
    clean,
    service_key,
)
from src.models import Recurso, TipoServico

# Serializa importadores neste banco sem alterar as tabelas existentes.
IMPORT_LOCK_KEY = 1937010277


async def persist_plan(session, plan: ImportPlan):
    report = dict(
        recursos_criados=0,
        recursos_reutilizados=0,
        servicos_criados=0,
        servicos_reutilizados=0,
        vinculos_criados=0,
        vinculos_existentes=0,
    )
    async with session.begin():
        if session.bind.dialect.name == "postgresql":
            await session.execute(
                text("SELECT pg_advisory_xact_lock(:key)"), {"key": IMPORT_LOCK_KEY}
            )
        # Uma consulta ao catálogo; identidade normalizada em Python evita diferenças
        # de locale/casefold entre PostgreSQL e Python.
        catalog = {}
        for service in (await session.scalars(select(TipoServico))).all():
            key = service_key(service.nome)
            if key not in plan.services:
                continue
            if key in catalog:
                raise ImportErrorDetail(
                    "Há tipos de serviço ambíguos no banco. Revise o catálogo.",
                    code="SERVICO_AMBIGUO",
                    status=409,
                )
            catalog[key] = service
        for key, data in plan.services.items():
            if key in catalog:
                service = catalog[key]
                if not service.ativo:
                    raise ImportErrorDetail(
                        "O arquivo referencia um serviço inativo.",
                        code="SERVICO_INATIVO",
                        status=409,
                    )
                if data.descricao and clean(service.descricao) != data.descricao:
                    raise ImportErrorDetail(
                        "Descrição diferente de um serviço já cadastrado.",
                        code="SERVICO_CONFLITANTE",
                        status=409,
                    )
                report["servicos_reutilizados"] += 1
            else:
                catalog[key] = TipoServico(
                    id=uuid4(), ativo=True, nome=data.nome, descricao=data.descricao
                )
                report["servicos_criados"] += 1
        existing = {}
        emails = list(plan.resources)
        for start in range(0, len(emails), 500):
            stmt = (
                select(Recurso)
                .where(
                    func.lower(func.trim(Recurso.email)).in_(
                        emails[start : start + 500]
                    )
                )
                .options(selectinload(Recurso.tipos_servicos))
            )
            for resource in (await session.scalars(stmt)).all():
                key = clean(resource.email).lower()
                if key in existing:
                    raise ImportErrorDetail(
                        "Há recursos com e-mail ambíguo no banco.",
                        code="EMAIL_AMBIGUO",
                        status=409,
                    )
                existing[key] = resource
        pending = []
        for email, data in plan.resources.items():
            resource = existing.get(email)
            if resource is None:
                resource = Recurso(
                    id=uuid4(),
                    nome=data.nome,
                    email=email,
                    telefone=data.telefone,
                    ativo=data.ativo,
                    tipos_servicos=[],
                )
                report["recursos_criados"] += 1
            else:
                if (
                    clean(resource.nome),
                    clean(resource.telefone) or None,
                    resource.ativo,
                ) != (data.nome, data.telefone, data.ativo):
                    raise ImportErrorDetail(
                        "O arquivo diverge de um recurso existente. Nenhum cadastro foi alterado.",
                        code="RECURSO_CONFLITANTE",
                        status=409,
                    )
                report["recursos_reutilizados"] += 1
            linked = {service.id for service in resource.tipos_servicos}
            for key in sorted(data.services):
                service = catalog[key]
                if service.id in linked:
                    report["vinculos_existentes"] += 1
                else:
                    resource.tipos_servicos.append(service)
                    linked.add(service.id)
                    report["vinculos_criados"] += 1
            pending.append(resource)
        # Autoflush desabilitado: nenhuma escrita durante as consultas anteriores.
        session.add_all(pending)
        session.add_all(list(catalog.values()))
        # Um único commit na saída do begin; qualquer falha reverte a transação.
    return report


async def process_dataframe(df):
    """Compatibilidade com chamadas Python; valida antes de abrir a sessão."""
    return await process_plan(build_plan(df))


async def process_plan(plan):
    async with get_db() as session:
        return await persist_plan(session, plan)
