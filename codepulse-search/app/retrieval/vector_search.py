from __future__ import annotations

from sqlalchemy import text
from sqlalchemy.orm import Session

from app.core.constants import SEARCH_CHUNKS_TABLE
from app.retrieval.types import ChunkHit

# Drop weak semantic neighbours (cosine similarity = 1 - distance).
MIN_VECTOR_SCORE = 0.18


def _vector_literal(values: list[float]) -> str:
    return "[" + ",".join(f"{value:.8f}" for value in values) + "]"


def vector_search(
    query_embedding: list[float],
    top_k: int,
    source_types: list[str],
    db: Session,
    tag: str | None = None,
    *,
    min_score: float = MIN_VECTOR_SCORE,
) -> list[ChunkHit]:
    """Nearest-neighbour search over `search_chunk.embedding` (pgvector)."""
    if not source_types:
        return []

    candidate_limit = max(top_k * 8, 40)
    params: dict[str, object] = {
        "query_vec": _vector_literal(query_embedding),
        "limit": candidate_limit,
        "source_types": source_types,
        "tag_pattern": f"%{tag.strip()}%" if tag else None,
        "min_score": min_score,
    }

    rows = db.execute(
        text(
            f"""
            WITH ranked AS (
                SELECT
                    sc.id,
                    sc.source_type,
                    sc.source_id,
                    sc.chunk_index,
                    sc.content,
                    1 - (sc.embedding <=> CAST(:query_vec AS vector)) AS score
                FROM {SEARCH_CHUNKS_TABLE} sc
                WHERE sc.embedding IS NOT NULL
                  AND sc.source_type = ANY(:source_types)
                  AND (
                        :tag_pattern IS NULL
                        OR (
                            sc.source_type = 'CHALLENGE'
                            AND EXISTS (
                                SELECT 1
                                FROM coding_challenge c
                                WHERE c.id = sc.source_id
                                  AND c.supprime = FALSE
                                  AND c.tag ILIKE :tag_pattern
                            )
                        )
                        OR (
                            sc.source_type = 'FEEDBACK'
                            AND EXISTS (
                                SELECT 1
                                FROM feedback f
                                WHERE f.id = sc.source_id
                                  AND f.supprime = FALSE
                                  AND f.challenge_tag ILIKE :tag_pattern
                            )
                        )
                        OR (
                            sc.source_type = 'QUESTION'
                            AND :tag_pattern IS NOT NULL
                            AND FALSE
                        )
                      )
            )
            SELECT *
            FROM ranked
            WHERE score >= :min_score
            ORDER BY score DESC
            LIMIT :limit
            """
        ),
        params,
    ).mappings()

    hits: list[ChunkHit] = []
    for rank, row in enumerate(rows, start=1):
        hits.append(
            ChunkHit(
                chunk_id=int(row["id"]),
                source_type=str(row["source_type"]),
                source_id=int(row["source_id"]),
                chunk_index=int(row["chunk_index"]),
                content=str(row["content"]),
                score=float(row["score"]),
                rank=rank,
            )
        )
    return hits
