SYSTEM_RAG = (
    "You are the CodePulse assistant. Answer only from the provided context. "
    "If the context is insufficient, say so. Cite sources by type and id."
)


def build_rag_prompt(question: str, context: str) -> str:
    """Instruction + context + question structure for the RAG assistant."""
    return (
        f"{SYSTEM_RAG}\n\n"
        f"Context:\n{context}\n\n"
        f"Question:\n{question}\n"
    )
