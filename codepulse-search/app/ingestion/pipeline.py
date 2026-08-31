from __future__ import annotations

from collections import defaultdict
from dataclasses import dataclass
from datetime import UTC, datetime

from sqlalchemy import delete, select, text
from sqlalchemy.orm import Session

from app.core.constants import EMBED_BATCH_SIZE, SourceType
from app.db.models import (
    CodingChallenge,
    Feedback,
    KnowledgeDocument,
    QuestionFeedback,
    SearchChunk,
    SearchIndexState,
)
from app.db.session import SessionLocal
from app.embeddings.chunking import chunk_text
from app.embeddings.provider import get_embedding_provider
from app.ingestion.documents import (
    IndexDocument,
    document_from_challenge,
    document_from_feedback,
    document_from_knowledge,
    document_from_question,
)
from app.ingestion.result import IngestionResult


@dataclass(frozen=True)
class _ChunkRecord:
    source_type: SourceType
    source_id: int
    chunk_index: int
    content: str
    content_hash: str


def _load_documents(db: Session) -> list[IndexDocument]:
    docs: list[IndexDocument] = []

    for row in db.scalars(
        select(CodingChallenge).where(CodingChallenge.supprime.is_(False))
    ).all():
        doc = document_from_challenge(row)
        if doc:
            docs.append(doc)

    for row in db.scalars(select(Feedback).where(Feedback.supprime.is_(False))).all():
        doc = document_from_feedback(row)
        if doc:
            docs.append(doc)

    for row in db.scalars(
        select(QuestionFeedback).where(QuestionFeedback.supprime.is_(False))
    ).all():
        doc = document_from_question(row)
        if doc:
            docs.append(doc)

    for row in db.scalars(
        select(KnowledgeDocument).where(KnowledgeDocument.active.is_(True))
    ).all():
        doc = document_from_knowledge(row)
        if doc:
            docs.append(doc)

    return docs


def _embed_all(texts: list[str]) -> list[list[float]]:
    provider = get_embedding_provider()
    vectors: list[list[float]] = []
    for start in range(0, len(texts), EMBED_BATCH_SIZE):
        batch = texts[start : start + EMBED_BATCH_SIZE]
        vectors.extend(provider.embed(batch))
    return vectors


def _replace_chunks_for_source(
    db: Session,
    source_type: SourceType,
    source_id: int,
    content_hash: str,
    chunks: list[str],
    embeddings: list[list[float]],
    result: IngestionResult,
) -> None:
    deleted = db.execute(
        delete(SearchChunk).where(
            SearchChunk.source_type == source_type.value,
            SearchChunk.source_id == source_id,
        )
    )
    result.chunks_deleted += deleted.rowcount or 0

    now = datetime.now(UTC)
    for index, (content, vector) in enumerate(zip(chunks, embeddings, strict=True)):
        db.add(
            SearchChunk(
                source_type=source_type.value,
                source_id=source_id,
                chunk_index=index,
                content=content,
                embedding=vector,
                created_at=now,
            )
        )
        result.chunks_written += 1

    state = db.get(SearchIndexState, (source_type.value, source_id))
    if state is None:
        state = SearchIndexState(
            source_type=source_type.value,
            source_id=source_id,
            content_hash=content_hash,
            chunk_count=len(chunks),
            indexed_at=now,
        )
        db.add(state)
    else:
        state.content_hash = content_hash
        state.chunk_count = len(chunks)
        state.indexed_at = now


def _remove_source(
    db: Session,
    source_type: str,
    source_id: int,
    result: IngestionResult,
) -> None:
    deleted = db.execute(
        delete(SearchChunk).where(
            SearchChunk.source_type == source_type,
            SearchChunk.source_id == source_id,
        )
    )
    result.chunks_deleted += deleted.rowcount or 0
    db.execute(
        delete(SearchIndexState).where(
            SearchIndexState.source_type == source_type,
            SearchIndexState.source_id == source_id,
        )
    )
    result.sources_removed += 1


def run_ingestion(
    db: Session | None = None,
    *,
    full: bool = False,
) -> IngestionResult:
    """Extract → chunk → embed → upsert.

    Incremental mode (default): only re-embed sources whose content hash changed.
    Full mode: rebuild every source (still hash-tracked for later syncs).
    """
    result = IngestionResult(mode="full" if full else "incremental")
    own_session = db is None
    session = db or SessionLocal()

    try:
        session.execute(text("SELECT 1 FROM search_chunk LIMIT 1"))

        documents = _load_documents(session)
        result.sources_scanned = len(documents)
        live_keys = {(doc.source_type.value, doc.source_id) for doc in documents}

        existing_states = {
            (row.source_type, row.source_id): row.content_hash
            for row in session.scalars(select(SearchIndexState)).all()
        }

        to_index: list[IndexDocument] = []
        for doc in documents:
            key = (doc.source_type.value, doc.source_id)
            if not full and existing_states.get(key) == doc.content_hash:
                result.sources_unchanged += 1
                continue
            pieces = chunk_text(doc.body)
            if not pieces:
                result.sources_skipped_empty += 1
                continue
            to_index.append(doc)

        for key in list(existing_states.keys()):
            if key not in live_keys:
                _remove_source(session, key[0], key[1], result)

        if not to_index:
            session.commit()
            return result

        records: list[_ChunkRecord] = []
        for doc in to_index:
            for index, content in enumerate(chunk_text(doc.body)):
                records.append(
                    _ChunkRecord(
                        doc.source_type,
                        doc.source_id,
                        index,
                        content,
                        doc.content_hash,
                    )
                )

        vectors = _embed_all([record.content for record in records])
        grouped: dict[tuple[SourceType, int], list[tuple[int, str, list[float], str]]] = (
            defaultdict(list)
        )
        for record, vector in zip(records, vectors, strict=True):
            grouped[(record.source_type, record.source_id)].append(
                (record.chunk_index, record.content, vector, record.content_hash)
            )

        for (source_type, source_id), items in grouped.items():
            items.sort(key=lambda row: row[0])
            chunks = [content for _, content, _, _ in items]
            embeddings = [vector for _, _, vector, _ in items]
            doc_hash = items[0][3]
            _replace_chunks_for_source(
                session,
                source_type,
                source_id,
                doc_hash,
                chunks,
                embeddings,
                result,
            )
            result.sources_indexed += 1
            result.merge_type(source_type.value)

        session.commit()
        return result
    except Exception:
        session.rollback()
        raise
    finally:
        if own_session:
            session.close()
