"""Periodic ingestion trigger.

No container/orchestrator: wire this with a crontab, e.g.

    */15 * * * *  /opt/codepulse/codepulse-search/.venv/bin/python /opt/codepulse/codepulse-search/scripts/reindex.py

or a simple loop:

    while True:
        run_ingestion()
        time.sleep(interval_seconds)
"""

import time

from app.ingestion.pipeline import run_ingestion


def run_forever(interval_seconds: int = 900) -> None:
    # TODO: choose cron vs in-process loop for the target host
    while True:
        run_ingestion()
        time.sleep(interval_seconds)
