"""Teste opcional em PostgreSQL real; cria e remove SOMENTE um schema aleatório."""

import asyncio
import os
from contextlib import asynccontextmanager
from uuid import uuid4

import pytest
from sqlalchemy import func, select, text
from sqlalchemy.ext.asyncio import async_sessionmaker, create_async_engine

from src import process
from src.models import Recurso, TipoServico
from src.models.base import Base
from src.models.recurso_tipos_servicos import recurso_tipos_servicos
from src.import_plan import build_plan, read_dataframe


@pytest.mark.skipif(
    not os.getenv("TEST_POSTGRES_URL"), reason="TEST_POSTGRES_URL não configurada"
)
def test_real_postgres_transaction_and_reupload(monkeypatch):
    async def scenario():
        schema = "teste_import_" + uuid4().hex
        engine = create_async_engine(
            os.environ["TEST_POSTGRES_URL"], hide_parameters=True
        )
        scoped = engine.execution_options(schema_translate_map={None: schema})
        factory = async_sessionmaker(scoped, expire_on_commit=False, autoflush=False)

        @asynccontextmanager
        async def get_db():
            async with factory() as session:
                yield session

        monkeypatch.setattr(process, "get_db", get_db)
        created = False
        try:
            async with engine.begin() as connection:
                await connection.execute(text(f'CREATE SCHEMA "{schema}"'))
            created = True
            async with scoped.begin() as connection:
                await connection.run_sync(Base.metadata.create_all)
            csv = (
                "ativo;e-mail;nome;telefone;tipo_servico;descricao_servico\n"
                "1;ana@example.com;Ana;;Tradução;Textos\n"
                "1;ana@example.com;Ana;;Revisão;Revisão\n"
            )
            plan = build_plan(read_dataframe(csv.encode()))
            first = await process.process_plan(plan)
            second = await process.process_plan(plan)
            assert first["recursos_criados"] == 1
            assert first["servicos_criados"] == first["vinculos_criados"] == 2
            assert (
                second["recursos_criados"]
                == second["servicos_criados"]
                == second["vinculos_criados"]
                == 0
            )
            async with scoped.begin() as connection:
                await connection.execute(
                    text(
                        f"ALTER TABLE \"{schema}\".recursos ADD CONSTRAINT bloquear_nome CHECK (nome <> 'Falha')"
                    )
                )
            failing = build_plan(
                read_dataframe(
                    csv.replace("ana@example.com", "erro@example.com")
                    .replace(";Ana;", ";Falha;")
                    .replace("Tradução", "Serviço Novo")
                    .encode()
                )
            )
            from sqlalchemy.exc import IntegrityError

            with pytest.raises(IntegrityError):
                await process.process_plan(failing)
            async with factory() as session:
                assert (
                    await session.scalar(select(func.count()).select_from(Recurso)) == 1
                )
                assert (
                    await session.scalar(select(func.count()).select_from(TipoServico))
                    == 2
                )
                assert (
                    await session.scalar(
                        select(func.count()).select_from(recurso_tipos_servicos)
                    )
                    == 2
                )
        finally:
            if created:
                async with engine.begin() as connection:
                    await connection.execute(text(f'DROP SCHEMA "{schema}" CASCADE'))
            await engine.dispose()

    asyncio.run(scenario())
