#!/usr/bin/env python3
"""One-off: enable pgvector and create the chunks table this service owns."""

from __future__ import annotations

import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))

from sqlalchemy import text

from app.core.constants import EMBEDDING_DIM, SEARCH_CHUNKS_TABLE
from app.db.session import engine


def main() -> None:
    with engine.begin() as conn:
        conn.execute(text("CREATE EXTENSION IF NOT EXISTS vector"))
        conn.execute(
            text(
                f"""
                CREATE TABLE IF NOT EXISTS {SEARCH_CHUNKS_TABLE} (
                    id SERIAL PRIMARY KEY,
                    source_type VARCHAR(64) NOT NULL,
                    source_id INTEGER NOT NULL,
                    chunk_index INTEGER NOT NULL DEFAULT 0,
                    content TEXT NOT NULL,
                    embedding vector({EMBEDDING_DIM}),
                    created_at TIMESTAMPTZ DEFAULT NOW()
                )
                """
            )
        )
    print(f"pgvector ready; table `{SEARCH_CHUNKS_TABLE}` ensured")


if __name__ == "__main__":
    main()
