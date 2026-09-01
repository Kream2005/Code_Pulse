"""Route a natural-language KPI question to a SQL resolver.

Primary path: deterministic keyword routing (works without Ollama).
Optional path: tiny local LLM when Ollama is reachable.
"""

from __future__ import annotations

import json
import re
from dataclasses import dataclass
from typing import Any

from sqlalchemy.orm import Session

from app.generation.llm_client import LlmClient, ollama_reachable
from app.kpi_tools.resolvers import run_resolver
from app.kpi_tools.tool_schemas import KPI_TOOL_NAMES, KPI_TOOLS


@dataclass(frozen=True)
class KpiRoute:
    tool: str
    arguments: dict[str, Any]
    explanation: str
    router: str


_TAG_PATTERNS = (
    re.compile(r"\btag\s*[:=]?\s*['\"]?([A-Za-z0-9_\-+.#]+)", re.I),
    re.compile(r"\b(?:pour|sur|about|for)\s+(?:le\s+)?tag\s+['\"]?([A-Za-z0-9_\-+.#]+)", re.I),
    re.compile(r"\b(?:java|python|sql|javascript|typescript|spring|react)\b", re.I),
)


def _extract_tag(question: str) -> str | None:
    for pattern in _TAG_PATTERNS[:2]:
        match = pattern.search(question)
        if match:
            return match.group(1)
    match = _TAG_PATTERNS[2].search(question)
    if match:
        return match.group(0)
    return None


def _extract_statut(question: str) -> str | None:
    lower = question.lower()
    if "non_soumis" in lower or "non soumis" in lower or "draft" in lower:
        return "NON_SOUMIS"
    if "soumis" in lower or "submitted" in lower:
        return "SOUMIS"
    return None


def route_with_rules(question: str) -> KpiRoute | None:
    """Pick a tool from simple FR/EN keywords. Prefer precision over guessing."""
    q = question.lower()
    tag = _extract_tag(question)
    statut = _extract_statut(question)

    if any(word in q for word in ("participation", "taux de participation", "coverage")):
        return KpiRoute(
            tool="get_participation_rate",
            arguments={},
            explanation="Mot-clé participation → taux de challenges avec feedback soumis.",
            router="rules",
        )

    if any(
        word in q
        for word in (
            "moyenne",
            "average",
            "note moyenne",
            "score moyen",
            "avg",
            "note globale",
        )
    ):
        args: dict[str, Any] = {}
        if tag:
            args["tag"] = tag
        return KpiRoute(
            tool="get_average_score",
            arguments=args,
            explanation="Mot-clé moyenne/score → moyenne des notes globales (SQL).",
            router="rules",
        )

    if any(word in q for word in ("question", "questions", "formulaire")):
        return KpiRoute(
            tool="count_questions",
            arguments={},
            explanation="Mot-clé question → comptage des questions de feedback.",
            router="rules",
        )

    if any(word in q for word in ("feedback", "feedbacks", "retour", "retours")):
        args = {}
        if statut:
            args["statut"] = statut
        return KpiRoute(
            tool="count_feedbacks",
            arguments=args,
            explanation="Mot-clé feedback → comptage des feedbacks.",
            router="rules",
        )

    if any(word in q for word in ("challenge", "challenges", "exercice", "exercices")):
        args = {}
        if tag:
            args["tag"] = tag
        return KpiRoute(
            tool="count_challenges",
            arguments=args,
            explanation="Mot-clé challenge → comptage des coding challenges.",
            router="rules",
        )

    return None


def route_with_llm(question: str, llm: LlmClient | None = None) -> KpiRoute | None:
    client = llm or LlmClient()
    tool_list = json.dumps(KPI_TOOLS, ensure_ascii=False, indent=2)
    prompt = (
        "Tu es un routeur de métriques CodePulse. "
        "Choisis UN outil dans la liste et réponds UNIQUEMENT en JSON "
        'avec les clés "tool" et "arguments".\n\n'
        f"Outils:\n{tool_list}\n\n"
        f"Question:\n{question}\n"
    )
    try:
        raw = client.generate(prompt, temperature=0.0, max_tokens=200)
    except Exception:
        return None

    try:
        start = raw.find("{")
        end = raw.rfind("}")
        if start < 0 or end < 0:
            return None
        payload = json.loads(raw[start : end + 1])
    except json.JSONDecodeError:
        return None

    tool = payload.get("tool")
    arguments = payload.get("arguments") or {}
    if tool not in KPI_TOOL_NAMES or not isinstance(arguments, dict):
        return None
    return KpiRoute(
        tool=str(tool),
        arguments=arguments,
        explanation="Outil choisi via le modèle local (Ollama).",
        router="llm",
    )


def answer_kpi(db: Session, question: str) -> dict[str, Any]:
    route = route_with_rules(question)
    if route is None and ollama_reachable():
        route = route_with_llm(question)

    if route is None:
        return {
            "question": question,
            "tool": None,
            "value": None,
            "explanation": (
                "Impossible d'associer la question à un outil KPI connu. "
                "Essayez: moyenne des notes, nombre de challenges, "
                "nombre de feedbacks, taux de participation."
            ),
        }

    try:
        value = run_resolver(route.tool, db, **route.arguments)
    except TypeError:
        value = run_resolver(route.tool, db)

    return {
        "question": question,
        "tool": route.tool,
        "value": value,
        "explanation": route.explanation,
        "router": route.router,
    }
