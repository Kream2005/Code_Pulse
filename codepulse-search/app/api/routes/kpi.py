from fastapi import APIRouter, Depends
from sqlalchemy.orm import Session

from app.api.deps import get_db_session, require_role
from app.core.constants import ADMIN_ROLES
from app.core.security import CurrentUser
from app.kpi_tools.router import answer_kpi
from app.schemas.kpi import KpiRequest, KpiResponse

router = APIRouter(prefix="/kpi", tags=["kpi"])


@router.post("", response_model=KpiResponse)
def ask_kpi(
    body: KpiRequest,
    _user: CurrentUser = Depends(require_role(*ADMIN_ROLES)),
    db: Session = Depends(get_db_session),
) -> KpiResponse:
    """Natural-language KPI question. Values always come from SQL resolvers."""
    result = answer_kpi(db, body.question)
    return KpiResponse(
        question=result["question"],
        tool=result.get("tool"),
        value=result.get("value"),
        explanation=result.get("explanation"),
    )
