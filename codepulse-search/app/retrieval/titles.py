from __future__ import annotations

from collections import defaultdict

from sqlalchemy import select
from sqlalchemy.orm import Session

from app.core.constants import SourceType
from app.db.models import CodingChallenge, Feedback, KnowledgeDocument, QuestionFeedback


def load_titles(
    db: Session,
    sources: list[tuple[str, int]],
) -> dict[tuple[str, int], str]:
    """Batch-load display titles for search hits."""
    by_type: dict[str, set[int]] = defaultdict(set)
    for source_type, source_id in sources:
        by_type[source_type].add(source_id)

    titles: dict[tuple[str, int], str] = {}

    challenge_ids = by_type.get(SourceType.CHALLENGE.value, set())
    if challenge_ids:
        rows = db.scalars(
            select(CodingChallenge).where(CodingChallenge.id.in_(challenge_ids))
        ).all()
        for row in rows:
            title = (row.titre or "").strip() or f"Challenge #{row.id}"
            titles[(SourceType.CHALLENGE.value, row.id)] = title

    feedback_ids = by_type.get(SourceType.FEEDBACK.value, set())
    if feedback_ids:
        rows = db.scalars(select(Feedback).where(Feedback.id.in_(feedback_ids))).all()
        for row in rows:
            title = (row.challenge_titre or "").strip() or f"Feedback #{row.id}"
            titles[(SourceType.FEEDBACK.value, row.id)] = title

    question_ids = by_type.get(SourceType.QUESTION.value, set())
    if question_ids:
        rows = db.scalars(
            select(QuestionFeedback).where(QuestionFeedback.id.in_(question_ids))
        ).all()
        for row in rows:
            title = (row.libelle or "").strip() or f"Question #{row.id}"
            titles[(SourceType.QUESTION.value, row.id)] = title[:120]

    document_ids = by_type.get(SourceType.DOCUMENT.value, set())
    if document_ids:
        rows = db.scalars(
            select(KnowledgeDocument).where(KnowledgeDocument.id.in_(document_ids))
        ).all()
        for row in rows:
            titles[(SourceType.DOCUMENT.value, row.id)] = row.title

    return titles
