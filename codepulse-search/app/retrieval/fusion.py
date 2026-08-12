from typing import Any


def reciprocal_rank_fusion(
    vector_results: list[dict[str, Any]],
    keyword_results: list[dict[str, Any]],
    k: int = 60,
) -> list[dict[str, Any]]:
    """Merge ranked lists with Reciprocal Rank Fusion."""
    # TODO: score = sum 1/(k + rank) per source, sort descending
    _ = (vector_results, keyword_results, k)
    return []
