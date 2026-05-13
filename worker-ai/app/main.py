import logging
from uuid import UUID

import structlog
from fastapi import BackgroundTasks, FastAPI, HTTPException

from app.config import settings
from app.domain.models import ChunkResult, ContextResponse, IngestRequest, IngestStatus, SearchRequest, SearchResult
from app.embedding.gateway import create_embedding_gateway
from app.persistence.repository import ChunkRepository
from app.service.ingest import IngestService

_LOG_LEVELS = {"DEBUG": 10, "INFO": 20, "WARNING": 30, "ERROR": 40, "CRITICAL": 50}

structlog.configure(
    processors=[
        structlog.contextvars.merge_contextvars,
        structlog.processors.add_log_level,
        structlog.processors.TimeStamper(fmt="iso"),
        structlog.processors.JSONRenderer(),
    ],
    wrapper_class=structlog.make_filtering_bound_logger(
        _LOG_LEVELS.get(settings.log_level.upper(), logging.INFO)
    ),
)

log = structlog.get_logger()

app = FastAPI(title="ArchLens AI Worker", version="0.1.0")

_ingest_statuses: dict[UUID, IngestStatus] = {}


@app.get("/health")
async def health() -> dict[str, str]:
    return {"status": "ok"}


@app.post("/v1/ingest/{project_id}", status_code=202)
async def trigger_ingest(
    project_id: UUID,
    request: IngestRequest,
    background_tasks: BackgroundTasks,
) -> IngestStatus:
    if request.project_id != project_id:
        raise HTTPException(status_code=400, detail="project_id mismatch")

    status = IngestStatus(
        project_id=project_id,
        status="pending",
        total_files=len(request.file_paths),
        processed_files=0,
        total_chunks=0,
    )
    _ingest_statuses[project_id] = status

    background_tasks.add_task(_run_ingest, request)
    log.info("ingest_queued", project_id=str(project_id))
    return status


@app.get("/v1/ingest/{project_id}/status")
async def ingest_status(project_id: UUID) -> IngestStatus:
    status = _ingest_statuses.get(project_id)
    if status is None:
        raise HTTPException(status_code=404, detail="No ingestion found for project")
    return status


@app.post("/v1/search")
async def semantic_search(request: SearchRequest) -> SearchResult:
    gateway = create_embedding_gateway()
    repository = ChunkRepository()

    embeddings = await gateway.generate([request.query])
    query_embedding = embeddings[0]

    results = await repository.find_similar_with_scores(
        project_id=request.project_id,
        embedding=query_embedding,
        limit=request.limit,
    )

    chunk_results = [
        ChunkResult(
            file_path=chunk.file_path,
            chunk_index=chunk.chunk_index,
            content=chunk.content,
            score=score,
            metadata=chunk.metadata,
        )
        for chunk, score in results
    ]

    log.info(
        "search_completed",
        project_id=str(request.project_id),
        results=len(chunk_results),
    )
    return SearchResult(
        project_id=request.project_id,
        query=request.query,
        results=chunk_results,
        total_results=len(chunk_results),
    )


@app.post("/v1/context")
async def build_context(request: SearchRequest) -> ContextResponse:
    search_result = await semantic_search(request)

    context_parts = []
    for r in search_result.results:
        context_parts.append(f"--- {r.file_path} (chunk {r.chunk_index}, score {r.score:.3f}) ---\n{r.content}\n")

    assembled = "\n".join(context_parts)

    log.info(
        "context_built",
        project_id=str(request.project_id),
        chunks=len(search_result.results),
    )
    return ContextResponse(
        context=assembled,
        sources=search_result.results,
        total_chunks=len(search_result.results),
    )


async def _run_ingest(request: IngestRequest) -> None:
    service = IngestService()
    try:
        result = await service.ingest(
            project_id=request.project_id,
            tenant_id=request.tenant_id,
            file_paths=request.file_paths,
        )
        _ingest_statuses[request.project_id] = result
    except Exception:
        log.exception("ingest_failed", project_id=str(request.project_id))
        _ingest_statuses[request.project_id] = IngestStatus(
            project_id=request.project_id,
            status="failed",
            total_files=len(request.file_paths),
            processed_files=0,
            total_chunks=0,
        )
