from contextlib import asynccontextmanager

from sqlalchemy.ext.asyncio import async_sessionmaker, create_async_engine

from src.config import settings

async_engine = create_async_engine(
    settings.database_url,
    echo=settings.DATABASE_ECHO,
    hide_parameters=True,
    pool_pre_ping=True,
)
AsyncSessionLocal = async_sessionmaker(
    bind=async_engine, expire_on_commit=False, autoflush=False
)


@asynccontextmanager
async def get_db():
    async with AsyncSessionLocal() as session:
        yield session
