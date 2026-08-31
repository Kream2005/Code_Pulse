from pydantic import BaseModel, Field

from app.core.constants import DEFAULT_TOP_K


class SearchFilters(BaseModel):
    tag: str | None = None
    source_type: str | None = None


class SearchRequest(BaseModel):
    query: str = Field(min_length=1)
    filters: SearchFilters = Field(default_factory=SearchFilters)
    top_k: int = Field(default=DEFAULT_TOP_K, ge=1, le=50)


class SearchHit(BaseModel):
    source_type: str
    source_id: int
    title: str | None = None
    snippet: str
    score: float


class SearchResponse(BaseModel):
    query: str
    results: list[SearchHit] = Field(default_factory=list)
