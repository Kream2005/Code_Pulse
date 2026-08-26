from __future__ import annotations

from sqlalchemy.orm import Session

from app.core.security import CurrentUser
from app.generation.llm_client import LlmClient, ollama_reachable
from app.generation.prompts import SYSTEM_RAG, build_rag_prompt, format_context
from app.generation.snippets import (
    clean_snippet,
    generalist_answer,
    hits_look_relevant,
    is_non_retrieval_query,
)
from app.retrieval.hybrid_search import hybrid_search
from app.schemas.assistant import AssistantResponse, Citation


def _citations_from_hits(hits) -> list[Citation]:
    return [
        Citation(
            source_type=hit.source_type,
            source_id=hit.source_id,
            snippet=clean_snippet(hit.snippet),
            score=hit.score,
        )
        for hit in hits
    ]


def _passage_fallback(citations: list[Citation]) -> str:
    lines = [
        f"• [{c.source_type}#{c.source_id}] {c.snippet[:140]}"
        for c in citations[:5]
        if c.snippet
    ]
    return (
        "Voici les passages les plus proches de votre question :\n" + "\n".join(lines)
        if lines
        else generalist_answer()
    )


def run_rag(
    db: Session,
    question: str,
    user: CurrentUser,
    *,
    top_k: int = 8,
    llm: LlmClient | None = None,
) -> AssistantResponse:
    """Retrieve relevant chunks, then optionally generate an answer with citations."""
    if is_non_retrieval_query(question):
        return AssistantResponse(answer=generalist_answer(), citations=[])

    hits = hybrid_search(
        db=db,
        query=question,
        top_k=top_k,
        user=user,
    )
    relevance_texts = [
        f"{hit.title or ''} {hit.snippet}" for hit in hits
    ]
    if not hits or not hits_look_relevant(question, relevance_texts):
        return AssistantResponse(answer=generalist_answer(), citations=[])

    citations = _citations_from_hits(hits)
    context_payload = [
        {
            "source_type": hit.source_type,
            "source_id": hit.source_id,
            "title": hit.title,
            "snippet": clean_snippet(hit.snippet, max_len=480),
        }
        for hit in hits
    ]
    context = format_context(context_payload)

    if not ollama_reachable():
        return AssistantResponse(answer=_passage_fallback(citations), citations=citations)

    client = llm or LlmClient()
    prompt = build_rag_prompt(question, context)
    try:
        answer = client.generate(prompt, system=SYSTEM_RAG, temperature=0.2, max_tokens=512)
    except Exception:  # noqa: BLE001 — keep UI free of stack traces
        return AssistantResponse(answer=_passage_fallback(citations), citations=citations)

    return AssistantResponse(answer=answer, citations=citations)
