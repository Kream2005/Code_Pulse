from app.core.constants import Role
from app.generation.llm_client import LlmClient
from app.generation.prompts import build_rag_prompt
from app.schemas.assistant import AssistantResponse


def run_rag(query: str, role: Role | None, llm: LlmClient | None = None) -> AssistantResponse:
    """Retrieve → prompt → LLM → answer with citations."""
    # TODO: retrieve with role filter, build_rag_prompt, llm.generate, attach citations
    _ = (query, role, llm, build_rag_prompt)
    return AssistantResponse(answer="", citations=[])
