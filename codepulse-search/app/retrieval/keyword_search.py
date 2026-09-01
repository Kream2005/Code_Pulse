from __future__ import annotations

from sqlalchemy import text
from sqlalchemy.orm import Session

from app.core.constants import SEARCH_CHUNKS_TABLE
from app.retrieval.types import ChunkHit


def keyword_search(
    query: str,
    top_k: int,
    source_types: list[str],
    db: Session,
    tag: str | None = None,
) -> list[ChunkHit]:
    """Full-text + lexical fallback over chunk content."""
    if not source_types:
        return []

    candidate_limit = max(top_k * 8, 40)
    params: dict[str, object] = {
        "query": query,
        "limit": candidate_limit,
        "source_types": source_types,
        "tag_pattern": f"%{tag.strip()}%" if tag else None,
        "like_pattern": f"%{query.strip()}%",
    }

    # Prefer websearch syntax when it parses; fall back to plainto.
    # ILIKE adds exact-substring recall for tags / rare tokens FTS may miss.
    rows = db.execute(
        text(
            f"""
            SELECT
                sc.id,
                sc.source_type,
                sc.source_id,
                sc.chunk_index,
                sc.content,
                (
                    COALESCE(
                        ts_rank_cd(
                            to_tsvector('simple', sc.content),
                            websearch_to_tsquery('simple', :query)
                        ),
                        0
                    )
                    + COALESCE(
                        ts_rank_cd(
                            to_tsvector('simple', sc.content),
                            plainto_tsquery('simple', :query)
                        ),
                        0
                    )
                    + CASE
                        WHEN sc.content ILIKE :like_pattern THEN 0.15
                        ELSE 0
                      END
                ) AS score
            FROM {SEARCH_CHUNKS_TABLE} sc
            WHERE sc.source_type = ANY(:source_types)
              AND (
                    to_tsvector('simple', sc.content)
                        @@ websearch_to_tsquery('simple', :query)
                    OR to_tsvector('simple', sc.content)
                        @@ plainto_tsquery('simple', :query)
                    OR sc.content ILIKE :like_pattern
                  )
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
            ORDER BY score DESC
            LIMIT :limit
            """
        ),
        params,
    ).mappings()

    hits: list[ChunkHit] = []
    for rank, row in enumerate(rows, start=1):
        score = float(row["score"] or 0.0)
        if score <= 0:
            continue
        hits.append(
            ChunkHit(
                chunk_id=int(row["id"]),
                source_type=str(row["source_type"]),
                source_id=int(row["source_id"]),
                chunk_index=int(row["chunk_index"]),
                content=str(row["content"]),
                score=score,
                rank=rank,
            )
        )
    return hits
