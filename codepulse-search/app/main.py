from contextlib import asynccontextmanager

from fastapi import FastAPI

from app.api.routes import assistant, health, kpi, search
from app.logging_config import setup_logging


@asynccontextmanager
async def lifespan(_app: FastAPI):
    setup_logging()
    yield


app = FastAPI(
    title="codepulse-search",
    description="Admin semantic search, conversational KPIs, and RAG assistant for CodePulse.",
    lifespan=lifespan,
)

app.include_router(health.router)
app.include_router(search.router)
app.include_router(kpi.router)
app.include_router(assistant.router)
