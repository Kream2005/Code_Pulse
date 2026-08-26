from __future__ import annotations

from abc import ABC, abstractmethod
from functools import lru_cache

from app.config import get_settings
from app.core.constants import EMBED_BATCH_SIZE


class EmbeddingProvider(ABC):
    @abstractmethod
    def embed(self, texts: list[str]) -> list[list[float]]:
        """Return one embedding vector per input text."""


@lru_cache(maxsize=1)
def _load_sentence_transformer():
    """Load once per process — first run downloads ~90 MB (cached on disk)."""
    from sentence_transformers import SentenceTransformer

    settings = get_settings()
    return SentenceTransformer(settings.embedding_model_name)


class LocalSentenceTransformerProvider(EmbeddingProvider):
    """Local MiniLM via sentence-transformers (open source, CPU-friendly)."""

    def embed(self, texts: list[str]) -> list[list[float]]:
        if not texts:
            return []
        model = _load_sentence_transformer()
        vectors = model.encode(
            texts,
            batch_size=min(len(texts), EMBED_BATCH_SIZE),
            normalize_embeddings=True,
            show_progress_bar=len(texts) > 64,
        )
        return [row.tolist() for row in vectors]


class HostedEmbeddingProvider(EmbeddingProvider):
    def embed(self, texts: list[str]) -> list[list[float]]:
        raise NotImplementedError("Hosted embeddings are not used in this project (open source only).")


def get_embedding_provider() -> EmbeddingProvider:
    settings = get_settings()
    if settings.embedding_provider == "hosted":
        return HostedEmbeddingProvider()
    return LocalSentenceTransformerProvider()
