from __future__ import annotations

import httpx

from app.config import get_settings


def ollama_reachable(timeout: float = 1.5) -> bool:
    settings = get_settings()
    base = settings.llm_api_base_url.rstrip("/")
    # Ollama OpenAI-compatible root does not expose /models the same way;
    # hit the native tags endpoint when using the default Ollama host.
    tags_url = "http://127.0.0.1:11434/api/tags"
    try:
        with httpx.Client(timeout=timeout) as client:
            response = client.get(tags_url)
            if response.status_code == 200:
                return True
            # Fallback: OpenAI-compatible models list
            response = client.get(f"{base}/models")
            return response.status_code == 200
    except httpx.HTTPError:
        return False


class LlmClient:
    """Chat client for OpenAI-compatible APIs (default: local Ollama)."""

    def __init__(self) -> None:
        self.settings = get_settings()

    def generate(
        self,
        prompt: str,
        *,
        temperature: float = 0.2,
        max_tokens: int = 512,
        system: str | None = None,
    ) -> str:
        messages: list[dict[str, str]] = []
        if system:
            messages.append({"role": "system", "content": system})
        messages.append({"role": "user", "content": prompt})

        url = f"{self.settings.llm_api_base_url.rstrip('/')}/chat/completions"
        payload = {
            "model": self.settings.llm_model_name,
            "messages": messages,
            "temperature": temperature,
            "max_tokens": max_tokens,
            "stream": False,
        }
        headers = {
            "Authorization": f"Bearer {self.settings.llm_api_key}",
            "Content-Type": "application/json",
        }
        with httpx.Client(timeout=120.0) as client:
            response = client.post(url, json=payload, headers=headers)
            response.raise_for_status()
            data = response.json()

        choices = data.get("choices") or []
        if not choices:
            raise RuntimeError("Réponse LLM vide")
        message = choices[0].get("message") or {}
        content = message.get("content")
        if not content:
            raise RuntimeError("Réponse LLM sans contenu")
        return str(content).strip()
