"""Periodic ingestion trigger.

No container orchestrator required. Prefer OS cron/Task Scheduler, e.g.

    */15 * * * *  /path/to/codepulse-search/.venv/bin/python \\
                  /path/to/codepulse-search/scripts/reindex.py

Or run this module as a long-lived process on a dedicated host.
"""

from __future__ import annotations

import time

from app.ingestion.pipeline import run_ingestion


def run_forever(interval_seconds: int = 900) -> None:
    while True:
        result = run_ingestion()
        print(result.as_dict())
        time.sleep(interval_seconds)


if __name__ == "__main__":
    run_forever()
