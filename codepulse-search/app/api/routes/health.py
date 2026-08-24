from fastapi import APIRouter, Depends

from app.api.deps import require_role
from app.core.constants import Role
from app.core.security import CurrentUser

router = APIRouter(tags=["health"])


@router.get("/health")
def health(
    _user: CurrentUser = Depends(
        require_role(
            Role.USER,
            Role.ADMIN_CODING_CHALLENGE,
            Role.MANAGER_RH,
            Role.ADMIN_CODEPULSE,
        )
    ),
) -> dict[str, str]:
    return {"status": "ok", "service": "codepulse-search"}
