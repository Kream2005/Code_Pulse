#!/usr/bin/env python3
"""One-off: enable pgvector and create the chunks table this service owns.

Requires: PostgreSQL with the pgvector package installed.
Creating the extension needs a DB superuser once (see scripts/enable_pgvector.sql).
"""

from __future__ import annotations

import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))

from sqlalchemy import text
from sqlalchemy.exc import DBAPIError

from app.core.constants import (
    EMBEDDING_DIM,
    KNOWLEDGE_DOCUMENTS_TABLE,
    SEARCH_CHUNKS_TABLE,
    SEARCH_INDEX_STATE_TABLE,
)
from app.db.session import engine


def main() -> None:
    # AUTOCOMMIT so a failed CREATE EXTENSION does not poison the connection pool.
    with engine.connect().execution_options(isolation_level="AUTOCOMMIT") as conn:
        try:
            conn.execute(text("CREATE EXTENSION IF NOT EXISTS vector"))
            print("OK: extension `vector` enabled")
        except DBAPIError as exc:
            print(
                "WARN: could not CREATE EXTENSION vector (need superuser once).\n"
                "  Linux:  sudo -u postgres psql -d codepulse -f scripts/enable_pgvector.sql\n"
                "  Windows (as postgres admin): psql -U postgres -d codepulse -f scripts\\enable_pgvector.sql\n"
                f"  Detail: {exc.orig}"
            )

    with engine.connect().execution_options(isolation_level="AUTOCOMMIT") as conn:
        try:
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
            conn.execute(
                text(
                    f"""
                    CREATE UNIQUE INDEX IF NOT EXISTS uq_{SEARCH_CHUNKS_TABLE}_source
                    ON {SEARCH_CHUNKS_TABLE} (source_type, source_id, chunk_index)
                    """
                )
            )
            conn.execute(
                text(
                    f"""
                    CREATE INDEX IF NOT EXISTS idx_{SEARCH_CHUNKS_TABLE}_content_fts
                    ON {SEARCH_CHUNKS_TABLE}
                    USING gin (to_tsvector('simple', content))
                    """
                )
            )
            try:
                conn.execute(
                    text(
                        f"""
                        CREATE INDEX IF NOT EXISTS idx_{SEARCH_CHUNKS_TABLE}_embedding_hnsw
                        ON {SEARCH_CHUNKS_TABLE}
                        USING hnsw (embedding vector_cosine_ops)
                        """
                    )
                )
                print(f"OK: HNSW index on `{SEARCH_CHUNKS_TABLE}.embedding`")
            except DBAPIError as exc:
                print(f"WARN: HNSW index skipped ({exc.orig})")

            conn.execute(
                text(
                    f"""
                    CREATE TABLE IF NOT EXISTS {KNOWLEDGE_DOCUMENTS_TABLE} (
                        id SERIAL PRIMARY KEY,
                        title VARCHAR(255) NOT NULL,
                        body TEXT NOT NULL,
                        category VARCHAR(64) NOT NULL DEFAULT 'company',
                        tags VARCHAR(255),
                        active BOOLEAN NOT NULL DEFAULT TRUE,
                        content_hash VARCHAR(64) NOT NULL,
                        created_at TIMESTAMPTZ DEFAULT NOW(),
                        updated_at TIMESTAMPTZ DEFAULT NOW()
                    )
                    """
                )
            )
            conn.execute(
                text(
                    f"""
                    CREATE TABLE IF NOT EXISTS {SEARCH_INDEX_STATE_TABLE} (
                        source_type VARCHAR(64) NOT NULL,
                        source_id INTEGER NOT NULL,
                        content_hash VARCHAR(64) NOT NULL,
                        chunk_count INTEGER NOT NULL DEFAULT 0,
                        indexed_at TIMESTAMPTZ DEFAULT NOW(),
                        PRIMARY KEY (source_type, source_id)
                    )
                    """
                )
            )
            print(f"OK: table `{SEARCH_CHUNKS_TABLE}` ensured")
            print(f"OK: table `{KNOWLEDGE_DOCUMENTS_TABLE}` ensured")
            print(f"OK: table `{SEARCH_INDEX_STATE_TABLE}` ensured")
        except DBAPIError as exc:
            print(
                f"FAIL: could not create search tables.\n"
                "  Enable the vector extension first, then re-run this script.\n"
                f"  Detail: {exc.orig}"
            )
            sys.exit(1)


if __name__ == "__main__":
    main()
