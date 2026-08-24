from typing import Any


def rerank(query: str, candidates: list[dict[str, Any]]) -> list[dict[str, Any]]:
    """Optional cross-encoder pass over fused candidates."""
    # TODO: score (query, candidate) with a cross-encoder and reorder
    _ = query
    return candidates
