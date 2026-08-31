from pydantic import BaseModel, Field


class AssistantRequest(BaseModel):
    question: str = Field(min_length=1)


class Citation(BaseModel):
    source_type: str
    source_id: int
    snippet: str
    score: float | None = None


class AssistantResponse(BaseModel):
    answer: str
    citations: list[Citation] = Field(default_factory=list)
