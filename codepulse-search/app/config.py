from functools import lru_cache
from pathlib import Path

from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
    """Runtime config loaded from `.env` and the process environment."""

    model_config = SettingsConfigDict(
        env_file=".env",
        env_file_encoding="utf-8",
        extra="ignore",
    )

    database_url: str = "postgresql://codepulse:codepulse@localhost:5432/codepulse"
    jwt_public_key_path: Path = Path("../backend/src/main/resources/public.key")
    jwt_issuer: str = "codepulse-dev"

    embedding_provider: str = "local"
    embedding_model_name: str = "sentence-transformers/all-MiniLM-L6-v2"
    embedding_dimensions: int = 384

    llm_api_key: str = ""
    llm_model_name: str = "gpt-4o-mini"
    llm_api_base_url: str = "https://api.openai.com/v1"

    service_port: int = 8090
    log_level: str = "INFO"


@lru_cache
def get_settings() -> Settings:
    return Settings()
