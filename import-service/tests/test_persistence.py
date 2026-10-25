import asyncio
from contextlib import asynccontextmanager
from uuid import uuid4

import httpx
import pytest
from sqlalchemy import event, func, select, text
from sqlalchemy.ext.asyncio import async_sessionmaker, create_async_engine
from sqlalchemy.orm import selectinload

from src import process
from src.app import app
from src.models import Recurso, TipoServico
from src.models.base import Base
from src.models.recurso_tipos_servicos import recurso_tipos_servicos
from src.import_plan import build_plan, read_dataframe

HEADER = "ativo;e-mail;nome;telefone;tipo_servico;descricao_servico\n"
ANA = "1;ana@example.com;Ana;01199990000;Tradução;Textos\n"
BIA = "1;bia@example.com;Bia;;Tradução;Textos\n"
REVIEW = "1;ana@example.com;Ana;01199990000;Revisão;Revisão de textos\n"


@asynccontextmanager
async def database(monkeypatch):
    engine = create_async_engine("sqlite+aiosqlite://")

    @event.listens_for(engine.sync_engine, "connect")
    def enable_fk(connection, record):
        cursor = connection.cursor()
        cursor.execute("PRAGMA foreign_keys=ON")
        cursor.close()

    async with engine.begin() as connection:
        await connection.run_sync(Base.metadata.create_all)
    factory = async_sessionmaker(engine, expire_on_commit=False, autoflush=False)

    @asynccontextmanager
    async def get_db():
        async with factory() as session:
            yield session

    monkeypatch.setattr(process, "get_db", get_db)
    try:
        yield engine, factory
    finally:
        await engine.dispose()


async def post(data):
    async with httpx.AsyncClient(
        transport=httpx.ASGITransport(app), base_url="http://test"
    ) as client:
        return await client.post(
            "/upload-csv", files={"file": ("recursos.csv", data.encode(), "text/csv")}
        )


async def counts(factory):
    async with factory() as session:
        return tuple(
            [
                await session.scalar(select(func.count()).select_from(model))
                for model in (Recurso, TipoServico, recurso_tipos_servicos)
            ]
        )


def test_complete_flow_one_commit_reupload_and_grouping(monkeypatch):
    async def scenario():
        async with database(monkeypatch) as (engine, factory):
            commits = []
            event.listen(engine.sync_engine, "commit", lambda conn: commits.append(1))
            response = await post(HEADER + ANA + REVIEW + BIA + ANA)
            assert response.status_code == 200, response.text
            body = response.json()
            assert body["total_linhas"] == 4
            assert body["recursos_criados"] == 2
            assert body["servicos_criados"] == 2
            assert body["vinculos_criados"] == 3
            assert len(commits) == 1
            assert await counts(factory) == (2, 2, 3)
            response = await post(HEADER + ANA + REVIEW + BIA)
            assert response.status_code == 200, response.text
            assert response.json()["vinculos_criados"] == 0
            assert response.json()["vinculos_existentes"] == 3
            assert await counts(factory) == (2, 2, 3)
            async with factory() as session:
                ana = (
                    await session.scalars(
                        select(Recurso)
                        .where(Recurso.email == "ana@example.com")
                        .options(selectinload(Recurso.tipos_servicos))
                    )
                ).one()
                assert {s.nome for s in ana.tipos_servicos} == {"Tradução", "Revisão"}
                assert ana.telefone == "01199990000"

    asyncio.run(scenario())


def test_reuse_existing_resource_and_service_add_only_missing_link(monkeypatch):
    async def scenario():
        async with database(monkeypatch) as (_, factory):
            assert (await post(HEADER + ANA)).status_code == 200
            response = await post(HEADER + ANA + REVIEW)
            assert response.status_code == 200, response.text
            assert response.json()["recursos_reutilizados"] == 1
            assert response.json()["servicos_reutilizados"] == 1
            assert response.json()["servicos_criados"] == 1
            assert response.json()["vinculos_criados"] == 1
            assert await counts(factory) == (1, 2, 2)

    asyncio.run(scenario())


def test_resource_conflict_does_not_write_other_rows(monkeypatch):
    async def scenario():
        async with database(monkeypatch) as (_, factory):
            await post(HEADER + ANA)
            response = await post(
                HEADER
                + BIA
                + ANA.replace(";Ana;", ";Outro;")
                + REVIEW.replace(";Ana;", ";Outro;")
            )
            assert response.status_code == 409
            assert await counts(factory) == (1, 1, 1)

    asyncio.run(scenario())


def test_database_failure_during_flush_rolls_back_every_table(monkeypatch):
    async def scenario():
        async with database(monkeypatch) as (engine, factory):
            await post(HEADER + ANA)
            async with engine.begin() as connection:
                await connection.execute(
                    text(
                        "CREATE TRIGGER bloquear_vinculo BEFORE INSERT ON recurso_tipos_servicos BEGIN SELECT RAISE(ABORT, 'forced failure'); END"
                    )
                )
            response = await post(HEADER + REVIEW + BIA)
            assert response.status_code == 409, response.text
            # O erro ocorre na associação, depois dos INSERTs dos novos registros.
            assert await counts(factory) == (1, 1, 1)

    asyncio.run(scenario())


@pytest.mark.parametrize(
    "inactive,duplicate,description",
    [(True, False, False), (False, True, False), (False, False, True)],
)
def test_catalog_errors_abort_batch(monkeypatch, inactive, duplicate, description):
    async def scenario():
        async with database(monkeypatch) as (_, factory):
            async with factory.begin() as session:
                session.add(
                    TipoServico(
                        id=uuid4(),
                        nome="Tradução",
                        ativo=not inactive,
                        descricao="Outro" if description else "Textos",
                    )
                )
                if duplicate:
                    session.add(
                        TipoServico(
                            id=uuid4(),
                            nome=" tradução ",
                            ativo=True,
                            descricao="Textos",
                        )
                    )
            before = await counts(factory)
            response = await post(HEADER + ANA + REVIEW)
            assert response.status_code == 409, response.text
            assert await counts(factory) == before

    asyncio.run(scenario())


def test_normalized_identity_and_optional_description(monkeypatch):
    async def scenario():
        async with database(monkeypatch) as (_, factory):
            await post(HEADER + ANA)
            response = await post(
                HEADER
                + ANA.replace("ana@example.com", " ANA@EXAMPLE.COM ").replace(
                    "Tradução;Textos", " tradução ;"
                )
            )
            assert response.status_code == 200, response.text
            assert response.json()["servicos_reutilizados"] == 1
            assert response.json()["vinculos_existentes"] == 1
            assert await counts(factory) == (1, 1, 1)

    asyncio.run(scenario())
