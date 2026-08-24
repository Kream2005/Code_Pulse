from fastapi import APIRouter, Depends
from sqlalchemy.orm import Session

from app.api.deps import get_db_session, require_role
from app.core.constants import ADMIN_ROLES
from app.core.security import CurrentUser
from app.schemas.search import SearchRequest, SearchResponse

router = APIRouter(prefix="/search", tags=["search"])


@router.post("", response_model=SearchResponse)
def search(
    body: SearchRequest,
    _user: CurrentUser = Depends(require_role(*ADMIN_ROLES)),
    _db: Session = Depends(get_db_session),
) -> SearchResponse:
    """Hybrid retrieval (vector + keyword + fusion). Not implemented yet."""
    # TODO: embed query, vector_search + keyword_search, fuse, rerank, apply role_filter
    return SearchResponse(query=body.query, results=[])
