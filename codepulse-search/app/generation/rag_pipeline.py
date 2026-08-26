from __future__ import annotations

from sqlalchemy.orm import Session

from app.core.security import CurrentUser
from app.generation.llm_client import LlmClient, ollama_reachable
from app.generation.prompts import SYSTEM_RAG, build_rag_prompt, format_context
from app.retrieval.hybrid_search import hybrid_search
from app.schemas.assistant import AssistantResponse, Citation


def run_rag(
    db: Session,
    question: str,
    user: CurrentUser,
    *,
    top_k: int = 8,
    llm: LlmClient | None = None,
) -> AssistantResponse:
    """Retrieve relevant chunks, then optionally generate an answer with citations."""
    hits = hybrid_search(
        db=db,
        query=question,
        top_k=top_k,
        user=user,
    )
    citations = [
        Citation(
            source_type=hit.source_type,
            source_id=hit.source_id,
            snippet=hit.snippet,
            score=hit.score,
        )
        for hit in hits
    ]

    if not hits:
        return AssistantResponse(
            answer=(
                "Aucun passage indexé ne correspond à cette question. "
                "Vérifiez que le reindex a bien été exécuté."
            ),
            citations=[],
        )

    context_payload = [
        {
            "source_type": hit.source_type,
            "source_id": hit.source_id,
            "title": hit.title,
            "snippet": hit.snippet,
        }
        for hit in hits
    ]
    context = format_context(context_payload)

    if not ollama_reachable():
        preview = "\n".join(
            f"- [{c.source_type}#{c.source_id}] {c.snippet[:160]}" for c in citations[:3]
        )
        return AssistantResponse(
            answer=(
                "Ollama n'est pas joignable. Voici les passages les plus proches "
                "(installez Ollama et tirez llama3.2:1b pour une réponse rédigée):\n"
                f"{preview}"
            ),
            citations=citations,
        )

    client = llm or LlmClient()
    prompt = build_rag_prompt(question, context)
    try:
        answer = client.generate(prompt, system=SYSTEM_RAG, temperature=0.2, max_tokens=512)
    except Exception as exc:  # noqa: BLE001 — surface a useful French message
        return AssistantResponse(
            answer=f"Échec de génération LLM: {exc}. Passages récupérés ci-dessous.",
            citations=citations,
        )

    return AssistantResponse(answer=answer, citations=citations)
