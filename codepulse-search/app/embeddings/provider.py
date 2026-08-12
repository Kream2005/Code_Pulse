from abc import ABC, abstractmethod

from app.config import get_settings


class EmbeddingProvider(ABC):
    @abstractmethod
    def embed(self, texts: list[str]) -> list[list[float]]:
        """Return one embedding vector per input text."""


class LocalSentenceTransformerProvider(EmbeddingProvider):
    """Local models via sentence-transformers. Hosted providers can be added later."""

    def embed(self, texts: list[str]) -> list[list[float]]:
        # TODO: load EMBEDDING_MODEL_NAME once and encode `texts`
        _ = get_settings().embedding_model_name
        raise NotImplementedError("TODO: local embedding encode")


class HostedEmbeddingProvider(EmbeddingProvider):
    def embed(self, texts: list[str]) -> list[list[float]]:
        # TODO: call the configured hosted embedding API
        raise NotImplementedError("TODO: hosted embedding encode")


def get_embedding_provider() -> EmbeddingProvider:
    settings = get_settings()
    if settings.embedding_provider == "hosted":
        return HostedEmbeddingProvider()
    return LocalSentenceTransformerProvider()
