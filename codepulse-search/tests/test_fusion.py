"""Tests for RRF fusion and query analysis."""

from app.retrieval.fusion import (
    apply_source_type_boost,
    collapse_to_sources,
    reciprocal_rank_fusion,
)
from app.retrieval.query_analysis import analyze_query
from app.retrieval.types import ChunkHit


def _hit(
    chunk_id: int,
    source_type: str,
    source_id: int,
    chunk_index: int,
    score: float,
    rank: int,
) -> ChunkHit:
    return ChunkHit(
        chunk_id=chunk_id,
        source_type=source_type,
        source_id=source_id,
        chunk_index=chunk_index,
        content=f"content-{chunk_id}",
        score=score,
        rank=rank,
    )


def test_rrf_prefers_items_in_both_lists() -> None:
    vector = [_hit(1, "CHALLENGE", 10, 0, 0.9, 1), _hit(2, "CHALLENGE", 11, 0, 0.8, 2)]
    keyword = [_hit(1, "CHALLENGE", 10, 0, 0.7, 1), _hit(3, "FEEDBACK", 5, 0, 0.6, 2)]
    fused = reciprocal_rank_fusion([vector, keyword])
    assert fused[0].chunk_id == 1


def test_collapse_keeps_best_chunk_per_source() -> None:
    hits = [
        _hit(1, "CHALLENGE", 10, 0, 0.5, 1),
        _hit(2, "CHALLENGE", 10, 1, 0.9, 2),
        _hit(3, "FEEDBACK", 5, 0, 0.8, 3),
    ]
    collapsed = collapse_to_sources(hits, top_k=2)
    assert len(collapsed) == 2
    challenge = next(item for item in collapsed if item.source_id == 10)
    assert challenge.chunk_index == 1


def test_source_type_boost() -> None:
    hits = [
        _hit(1, "QUESTION", 1, 0, 0.5, 1),
        _hit(2, "FEEDBACK", 2, 0, 0.45, 2),
    ]
    boosted = apply_source_type_boost(hits, ("FEEDBACK",), boost=2.0)
    assert boosted[0].source_type == "FEEDBACK"


def test_analyze_query_feedback_and_tag() -> None:
    analysis = analyze_query("feedback Java difficulté")
    assert any(item.value == "FEEDBACK" for item in analysis.preferred_types)
    assert analysis.inferred_tag and analysis.inferred_tag.lower() == "java"
