from contextlib import asynccontextmanager

from fastapi import FastAPI

from app.api.routes import assistant, health, kpi, knowledge, search
from app.ingestion.learner import start_learner, stop_learner
from app.logging_config import setup_logging


@asynccontextmanager
async def lifespan(_app: FastAPI):
    setup_logging()
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
