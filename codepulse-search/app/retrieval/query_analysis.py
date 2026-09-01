"""Light query analysis to steer hybrid retrieval."""

from __future__ import annotations

import re
from dataclasses import dataclass

from app.core.constants import SourceType

_TAG_RE = re.compile(
    r"\b(?:tag\s*[:=]?\s*)?([A-Za-z][A-Za-z0-9_+.#-]{1,32})\b",
    re.I,
)

_SOURCE_HINTS: list[tuple[SourceType, tuple[str, ...]]] = [
    (
        SourceType.FEEDBACK,
        (
            "feedback",
            "feedbacks",
            "commentaire",
            "commentaires",
            "retour",
            "retours",
            "avis",
            "note",
            "notes",
        ),
    ),
    (
        SourceType.CHALLENGE,
        (
            "challenge",
            "challenges",
            "exercice",
            "exercices",
            "coding",
            "énoncé",
            "enonce",
        ),
    ),
    (
        SourceType.QUESTION,
        (
            "question",
            "questions",
            "formulaire",
            "questionnaire",
            "libellé",
            "libelle",
        ),
    ),
    (
        SourceType.DOCUMENT,
        (
            "capgemini",
            "entreprise",
            "company",
            "société",
            "societe",
            "document",
            "knowledge",
            "connaissance",
        ),
    ),
]

_KNOWN_TAGS = {
    "java",
    "python",
    "sql",
    "javascript",
    "typescript",
    "spring",
    "react",
    "angular",
    "nodejs",
    "c++",
    "csharp",
    "c#",
    "go",
    "rust",
    "php",
    "kotlin",
    "swift",
    "arrays",
    "trees",
    "graphs",
    "stacks",
    "heaps",
    "dp",
    "design",
    "intervals",
}


@dataclass(frozen=True)
class QueryAnalysis:
    cleaned: str
    preferred_types: tuple[SourceType, ...]
    inferred_tag: str | None
    is_short: bool


def analyze_query(query: str) -> QueryAnalysis:
    cleaned = " ".join((query or "").split())
    lower = cleaned.lower()
    preferred: list[SourceType] = []
    for source_type, keywords in _SOURCE_HINTS:
        if any(re.search(rf"\b{re.escape(word)}\b", lower) for word in keywords):
            preferred.append(source_type)

    inferred_tag: str | None = None
    tag_match = re.search(
        r"\btag\s*[:=]?\s*['\"]?([A-Za-z0-9_+.#-]+)",
        cleaned,
        re.I,
    )
    if tag_match:
        inferred_tag = tag_match.group(1)
    else:
        for token in _TAG_RE.findall(cleaned):
            if token.lower() in _KNOWN_TAGS:
                inferred_tag = token
                break

    return QueryAnalysis(
        cleaned=cleaned,
        preferred_types=tuple(preferred),
        inferred_tag=inferred_tag,
        is_short=len(cleaned) < 24,
    )
