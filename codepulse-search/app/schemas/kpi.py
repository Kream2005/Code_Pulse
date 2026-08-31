from typing import Any

from pydantic import BaseModel, Field


class KpiRequest(BaseModel):
    question: str = Field(min_length=1)


class KpiResponse(BaseModel):
    question: str
    tool: str | None = None
    value: Any = None
    explanation: str | None = None
