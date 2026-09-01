"""Role-based source scoping and SQL filter helpers."""

from __future__ import annotations

from fastapi import HTTPException, status

from app.core.constants import Role, SourceType
from app.core.security import CurrentUser

ROLE_SOURCE_TYPES: dict[Role, frozenset[SourceType]] = {
    Role.ADMIN_CODEPULSE: frozenset(SourceType),
    Role.ADMIN_CODING_CHALLENGE: frozenset(
        {SourceType.CHALLENGE, SourceType.QUESTION, SourceType.DOCUMENT}
    ),
    Role.MANAGER_RH: frozenset(
        {SourceType.FEEDBACK, SourceType.QUESTION, SourceType.DOCUMENT}
    ),
}


def allowed_source_types(user: CurrentUser) -> frozenset[SourceType]:
    merged: set[SourceType] = set()
    for role in user.roles:
        merged |= ROLE_SOURCE_TYPES.get(role, frozenset())
    if not merged:
        return frozenset(SourceType)
    return frozenset(merged)


def resolve_source_types(
    user: CurrentUser,
    requested: str | None,
) -> list[str]:
    """Return source_type values the user may search, optionally narrowed by filter."""
    allowed = {source_type.value for source_type in allowed_source_types(user)}
    if requested is None:
        return sorted(allowed)
    if requested not in allowed:
        raise HTTPException(
            status_code=status.HTTP_403_FORBIDDEN,
            detail=f"Rôle insuffisant pour rechercher le type « {requested} ».",
        )
    return [requested]
