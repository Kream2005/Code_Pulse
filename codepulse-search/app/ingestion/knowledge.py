"""CRUD helpers for manual knowledge documents."""

from __future__ import annotations

from datetime import UTC, datetime

from sqlalchemy import select
from sqlalchemy.orm import Session

from app.db.models import KnowledgeDocument
from app.ingestion.documents import content_hash
from app.ingestion.pipeline import run_ingestion


def list_documents(db: Session, *, active_only: bool = True) -> list[KnowledgeDocument]:
    stmt = select(KnowledgeDocument).order_by(KnowledgeDocument.id.desc())
    if active_only:
        stmt = stmt.where(KnowledgeDocument.active.is_(True))
    return list(db.scalars(stmt).all())


def create_document(
    db: Session,
    *,
    title: str,
    body: str,
    category: str = "company",
    tags: str | None = None,
    sync: bool = True,
) -> KnowledgeDocument:
    now = datetime.now(UTC)
    row = KnowledgeDocument(
        title=title.strip(),
        body=body.strip(),
        category=(category or "company").strip().lower(),
        tags=(tags or "").strip() or None,
        active=True,
        content_hash=content_hash(body.strip()),
        created_at=now,
        updated_at=now,
    )
    db.add(row)
    db.commit()
    db.refresh(row)
    if sync:
        run_ingestion(db)
    return row


def update_document(
    db: Session,
    doc_id: int,
    *,
    title: str | None = None,
    body: str | None = None,
    category: str | None = None,
    tags: str | None = None,
    active: bool | None = None,
    sync: bool = True,
) -> KnowledgeDocument | None:
    row = db.get(KnowledgeDocument, doc_id)
    if row is None:
        return None
    if title is not None:
        row.title = title.strip()
    if body is not None:
        row.body = body.strip()
        row.content_hash = content_hash(row.body)
    if category is not None:
        row.category = category.strip().lower()
    if tags is not None:
        row.tags = tags.strip() or None
    if active is not None:
        row.active = active
    row.updated_at = datetime.now(UTC)
    db.commit()
    db.refresh(row)
    if sync:
        run_ingestion(db)
    return row


def soft_delete_document(db: Session, doc_id: int, *, sync: bool = True) -> bool:
    row = db.get(KnowledgeDocument, doc_id)
    if row is None:
        return False
    row.active = False
    row.updated_at = datetime.now(UTC)
    db.commit()
    if sync:
        run_ingestion(db)
    return True
