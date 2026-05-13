import logging
from uuid import UUID

import structlog
from fastapi import BackgroundTasks, FastAPI, HTTPException

from app.config import settings
from app.domain.models import IngestRequest, IngestStatus
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
