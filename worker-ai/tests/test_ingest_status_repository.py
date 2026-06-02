import asyncio
from uuid import uuid4

import pytest
from sqlalchemy import text

from app.domain.models import IngestStatus
from app.persistence.repository import IngestStatusRepository, engine, ingestion_status


def _run(coro_factory):
    """Executa uma corrotina num loop dedicado e descarta o pool ao final,
    evitando reaproveitar conexões presas a um event loop já encerrado."""

    async def _wrapper():
        try:
            return await coro_factory()
        finally:
            await engine.dispose()

    return asyncio.run(_wrapper())


def _db_available() -> bool:
    async def _check():
        async with engine.connect() as conn:
            await conn.execute(text("SELECT 1"))

    try:
        _run(_check)
        return True
    except Exception:
        return False


pytestmark = pytest.mark.skipif(not _db_available(), reason="PostgreSQL indisponível para teste de integração")


def _reset_table():
    async def _setup():
        async with engine.begin() as conn:
            await conn.run_sync(lambda c: ingestion_status.create(c, checkfirst=True))
            await conn.execute(ingestion_status.delete())

    _run(_setup)


def setup_function():
    _reset_table()


def test_get_returns_none_when_absent():
    async def _scenario():
        return await IngestStatusRepository().get(uuid4())

    assert _run(_scenario) is None


def test_upsert_then_get_roundtrip():
    project_id = uuid4()
    status = IngestStatus(
        project_id=project_id,
        status="completed",
        total_files=3,
        processed_files=3,
        total_chunks=12,
    )

    async def _scenario():
        repo = IngestStatusRepository()
        await repo.upsert(status, "tenant-1")
        return await repo.get(project_id)

    result = _run(_scenario)
    assert result is not None
    assert result.status == "completed"
    assert result.total_files == 3
    assert result.total_chunks == 12


def test_upsert_updates_existing_row():
    project_id = uuid4()

    async def _scenario():
        repo = IngestStatusRepository()
        await repo.upsert(
            IngestStatus(project_id=project_id, status="pending", total_files=2, processed_files=0, total_chunks=0),
            "tenant-1",
        )
        await repo.upsert(
            IngestStatus(project_id=project_id, status="completed", total_files=2, processed_files=2, total_chunks=8),
            "tenant-1",
        )
        return await repo.get(project_id)

    result = _run(_scenario)
    assert result.status == "completed"
    assert result.processed_files == 2
    assert result.total_chunks == 8
