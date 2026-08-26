from __future__ import annotations

from app.retrieval.types import ChunkHit


def reciprocal_rank_fusion(
    ranked_lists: list[list[ChunkHit]],
    *,
    k: int = 60,
    weights: list[float] | None = None,
) -> list[ChunkHit]:
    """Weighted Reciprocal Rank Fusion across one or more ranked lists."""
    if not ranked_lists:
        return []
    if weights is None:
        weights = [1.0] * len(ranked_lists)
    if len(weights) != len(ranked_lists):
        raise ValueError("weights length must match ranked_lists")

    fused_scores: dict[tuple[str, int, int], float] = {}
    best_hit: dict[tuple[str, int, int], ChunkHit] = {}

    for weight, results in zip(weights, ranked_lists, strict=True):
        for hit in results:
            key = hit.chunk_key
            fused_scores[key] = fused_scores.get(key, 0.0) + weight * (
                1.0 / (k + hit.rank)
            )
            current = best_hit.get(key)
            if current is None or hit.score > current.score:
                best_hit[key] = hit

    ordered_keys = sorted(
        fused_scores.keys(), key=lambda key: fused_scores[key], reverse=True
    )
    fused: list[ChunkHit] = []
    for rank, key in enumerate(ordered_keys, start=1):
        base = best_hit[key]
        fused.append(
            ChunkHit(
                chunk_id=base.chunk_id,
                source_type=base.source_type,
                source_id=base.source_id,
                chunk_index=base.chunk_index,
                content=base.content,
                score=fused_scores[key],
                rank=rank,
            )
        )
    return fused


def collapse_to_sources(hits: list[ChunkHit], top_k: int) -> list[ChunkHit]:
    """Keep the best-scoring chunk per (source_type, source_id)."""
    best_by_source: dict[tuple[str, int], ChunkHit] = {}
    for hit in hits:
        key = hit.source_key
        current = best_by_source.get(key)
        if current is None or hit.score > current.score:
            best_by_source[key] = hit

    collapsed = sorted(best_by_source.values(), key=lambda item: item.score, reverse=True)
    trimmed = collapsed[:top_k]
    return [
        ChunkHit(
            chunk_id=hit.chunk_id,
            source_type=hit.source_type,
            source_id=hit.source_id,
            chunk_index=hit.chunk_index,
            content=hit.content,
            score=hit.score,
            rank=index,
        )
        for index, hit in enumerate(trimmed, start=1)
    ]


def apply_source_type_boost(
    hits: list[ChunkHit],
    preferred_types: tuple[str, ...],
    boost: float = 1.35,
) -> list[ChunkHit]:
    if not preferred_types:
        return hits
    preferred = set(preferred_types)
    boosted: list[ChunkHit] = []
    for hit in hits:
        score = hit.score * boost if hit.source_type in preferred else hit.score
        boosted.append(
            ChunkHit(
                chunk_id=hit.chunk_id,
                source_type=hit.source_type,
                source_id=hit.source_id,
                chunk_index=hit.chunk_index,
                content=hit.content,
                score=score,
                rank=hit.rank,
            )
        )
    boosted.sort(key=lambda item: item.score, reverse=True)
    return [
        ChunkHit(
            chunk_id=hit.chunk_id,
            source_type=hit.source_type,
            source_id=hit.source_id,
            chunk_index=hit.chunk_index,
            content=hit.content,
            score=hit.score,
            rank=index,
        )
        for index, hit in enumerate(boosted, start=1)
    ]
