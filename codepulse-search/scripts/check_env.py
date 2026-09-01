#!/usr/bin/env python3
"""Print Phase 0 environment status (DB, JWT key, Ollama). Run from codepulse-search/."""

from __future__ import annotations

import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))

from sqlalchemy import text

from app.config import get_settings
from app.db.session import engine


def main() -> None:
    settings = get_settings()
    print("=== codepulse-search Phase 0 check ===")
    print(f"DATABASE_URL     : {settings.database_url}")
    print(f"JWT key path     : {settings.resolved_jwt_public_key_path()}")
    print(f"JWT key exists   : {settings.resolved_jwt_public_key_path().is_file()}")
    print(f"LLM provider     : {settings.llm_provider}")
    print(f"LLM model        : {settings.llm_model_name}")
    print(f"LLM base URL     : {settings.llm_api_base_url}")
    print(f"Embedding model  : {settings.embedding_model_name}")

    try:
        with engine.connect() as conn:
            conn.execute(text("SELECT 1"))
            print("Database         : OK")
            row = conn.execute(
                text("SELECT extversion FROM pg_extension WHERE extname = 'vector'")
            ).first()
            print(f"pgvector         : {'OK v' + row[0] if row else 'MISSING (run enable_pgvector.sql as superuser)'}")
    except Exception as exc:  # noqa: BLE001
        print(f"Database         : FAIL ({exc})")

    try:
        import httpx

        r = httpx.get("http://127.0.0.1:11434/api/tags", timeout=2.0)
        if r.status_code == 200:
            names = [m.get("name") for m in r.json().get("models", [])]
            print(f"Ollama           : OK models={names or '(none pulled yet)'}")
        else:
            print(f"Ollama           : HTTP {r.status_code}")
    except Exception:
        print("Ollama           : not reachable (install from https://ollama.com — optional in Phase 0)")

    print("=== done ===")


if __name__ == "__main__":
    main()
