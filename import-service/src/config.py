from functools import lru_cache

from pydantic import Field, field_validator
from pydantic_settings import BaseSettings, SettingsConfigDict
from sqlalchemy import URL


class Settings(BaseSettings):
    MAX_UPLOAD_BYTES: int = Field(default=10 * 1024 * 1024, gt=0)
    MAX_IMPORT_ROWS: int = Field(default=5000, gt=0)
    MAX_IMPORT_RESOURCES: int = Field(default=1000, gt=0)
    CORS_ALLOWED_ORIGINS: list[str] = [
        "http://localhost",
        "http://localhost:8080",
        "http://localhost:19006",
        "http://127.0.0.1",
        "http://127.0.0.1:8080",
    ]
    POSTGRES_HOST: str = "localhost"
    POSTGRES_PORT: int = 5432
    POSTGRES_DB: str = "stentio"
    POSTGRES_USER: str = "postgres"
    POSTGRES_PASSWORD: str = "postgres"
    DATABASE_ECHO: bool = False
    LOGGER_LEVEL: str = "INFO"

    @field_validator("LOGGER_LEVEL")
    @classmethod
    def normalize_logger_level(cls, value):
        return value.upper()

    @field_validator("CORS_ALLOWED_ORIGINS")
    @classmethod
    def explicit_origins(cls, values):
        if "*" in values:
            raise ValueError("Configure origens explícitas para CORS com credenciais.")
        return [v.rstrip("/") for v in values]

    model_config = SettingsConfigDict(
        env_file=".env", env_file_encoding="utf-8", extra="ignore"
    )

    @property
    def database_url(self):
        return URL.create(
            "postgresql+asyncpg",
            username=self.POSTGRES_USER,
            password=self.POSTGRES_PASSWORD,
            host=self.POSTGRES_HOST,
            port=self.POSTGRES_PORT,
            database=self.POSTGRES_DB,
        )


@lru_cache
def get_settings():
    return Settings()


settings = get_settings()
