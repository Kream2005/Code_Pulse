#!/usr/bin/env python3
"""Import Markdown/text files from knowledge/ into knowledge_document + sync index.

Usage (from codepulse-search/):
  .venv/bin/python scripts/import_knowledge.py
  .venv/bin/python scripts/import_knowledge.py knowledge/company
"""

from __future__ import annotations

import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))

from app.db.session import SessionLocal
from app.ingestion.knowledge import create_document, list_documents

ROOT = Path(__file__).resolve().parents[1]
DEFAULT_DIR = ROOT / "knowledge" / "company"


def _title_from_file(path: Path, body: str) -> str:
    for line in body.splitlines():
        stripped = line.strip()
        if stripped.startswith("#"):
            return stripped.lstrip("#").strip()[:255]
    return path.stem.replace("_", " ").replace("-", " ").strip()[:255]


def main() -> None:
    target = Path(sys.argv[1]) if len(sys.argv) > 1 else DEFAULT_DIR
    if not target.is_absolute():
        target = ROOT / target
    if not target.exists():
        print(f"FAIL: folder not found: {target}")
        sys.exit(1)

    files = sorted(
        [*target.glob("*.md"), *target.glob("*.txt")],
        key=lambda p: p.name.lower(),
    )
    if not files:
        print(f"No .md/.txt files in {target}")
        sys.exit(0)

    db = SessionLocal()
    try:
        existing_titles = {row.title.lower() for row in list_documents(db, active_only=False)}
        imported = 0
        for path in files:
            body = path.read_text(encoding="utf-8").strip()
            if not body:
                continue
            title = _title_from_file(path, body)
            if title.lower() in existing_titles:
                print(f"SKIP (already exists): {title}")
                continue
            create_document(
                db,
                title=title,
                body=body,
                category="company",
                tags="capgemini,company",
                sync=False,
            )
            existing_titles.add(title.lower())
            imported += 1
            print(f"OK: {title}")

        if imported:
            from app.ingestion.pipeline import run_ingestion

            result = run_ingestion(db)
            print(result.as_dict())
        else:
            print("Nothing new to import.")
    finally:
        db.close()


if __name__ == "__main__":
    main()
