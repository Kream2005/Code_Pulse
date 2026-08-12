#!/usr/bin/env python3
"""Manually run extract → chunk → embed → upsert."""

from __future__ import annotations

import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))

from app.ingestion.pipeline import run_ingestion


def main() -> None:
    n = run_ingestion()
    print(f"ingestion finished, upserted={n}")


if __name__ == "__main__":
    main()
