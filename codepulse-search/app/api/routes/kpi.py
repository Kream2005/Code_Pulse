from fastapi import APIRouter, Depends
from sqlalchemy.orm import Session

from app.api.deps import get_db_session, require_role
from app.core.constants import ADMIN_ROLES
from app.core.security import CurrentUser
from app.schemas.kpi import KpiRequest, KpiResponse

router = APIRouter(prefix="/kpi", tags=["kpi"])


@router.post("", response_model=KpiResponse)
def ask_kpi(
    body: KpiRequest,
    _user: CurrentUser = Depends(require_role(*ADMIN_ROLES)),
    _db: Session = Depends(get_db_session),
) -> KpiResponse:
    """Natural-language KPI question. Will later use LLM function calling + SQL resolvers."""
    # TODO: route to kpi_tools.resolvers via function calling — never invent numbers
    return KpiResponse(question=body.question, tool=None, value=None, explanation=None)
