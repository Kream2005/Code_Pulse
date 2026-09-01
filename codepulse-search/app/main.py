import logging
from contextlib import asynccontextmanager

from fastapi import FastAPI

from app.api.routes import assistant, health, kpi, knowledge, search
from app.config import get_settings
from app.generation.llm_client import ollama_reachable
from app.ingestion.learner import start_learner, stop_learner
from app.logging_config import setup_logging

logger = logging.getLogger(__name__)


@asynccontextmanager
async def lifespan(_app: FastAPI):
    setup_logging()
    settings = get_settings()
    if settings.llm_provider.lower() == "ollama":
        if ollama_reachable():
            logger.info(
                "Ollama ready — assistant/KPI will use %s at %s",
                settings.llm_model_name,
                settings.llm_api_base_url,
            )
        else:
            logger.warning(
                "Ollama not reachable at %s — assistant answers will use passage fallback only",
                settings.llm_api_base_url,
            )
    start_learner()
    try:
        yield
    finally:
        stop_learner()


app = FastAPI(
    title="codepulse-search",
    description="Admin semantic search, conversational KPIs, and RAG assistant for CodePulse.",
    lifespan=lifespan,
)

app.include_router(health.router)
app.include_router(search.router)
app.include_router(kpi.router)
app.include_router(assistant.router)
app.include_router(knowledge.router)
