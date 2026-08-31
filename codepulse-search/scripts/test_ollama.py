#!/usr/bin/env python3
"""Smoke-test Ollama + optional RAG assistant (run from codepulse-search/)."""

from __future__ import annotations

import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))

from app.config import get_settings
from app.core.constants import Role
from app.core.security import CurrentUser
from app.db.session import SessionLocal
from app.generation.llm_client import LlmClient, ollama_reachable
from app.generation.rag_pipeline import run_rag


def main() -> None:
    settings = get_settings()
    print("=== Ollama smoke test ===")
    print(f"LLM model : {settings.llm_model_name}")
    print(f"LLM URL   : {settings.llm_api_base_url}")

    if not ollama_reachable():
        print("FAIL: Ollama not reachable on http://127.0.0.1:11434")
        print("Start Ollama and ensure the model is pulled: ollama pull llama3.2:1b")
        raise SystemExit(1)

    print("Ollama    : reachable")

    client = LlmClient()
    reply = client.generate(
        "Réponds en une phrase: quel est ton rôle dans CodePulse ?",
        system="Tu es l'assistant CodePulse.",
        temperature=0.1,
        max_tokens=80,
    )
    print(f"LLM reply : {reply}")

    question = "Quels sont les principaux services de Capgemini ?"
    user = CurrentUser(uid=1, email="admin@codepulse.local", roles=(Role.ADMIN_CODEPULSE,))
    with SessionLocal() as db:
        result = run_rag(db=db, question=question, user=user)

    print(f"\n=== RAG test ({question}) ===")
    print(f"Citations : {len(result.citations)}")
    print(f"Answer    : {result.answer[:500]}")
    if "Voici les passages les plus proches" in result.answer:
        print("\nWARN: got passage fallback — Ollama was not used for this answer.")
        raise SystemExit(2)
    print("\nOK: Ollama generated the assistant answer.")


if __name__ == "__main__":
    main()
