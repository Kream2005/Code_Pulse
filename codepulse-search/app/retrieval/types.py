"""Shared types for retrieval pipelines."""

from __future__ import annotations

from dataclasses import dataclass


@dataclass(frozen=True)
class ChunkHit:
    chunk_id: int
    source_type: str
    source_id: int
    chunk_index: int
    content: str
    score: float
    rank: int

    @property
    def source_key(self) -> tuple[str, int]:
        return (self.source_type, self.source_id)

    @property
    def chunk_key(self) -> tuple[str, int, int]:
        return (self.source_type, self.source_id, self.chunk_index)

    def as_dict(self) -> dict[str, object]:
        return {
            "chunk_id": self.chunk_id,
            "source_type": self.source_type,
            "source_id": self.source_id,
            "chunk_index": self.chunk_index,
            "content": self.content,
            "score": self.score,
            "rank": self.rank,
        }
