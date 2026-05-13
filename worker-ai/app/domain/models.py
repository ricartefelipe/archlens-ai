from uuid import UUID, uuid4

from pydantic import BaseModel, Field


class DocumentChunk(BaseModel):
    id: UUID = Field(default_factory=uuid4)
    project_id: UUID
    tenant_id: str
    file_path: str
    chunk_index: int
    content: str
    embedding: list[float] | None = None
    metadata: dict | None = None


class IngestRequest(BaseModel):
    project_id: UUID
    tenant_id: str
    file_paths: list[str]


class IngestStatus(BaseModel):
    project_id: UUID
    status: str
    total_files: int
    processed_files: int
    total_chunks: int
