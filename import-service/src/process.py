from polars import DataFrame
from sqlalchemy.dialects.postgresql import insert

from src.database import get_db
from src.models.recursos import Recurso


async def processar_arquivo(df: DataFrame):
    payload = df.to_dicts()
    async with get_db() as session:
        await session.execute(insert(Recurso).on_conflict_do_nothing(index_elements=["email"]), payload)
