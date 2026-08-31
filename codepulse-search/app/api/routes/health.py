from fastapi import APIRouter
from sqlalchemy import text

from app.config import get_settings
from app.db.session import engine

router = APIRouter(tags=["health"])


@router.get("/health")
def health() -> dict[str, str]:
    """Public liveness probe — no JWT required (Phase 0 smoke test)."""
    return {"status": "ok", "service": "codepulse-search"}


@router.get("/health/ready")
def ready() -> dict[str, object]:
    """Readiness: Postgres reachable, JWT key file present, optional Ollama."""
    settings = get_settings()
    checks: dict[str, object] = {
        "service": "codepulse-search",
        "database": False,
        "pgvector": False,
        "jwt_public_key": False,
        "jwt_public_key_path": str(settings.resolved_jwt_public_key_path()),
        "llm_provider": settings.llm_provider,
        "llm_model": settings.llm_model_name,
        "ollama_reachable": None,
    }

    try:
        with engine.connect() as conn:
            conn.execute(text("SELECT 1"))
            checks["database"] = True
            row = conn.execute(
                text("SELECT 1 FROM pg_extension WHERE extname = 'vector'")
            ).first()
            checks["pgvector"] = row is not None
    except Exception as exc:  # noqa: BLE001 — surface for ops/demo
        checks["database_error"] = str(exc)

    key_path = settings.resolved_jwt_public_key_path()
    checks["jwt_public_key"] = key_path.is_file()

    if settings.llm_provider.lower() == "ollama":
        try:
            import httpx

            response = httpx.get("http://127.0.0.1:11434/api/tags", timeout=2.0)
            checks["ollama_reachable"] = response.status_code == 200
        except Exception:  # noqa: BLE001
            checks["ollama_reachable"] = False

    ready_ok = bool(checks["database"] and checks["jwt_public_key"])
    checks["status"] = "ready" if ready_ok else "degraded"
    return checks
