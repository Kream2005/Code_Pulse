SYSTEM_RAG = (
    "Tu es l'assistant CodePulse. Réponds uniquement à partir du contexte fourni. "
    "Si le contexte est insuffisant, dis-le clairement. "
    "Cite les sources sous la forme [TYPE#id] quand tu t'appuies dessus. "
    "N'invente jamais de chiffres ou de faits absents du contexte."
)


def build_rag_prompt(question: str, context: str) -> str:
    return (
        f"Contexte:\n{context}\n\n"
        f"Question:\n{question}\n\n"
        "Réponse (en français, concise, avec citations [TYPE#id] si pertinent):\n"
    )


def format_context(hits: list[dict[str, object]]) -> str:
    blocks: list[str] = []
    for index, hit in enumerate(hits, start=1):
        source_type = hit.get("source_type")
        source_id = hit.get("source_id")
        title = hit.get("title") or ""
        snippet = hit.get("snippet") or hit.get("content") or ""
        blocks.append(
            f"[{index}] [{source_type}#{source_id}] {title}\n{snippet}"
        )
    return "\n\n".join(blocks)
