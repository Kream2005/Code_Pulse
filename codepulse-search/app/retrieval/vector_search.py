from typing import Any

from sqlalchemy.orm import Session

from app.core.constants import Role


def vector_search(
    query_embedding: list[float],
    top_k: int,
    role_filter: Role | None,
    db: Session | None = None,
) -> list[dict[str, Any]]:
    """Nearest-neighbour search over `search_chunk.embedding` (pgvector)."""
    # TODO: SELECT ... ORDER BY embedding <=> :query LIMIT :top_k, scoped by role_filter
    _ = (query_embedding, top_k, role_filter, db)
    return []
