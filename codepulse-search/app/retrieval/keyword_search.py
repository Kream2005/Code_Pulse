from typing import Any

from sqlalchemy.orm import Session

from app.core.constants import Role


def keyword_search(
    query: str,
    top_k: int,
    role_filter: Role | None,
    db: Session | None = None,
) -> list[dict[str, Any]]:
    """PostgreSQL full-text search over chunk / challenge / feedback text."""
    # TODO: tsvector @@ plainto_tsquery, scoped by role_filter
    _ = (query, top_k, role_filter, db)
    return []
