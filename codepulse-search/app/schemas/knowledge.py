from pydantic import BaseModel, Field


class KnowledgeDocumentCreate(BaseModel):
    title: str = Field(min_length=1, max_length=255)
    body: str = Field(min_length=1)
    category: str = Field(default="company", max_length=64)
    tags: str | None = Field(default=None, max_length=255)


class KnowledgeDocumentUpdate(BaseModel):
    title: str | None = Field(default=None, min_length=1, max_length=255)
    body: str | None = Field(default=None, min_length=1)
    category: str | None = Field(default=None, max_length=64)
    tags: str | None = Field(default=None, max_length=255)
    active: bool | None = None


class KnowledgeDocumentOut(BaseModel):
    id: int
    title: str
    body: str
    category: str
    tags: str | None = None
    active: bool

    model_config = {"from_attributes": True}


class IngestionSyncRequest(BaseModel):
    full: bool = False
