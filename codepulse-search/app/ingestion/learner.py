"""Background learner: periodically syncs new/changed sources into search_chunk."""

from __future__ import annotations

import logging
import threading
import time

from app.config import get_settings
from app.ingestion.pipeline import run_ingestion

logger = logging.getLogger(__name__)

_stop = threading.Event()
_thread: threading.Thread | None = None
_last_result: dict[str, object] | None = None
_last_error: str | None = None
_last_run_at: float | None = None


def learner_status() -> dict[str, object]:
    settings = get_settings()
    return {
        "enabled": settings.auto_ingest_enabled,
        "interval_seconds": settings.auto_ingest_interval_seconds,
        "running": _thread is not None and _thread.is_alive(),
        "last_run_at": _last_run_at,
        "last_error": _last_error,
        "last_result": _last_result,
    }


def _loop() -> None:
    global _last_result, _last_error, _last_run_at
    settings = get_settings()
    interval = max(30, int(settings.auto_ingest_interval_seconds))
    logger.info("Continuous learner started (every %ss)", interval)
    while not _stop.is_set():
        try:
            result = run_ingestion(full=False)
            _last_result = result.as_dict()
            _last_error = None
            _last_run_at = time.time()
            logger.info("Learner sync: %s", result.as_dict().get("message"))
        except Exception as exc:  # noqa: BLE001
            _last_error = str(exc)
            logger.exception("Learner sync failed: %s", exc)
        _stop.wait(interval)


def start_learner() -> None:
    global _thread
    settings = get_settings()
    if not settings.auto_ingest_enabled:
        logger.info("Continuous learner disabled (AUTO_INGEST_ENABLED=false)")
        return
    if _thread and _thread.is_alive():
        return
    _stop.clear()
    _thread = threading.Thread(target=_loop, name="codepulse-learner", daemon=True)
    _thread.start()


def stop_learner() -> None:
    _stop.set()
