from contextlib import asynccontextmanager

from loguru import logger
from sqlalchemy.ext.asyncio import async_sessionmaker, create_async_engine

from src.config import settings

async_engine = create_async_engine(
    url=settings.database_url, echo=settings.DATABASE_ECHO
)

AsyncSessionLocal = async_sessionmaker(bind=async_engine, expire_on_commit=False)


@asynccontextmanager
async def get_db():
    async with AsyncSessionLocal() as session:
        try:
            yield session
        except Exception:
            await session.rollback()
            raise


if __name__ == "__main__":
    import asyncio

    from sqlalchemy import text

    async def test_connection():
        async with get_db() as session:
            result = await session.execute(text("SELECT 1 FROM recursos"))
            logger.info("database is online!")
            logger.info(f"entries found: {len(result.fetchall())}")

    asyncio.run(test_connection())
