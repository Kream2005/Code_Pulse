"""Helpers for assistant UX: readable snippets + when not to dump retrieval."""

from __future__ import annotations

import re

_STOPWORDS = frozenset(
    {
        "les",
        "des",
        "une",
        "un",
        "le",
        "la",
        "de",
        "du",
        "et",
        "ou",
        "que",
        "qui",
        "quel",
        "quels",
        "quelle",
        "quelles",
        "pour",
        "dans",
        "sur",
        "avec",
        "par",
        "pas",
        "plus",
        "très",
        "tres",
        "dit",
        "the",
        "a",
        "an",
        "of",
        "to",
        "in",
        "is",
        "are",
        "was",
        "were",
        "be",
        "what",
        "how",
        "can",
        "you",
        "me",
        "my",
        "your",
        "this",
        "that",
        "mon",
        "ma",
        "mes",
        "ce",
        "cet",
        "cette",
        "ces",
        "est",
        "sont",
        "il",
        "elle",
        "nous",
        "vous",
        "je",
        "tu",
        "on",
        "do",
        "does",
        "did",
        "please",
        "svp",
    }
)

_NON_RETRIEVAL_RE = re.compile(
    r"""^\s*(
        bonjour|bonsoir|salut|hello|hi|hey|coucou|yo|
        merci(\s+beaucoup)?|thanks|thank\s+you|
        ok|okay|cool|super|parfait|d['']accord|
        test|testing|ping|pong|
        (ça|ca)\s*va\??|how\s+are\s+you|
        who\s+are\s+you|qui\s+(es|êtes|etes)[- ]tu|qui\s+êtes[- ]vous|
        (aide[- ]moi|help(\s+me)?|au\s+secours)|
        (what\s+can\s+you\s+do|que\s+(peux|pouvez)[- ]tu\s+faire|
         à\s+quoi\s+sers[- ]tu|a\s+quoi\s+sers[- ]tu)|
        (comment\s+(ça|ca)\s+marche|how\s+does\s+(this|it)\s+work)|
        (présente[- ]toi|presente[- ]toi|introduce\s+yourself)
    )\b[\s\W]*$""",
    re.I | re.X,
)

_DOMAIN_TERMS = frozenset(
    {
        "feedback",
        "feedbacks",
        "commentaire",
        "commentaires",
        "challenge",
        "challenges",
        "exercice",
        "exercices",
        "question",
        "questions",
        "formulaire",
        "capgemini",
        "entreprise",
        "note",
        "notes",
        "difficile",
        "difficiles",
        "difficulte",
        "difficulté",
        "moyenne",
        "candidat",
        "candidats",
        "coding",
        "java",
        "python",
        "arrays",
        "trees",
        "graphs",
        "stacks",
        "heaps",
        "dp",
        "design",
        "intervals",
        "points",
        "avis",
    }
)

_GENERALIST_ANSWER = (
    "Je suis l’assistant CodePulse : je m’appuie sur les données indexées "
    "(feedbacks, challenges, questions du formulaire, fiches entreprise). "
    "Posez une question concrète, par exemple : "
    "« Quels points reviennent dans les commentaires ? », "
    "« Challenges liés aux tableaux (arrays) », "
    "ou « Que dit-on de Capgemini ? »."
)

def clean_snippet(text: str, max_len: int = 320) -> str:
    """Drop index metadata; keep note / commentaire / description / body text."""
    if not text:
        return ""

    flat = " ".join(text.split())
    parts: list[str] = []

    note = re.search(r"\bNote globale:\s*([0-9]+(?:\.[0-9]+)?)", flat, re.I)
    if note:
        parts.append(f"Note {note.group(1)}")

    comment = re.search(r"\bCommentaire:\s*(.+?)(?=\s*$)", flat, re.I)
    if comment:
        parts.append(comment.group(1).strip())

    desc = re.search(
        r"\bDescription du challenge:\s*(.+?)(?=\s+Commentaire:|\s*$)",
        flat,
        re.I,
    )
    if desc:
        parts.append(desc.group(1).strip())

    libelle = re.search(r"\bLibellé:\s*(.+?)(?=\s+Keywords:|\s+Obligatoire:|\s*$)", flat, re.I)
    if libelle:
        parts.append(libelle.group(1).strip())

    # Knowledge / challenge body: text after the boilerplate keywords line.
    if not parts:
        body = re.sub(
            r"^.*?\bKeywords:\s*[^.]*?(?=\s{2,}|\s+Durée|\s*$)",
            "",
            flat,
            count=1,
            flags=re.I,
        )
        body = re.sub(
            r"^\s*(?:CHALLENGE|FEEDBACK|QUESTION|DOCUMENT)\s*\|[^.]*(?:tag:\S+|type:\S+|category:\S+)\s*",
            "",
            body,
            flags=re.I,
        )
        body = re.sub(
            r"\b(?:Tag|Category|Durée \(minutes\)|Obligatoire):\s*\S+",
            "",
            body,
            flags=re.I,
        )
        parts.append(body.strip(" -|"))

    cleaned = " — ".join(p for p in parts if p)
    cleaned = re.sub(r"\s+", " ", cleaned).strip(" -|")
    if not cleaned:
        cleaned = flat
    if len(cleaned) <= max_len:
        return cleaned
    return cleaned[: max_len - 1].rstrip() + "…"


def content_terms(text: str) -> set[str]:
    tokens = re.findall(r"[A-Za-zÀ-ÿ0-9_+#.-]+", text or "")
    return {t.lower() for t in tokens if len(t) > 2 and t.lower() not in _STOPWORDS}


def is_non_retrieval_query(question: str) -> bool:
    """True for chitchat / meta prompts that should not dump search hits."""
    q = (question or "").strip()
    if not q:
        return True
    if _NON_RETRIEVAL_RE.match(q):
        return True
    terms = content_terms(q)
    if not terms:
        return True
    if len(terms) == 1 and len(q) < 24:
        only = next(iter(terms))
        if only not in _DOMAIN_TERMS:
            return True
    return False


def _term_in_blob(term: str, blob: str) -> bool:
    variants = {term, term.rstrip("s"), f"{term}s"}
    return any(len(v) > 2 and v in blob for v in variants)


def hits_look_relevant(question: str, texts: list[str]) -> bool:
    """Keep real topic questions; drop accidental matches like hello→'hello there'."""
    terms = content_terms(question)
    if not terms or not texts:
        return False

    blob = " ".join(texts).lower()
    overlap = sum(1 for t in terms if _term_in_blob(t, blob))
    domain = terms & _DOMAIN_TERMS

    if domain and overlap >= 1:
        return True
    if len(terms) == 1 and overlap >= 1:
        return True
    if overlap >= 2:
        return True
    # Multi-word domain question: trust hybrid retrieval even without exact lexical hit.
    if domain and len(terms) >= 2:
        return True
    return False


def generalist_answer() -> str:
    return _GENERALIST_ANSWER
