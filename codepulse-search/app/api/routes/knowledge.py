from fastapi import APIRouter, Depends, HTTPException, status
from sqlalchemy.orm import Session

from app.api.deps import get_db_session, require_role
from app.core.constants import ADMIN_ROLES, Role
from app.core.security import CurrentUser
from app.ingestion import knowledge as knowledge_svc
from app.ingestion.learner import learner_status
from app.ingestion.pipeline import run_ingestion
from app.schemas.knowledge import (
    IngestionSyncRequest,
    KnowledgeDocumentCreate,
    KnowledgeDocumentOut,
    KnowledgeDocumentUpdate,
)

router = APIRouter(tags=["knowledge"])


@router.get("/knowledge/documents", response_model=list[KnowledgeDocumentOut])
def list_knowledge_documents(
    _user: CurrentUser = Depends(require_role(*ADMIN_ROLES)),
    db: Session = Depends(get_db_session),
) -> list[KnowledgeDocumentOut]:
    rows = knowledge_svc.list_documents(db, active_only=False)
    return [KnowledgeDocumentOut.model_validate(row) for row in rows]


@router.post(
    "/knowledge/documents",
    response_model=KnowledgeDocumentOut,
    status_code=status.HTTP_201_CREATED,
)
def create_knowledge_document(
    body: KnowledgeDocumentCreate,
    _user: CurrentUser = Depends(require_role(Role.ADMIN_CODEPULSE, Role.MANAGER_RH)),
    db: Session = Depends(get_db_session),
) -> KnowledgeDocumentOut:
    row = knowledge_svc.create_document(
        db,
        title=body.title,
        body=body.body,
        category=body.category,
        tags=body.tags,
        sync=True,
    )
    return KnowledgeDocumentOut.model_validate(row)


@router.put("/knowledge/documents/{doc_id}", response_model=KnowledgeDocumentOut)
def update_knowledge_document(
    doc_id: int,
    body: KnowledgeDocumentUpdate,
    _user: CurrentUser = Depends(require_role(Role.ADMIN_CODEPULSE, Role.MANAGER_RH)),
    db: Session = Depends(get_db_session),
) -> KnowledgeDocumentOut:
    row = knowledge_svc.update_document(
        db,
        doc_id,
        title=body.title,
        body=body.body,
        category=body.category,
        tags=body.tags,
        active=body.active,
        sync=True,
    )
    if row is None:
        raise HTTPException(status_code=404, detail="Document introuvable")
    return KnowledgeDocumentOut.model_validate(row)


@router.delete("/knowledge/documents/{doc_id}", status_code=status.HTTP_204_NO_CONTENT)
def delete_knowledge_document(
    doc_id: int,
    _user: CurrentUser = Depends(require_role(Role.ADMIN_CODEPULSE, Role.MANAGER_RH)),
    db: Session = Depends(get_db_session),
) -> None:
    ok = knowledge_svc.soft_delete_document(db, doc_id, sync=True)
    if not ok:
        raise HTTPException(status_code=404, detail="Document introuvable")


@router.post("/ingestion/sync")
def trigger_sync(
    body: IngestionSyncRequest | None = None,
    _user: CurrentUser = Depends(require_role(*ADMIN_ROLES)),
    db: Session = Depends(get_db_session),
) -> dict[str, object]:
    full = bool(body.full) if body else False
    result = run_ingestion(db, full=full)
    return result.as_dict()


@router.get("/ingestion/status")
def ingestion_status(
    _user: CurrentUser = Depends(require_role(*ADMIN_ROLES)),
) -> dict[str, object]:
    return learner_status()
