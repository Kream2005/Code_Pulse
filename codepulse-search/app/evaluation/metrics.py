def precision_at_k(relevant: set[str], retrieved: list[str], k: int) -> float:
    """Precision@k over a labelled test set."""
    # TODO: |relevant ∩ retrieved[:k]| / k
    _ = (relevant, retrieved, k)
    return 0.0


def recall_at_k(relevant: set[str], retrieved: list[str], k: int) -> float:
    """Recall@k over a labelled test set."""
    # TODO: |relevant ∩ retrieved[:k]| / |relevant|
    _ = (relevant, retrieved, k)
    return 0.0


def mrr(relevant: set[str], retrieved: list[str]) -> float:
    """Mean Reciprocal Rank for a single query (extend to a list of queries later)."""
    # TODO: 1/rank of first relevant hit, else 0
    _ = (relevant, retrieved)
    return 0.0
