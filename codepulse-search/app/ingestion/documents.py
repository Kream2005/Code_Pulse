"""Build plain-text documents from Spring-owned tables and knowledge docs."""

from __future__ import annotations

import hashlib
import json
from dataclasses import dataclass

from app.core.constants import SourceType
from app.db.models import CodingChallenge, Feedback, KnowledgeDocument, QuestionFeedback


@dataclass(frozen=True)
class IndexDocument:
    source_type: SourceType
    source_id: int
    title: str
    body: str
    content_hash: str = ""


def content_hash(text: str) -> str:
    return hashlib.sha256(text.encode("utf-8")).hexdigest()


def _clean(value: str | None) -> str:
    return (value or "").strip()


def _with_hash(doc: IndexDocument) -> IndexDocument:
    return IndexDocument(
        source_type=doc.source_type,
        source_id=doc.source_id,
        title=doc.title,
        body=doc.body,
        content_hash=content_hash(doc.body),
    )


def document_from_challenge(row: CodingChallenge) -> IndexDocument | None:
    titre = _clean(row.titre)
    description = _clean(row.description)
    if not titre and not description:
        return None
    tag = _clean(row.tag) or "general"
    header = f"CHALLENGE | {titre or f'Challenge #{row.id}'} | tag:{tag}"
    parts = [
        header,
        titre or f"Challenge #{row.id}",
        f"Tag: {tag}",
        f"Keywords: challenge coding {tag}",
    ]
    if row.duree is not None:
        parts.append(f"Durée (minutes): {row.duree}")
    if description:
        parts.append("")
        parts.append(description)
    return _with_hash(
        IndexDocument(
            source_type=SourceType.CHALLENGE,
            source_id=row.id,
            title=titre or f"Challenge #{row.id}",
            body="\n".join(parts),
        )
    )


def document_from_feedback(row: Feedback) -> IndexDocument | None:
    commentaire = _clean(row.commentaire)
    titre = _clean(row.challenge_titre)
    description = _clean(row.challenge_description)
    if not commentaire and not titre and not description:
        return None
    tag = _clean(row.challenge_tag) or "general"
    header = f"FEEDBACK | {titre or f'Feedback #{row.id}'} | tag:{tag}"
    parts = [
        header,
        f"Feedback candidat sur: {titre or '(challenge inconnu)'}",
        f"Tag challenge: {tag}",
        f"Keywords: feedback commentaire avis note {tag}",
    ]
    if row.note_globale is not None:
        parts.append(f"Note globale: {row.note_globale}")
    if row.statut_feedback:
        parts.append(f"Statut: {row.statut_feedback}")
    if description:
        parts.append("")
        parts.append("Description du challenge:")
        parts.append(description)
    if commentaire:
        parts.append("")
        parts.append("Commentaire:")
        parts.append(commentaire)
    return _with_hash(
        IndexDocument(
            source_type=SourceType.FEEDBACK,
            source_id=row.id,
            title=titre or f"Feedback #{row.id}",
            body="\n".join(parts),
        )
    )


def _format_choix(raw: str | None) -> str:
    if not raw or not raw.strip():
        return ""
    try:
        parsed = json.loads(raw)
        if isinstance(parsed, list):
            return ", ".join(str(x) for x in parsed)
    except json.JSONDecodeError:
        pass
    return raw.strip()


def document_from_question(row: QuestionFeedback) -> IndexDocument | None:
    libelle = _clean(row.libelle)
    if not libelle:
        return None
    qtype = row.type or "TEXTE"
    header = f"QUESTION | {libelle[:120]} | type:{qtype}"
    parts = [
        header,
        f"Question de feedback ({qtype})",
        f"Libellé: {libelle}",
        f"Keywords: question formulaire {qtype.lower()}",
        f"Obligatoire: {'oui' if row.obligatoire else 'non'}",
    ]
    options = _format_choix(row.choix)
    if options:
        parts.append(f"Options: {options}")
    return _with_hash(
        IndexDocument(
            source_type=SourceType.QUESTION,
            source_id=row.id,
            title=libelle[:120],
            body="\n".join(parts),
        )
    )


def document_from_knowledge(row: KnowledgeDocument) -> IndexDocument | None:
    title = _clean(row.title)
    body = _clean(row.body)
    if not title or not body:
        return None
    category = _clean(row.category) or "company"
    tags = _clean(row.tags)
    header = f"DOCUMENT | {title} | category:{category}"
    parts = [
        header,
        title,
        f"Category: {category}",
        f"Keywords: knowledge document {category} {tags}".strip(),
        "",
        body,
    ]
    return _with_hash(
        IndexDocument(
            source_type=SourceType.DOCUMENT,
            source_id=row.id,
            title=title,
            body="\n".join(parts),
        )
    )
