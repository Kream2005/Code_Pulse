from fastapi import APIRouter, Depends
from sqlalchemy.orm import Session

from app.api.deps import get_db_session, require_role
from app.core.constants import ADMIN_ROLES
from app.core.security import CurrentUser
from app.schemas.assistant import AssistantRequest, AssistantResponse

router = APIRouter(prefix="/assistant", tags=["assistant"])


@router.post("", response_model=AssistantResponse)
def ask_assistant(
    body: AssistantRequest,
    _user: CurrentUser = Depends(require_role(*ADMIN_ROLES)),
    _db: Session = Depends(get_db_session),
) -> AssistantResponse:
    """RAG / navigation Q&A. Will later call generation.rag_pipeline.run_rag."""
    # TODO: call run_rag(query, role) and return answer + citations
    return AssistantResponse(answer="", citations=[])
