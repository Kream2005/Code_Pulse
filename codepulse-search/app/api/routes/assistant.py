from fastapi import APIRouter, Depends
from sqlalchemy.orm import Session

from app.api.deps import get_db_session, require_role
from app.core.constants import ADMIN_ROLES
from app.core.security import CurrentUser
from app.generation.rag_pipeline import run_rag
from app.schemas.assistant import AssistantRequest, AssistantResponse

router = APIRouter(prefix="/assistant", tags=["assistant"])


@router.post("", response_model=AssistantResponse)
def ask_assistant(
    body: AssistantRequest,
    user: CurrentUser = Depends(require_role(*ADMIN_ROLES)),
    db: Session = Depends(get_db_session),
) -> AssistantResponse:
    """RAG assistant: hybrid retrieval + optional local LLM answer with citations."""
    return run_rag(db=db, question=body.question, user=user)
