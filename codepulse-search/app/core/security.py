"""RS256 JWT validation matching the Spring Boot resource server."""

from __future__ import annotations

from dataclasses import dataclass
from pathlib import Path

from jose import JWTError, jwt

from app.config import Settings, get_settings
from app.core.constants import JWT_ALG, JWT_ROLES_CLAIM, JWT_UID_CLAIM, Role


@dataclass(frozen=True)
class CurrentUser:
    uid: int | None
    email: str
    roles: tuple[Role, ...]

    @property
    def role(self) -> Role | None:
        return self.roles[0] if self.roles else None


def load_public_key(path: Path) -> str:
    return path.read_text(encoding="utf-8")


def decode_token(token: str, settings: Settings | None = None) -> CurrentUser:
    """Validate an RS256 access token issued by CodePulse Spring Boot.

    Expected claims: `iss`, `sub` (email), `roles` (list of Role names), `uid`.
    """
    settings = settings or get_settings()
    public_key = load_public_key(settings.resolved_jwt_public_key_path())
    try:
        payload = jwt.decode(
            token,
            public_key,
            algorithms=[JWT_ALG],
            issuer=settings.jwt_issuer,
            options={"verify_aud": False},
        )
    except JWTError as exc:
        raise ValueError(f"Invalid token: {exc}") from exc

    raw_roles = payload.get(JWT_ROLES_CLAIM) or []
    if isinstance(raw_roles, str):
        raw_roles = [raw_roles]
    roles = tuple(Role(r) for r in raw_roles if r in Role._value2member_map_)
    uid = payload.get(JWT_UID_CLAIM)
    return CurrentUser(
        uid=int(uid) if uid is not None else None,
        email=str(payload.get("sub") or ""),
        roles=roles,
    )
