from fastapi import APIRouter, Depends
from sqlalchemy.orm import Session

from app.api.deps import get_db_session, require_role
from app.core.constants import ADMIN_ROLES
from app.core.security import CurrentUser
from app.retrieval.hybrid_search import hybrid_search
from app.schemas.search import SearchRequest, SearchResponse

router = APIRouter(prefix="/search", tags=["search"])


@router.post("", response_model=SearchResponse)
def search(
    body: SearchRequest,
    user: CurrentUser = Depends(require_role(*ADMIN_ROLES)),
    db: Session = Depends(get_db_session),
) -> SearchResponse:
    """Hybrid retrieval: semantic (vectors) + keyword (full-text) fused with RRF."""
    results = hybrid_search(
        db=db,
        query=body.query,
        top_k=body.top_k,
        user=user,
        source_type_filter=body.filters.source_type,
        tag_filter=body.filters.tag,
    )
    return SearchResponse(query=body.query, results=results)
