import json
import uuid

import polars as pl
from loguru import logger
from polars import DataFrame
from sqlalchemy.dialects.postgresql import insert

from src.config import settings
from src.database import get_db
from src.models.recursos import Recurso

NAMESPACE = uuid.uuid5(uuid.NAMESPACE_DNS, "stentio-import-service")


async def process_dataframe(df: DataFrame):

    logger.debug("iniciando processamento")

    df = df.with_columns(
        pl.struct(["nome", "email"])
        .map_elements(
            lambda row: str(uuid.uuid5(NAMESPACE, f"{row['nome']}:{row['email']}")),
            return_dtype=pl.Object,
        )
        .alias("id")
    )

    payload = df.to_dicts()

    logger.debug(json.dumps(payload, indent=2))

    logger.debug(f"connection string: {settings.database_url}")

    async with get_db() as session:
        result = await session.execute(
            insert(Recurso)
            .on_conflict_do_nothing(index_elements=["email"])
            .returning(Recurso.id, Recurso.email, Recurso.nome),
            payload,
        )
        await session.commit()

        inserted = result.mappings().all()

        logger.info(f"{len(inserted)} out of {df.height} were inserted")
