from collections.abc import Callable

from fastapi import Depends, HTTPException, status
from fastapi.security import HTTPAuthorizationCredentials, HTTPBearer

from app.core.constants import Role
from app.core.security import CurrentUser, decode_token
from app.db.session import get_session

bearer = HTTPBearer(auto_error=False)

get_db_session = get_session


def get_current_user(
    credentials: HTTPAuthorizationCredentials | None = Depends(bearer),
) -> CurrentUser:
    if credentials is None or not credentials.credentials:
        raise HTTPException(status.HTTP_401_UNAUTHORIZED, "Missing bearer token")
    try:
        return decode_token(credentials.credentials)
    except (ValueError, OSError) as exc:
        raise HTTPException(status.HTTP_401_UNAUTHORIZED, str(exc)) from exc


def require_role(*roles: Role) -> Callable[..., CurrentUser]:
    """Dependency factory: reject the request unless the JWT role is allowed.

    Apply this on every route so role filtering happens before retrieval.
    """
    allowed = set(roles)

    def _check(user: CurrentUser = Depends(get_current_user)) -> CurrentUser:
        if not user.roles or allowed.isdisjoint(user.roles):
            raise HTTPException(status.HTTP_403_FORBIDDEN, "Insufficient role")
        return user

    return _check
