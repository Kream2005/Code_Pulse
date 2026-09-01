"""Read-mostly maps of existing CodePulse tables + the chunks table this service owns."""

from datetime import datetime

from pgvector.sqlalchemy import Vector
from sqlalchemy import Boolean, DateTime, Float, Integer, String, Text
from sqlalchemy.orm import Mapped, mapped_column

from app.core.constants import (
    EMBEDDING_DIM,
    KNOWLEDGE_DOCUMENTS_TABLE,
    SEARCH_CHUNKS_TABLE,
    SEARCH_INDEX_STATE_TABLE,
)
from app.db.session import Base


class CodingChallenge(Base):
    """Existing `coding_challenge` table (owned by Spring)."""

    __tablename__ = "coding_challenge"

    id: Mapped[int] = mapped_column(Integer, primary_key=True)
    titre: Mapped[str | None] = mapped_column(String)
    description: Mapped[str | None] = mapped_column(Text)
    tag: Mapped[str | None] = mapped_column(String)
    duree: Mapped[int | None] = mapped_column(Integer)
    supprime: Mapped[bool] = mapped_column(Boolean, default=False)


class Feedback(Base):
    """Existing `feedback` table (owned by Spring)."""

    __tablename__ = "feedback"

    id: Mapped[int] = mapped_column(Integer, primary_key=True)
    note_globale: Mapped[float | None] = mapped_column(Float)
    commentaire: Mapped[str | None] = mapped_column(Text)
    statut_feedback: Mapped[str | None] = mapped_column(String)
    challenge_titre: Mapped[str | None] = mapped_column(String)
    challenge_tag: Mapped[str | None] = mapped_column(String)
    challenge_description: Mapped[str | None] = mapped_column(Text)
    supprime: Mapped[bool] = mapped_column(Boolean, default=False)
    utilisateur_id: Mapped[int | None] = mapped_column(Integer)
    coding_challenge_id: Mapped[int | None] = mapped_column(Integer)


class QuestionFeedback(Base):
    """Existing `question_feedback` table (owned by Spring)."""

    __tablename__ = "question_feedback"

    id: Mapped[int] = mapped_column(Integer, primary_key=True)
    libelle: Mapped[str | None] = mapped_column(String)
    type: Mapped[str | None] = mapped_column(String)
    obligatoire: Mapped[bool] = mapped_column(Boolean, default=False)
    choix: Mapped[str | None] = mapped_column(Text)
    supprime: Mapped[bool] = mapped_column(Boolean, default=False)


class KnowledgeDocument(Base):
    """Manual knowledge base entries owned by the search service (company docs, etc.)."""

    __tablename__ = KNOWLEDGE_DOCUMENTS_TABLE

    id: Mapped[int] = mapped_column(Integer, primary_key=True, autoincrement=True)
    title: Mapped[str] = mapped_column(String(255))
    body: Mapped[str] = mapped_column(Text)
    category: Mapped[str] = mapped_column(String(64), default="company")
    tags: Mapped[str | None] = mapped_column(String(255))
    active: Mapped[bool] = mapped_column(Boolean, default=True)
    content_hash: Mapped[str] = mapped_column(String(64))
    created_at: Mapped[datetime | None] = mapped_column(DateTime(timezone=True))
    updated_at: Mapped[datetime | None] = mapped_column(DateTime(timezone=True))


class SearchIndexState(Base):
    """Tracks which sources are indexed and their content hash (incremental sync)."""

    __tablename__ = SEARCH_INDEX_STATE_TABLE

    source_type: Mapped[str] = mapped_column(String(64), primary_key=True)
    source_id: Mapped[int] = mapped_column(Integer, primary_key=True)
    content_hash: Mapped[str] = mapped_column(String(64))
    chunk_count: Mapped[int] = mapped_column(Integer, default=0)
    indexed_at: Mapped[datetime | None] = mapped_column(DateTime(timezone=True))


class SearchChunk(Base):
    """Embeddings / chunks table owned by this service."""

    __tablename__ = SEARCH_CHUNKS_TABLE

    id: Mapped[int] = mapped_column(Integer, primary_key=True, autoincrement=True)
    source_type: Mapped[str] = mapped_column(String(64))
    source_id: Mapped[int] = mapped_column(Integer)
    chunk_index: Mapped[int] = mapped_column(Integer, default=0)
    content: Mapped[str] = mapped_column(Text)
    embedding: Mapped[list[float] | None] = mapped_column(
        Vector(EMBEDDING_DIM),
        nullable=True,
    )
    created_at: Mapped[datetime | None] = mapped_column(DateTime(timezone=True))
