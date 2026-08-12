import httpx

from app.config import get_settings


class LlmClient:
    """Thin httpx wrapper around the configured chat-completions API."""

    def generate(self, prompt: str, **kwargs: object) -> str:
        # TODO: POST {LLM_API_BASE_URL}/chat/completions with LLM_API_KEY / LLM_MODEL_NAME
        _ = (prompt, kwargs, get_settings(), httpx)
        raise NotImplementedError("TODO: LLM generate")
