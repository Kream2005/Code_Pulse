#!/usr/bin/env python3
"""Manually run extract → chunk → embed → upsert."""

from __future__ import annotations

import json
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))

from app.ingestion.pipeline import run_ingestion


def main() -> None:
    print("==> Phase 1 reindex (chunk + embed + upsert)")
    print("    Requires: pgvector enabled + pip install -r requirements-ml.txt")
    try:
        result = run_ingestion()
    except Exception as exc:  # noqa: BLE001 — CLI should print a helpful message
        msg = str(exc)
        if "search_chunk" in msg and ("does not exist" in msg or "UndefinedTable" in msg):
            print(
                "FAIL: table search_chunk missing.\n"
                "  1) Enable pgvector (once, as postgres superuser):\n"
                "     psql -d codepulse -f scripts/enable_pgvector.sql\n"
                "  2) Create tables:\n"
                "     python scripts/init_db.py"
            )
        else:
            print(f"FAIL: {exc}")
        sys.exit(1)

    print(json.dumps(result.as_dict(), indent=2, ensure_ascii=False))


if __name__ == "__main__":
    main()
