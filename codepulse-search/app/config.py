from functools import lru_cache
from pathlib import Path

from pydantic_settings import BaseSettings, SettingsConfigDict

# codepulse-search/ root (parent of app/)
PROJECT_ROOT = Path(__file__).resolve().parent.parent


class Settings(BaseSettings):
    """Runtime config from `.env` and the process environment.

    Defaults target a normal Windows work laptop + local Ollama (open source).
    """

    model_config = SettingsConfigDict(
        env_file=PROJECT_ROOT / ".env",
        env_file_encoding="utf-8",
        extra="ignore",
    )

    database_url: str = "postgresql://codepulse:codepulse@localhost:5432/codepulse"

    # Relative paths are resolved against codepulse-search/ (works on Windows & Linux).
    jwt_public_key_path: Path = Path("../backend/src/main/resources/public.key")
    jwt_issuer: str = "codepulse-dev"

    embedding_provider: str = "local"
    embedding_model_name: str = "sentence-transformers/all-MiniLM-L6-v2"
    embedding_dimensions: int = 384

    # Local LLM via Ollama (open source, no cloud key required).
    llm_provider: str = "ollama"
    llm_api_key: str = "ollama"
    llm_model_name: str = "llama3.2:1b"
    llm_api_base_url: str = "http://127.0.0.1:11434/v1"

    service_port: int = 8090
    log_level: str = "INFO"

    # Continuous learning: poll Spring + knowledge tables and index deltas.
    auto_ingest_enabled: bool = True
    auto_ingest_interval_seconds: int = 120

    def resolved_jwt_public_key_path(self) -> Path:
        path = self.jwt_public_key_path
        if not path.is_absolute():
            path = (PROJECT_ROOT / path).resolve()
        return path


@lru_cache
def get_settings() -> Settings:
    return Settings()
