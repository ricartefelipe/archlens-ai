"""Uma ingestão sem arquivos utilizáveis não pode apagar o índice existente."""

import asyncio
from uuid import uuid4

from app.domain.models import IngestStatus
from app.service.ingest import IngestService


def _run(coro):
    return asyncio.run(coro)


class _SpyRepository:
    def __init__(self, existing_chunks: int = 7) -> None:
        self.deleted = False
        self.saved: list = []
        self._existing = existing_chunks

    async def delete_by_project_id(self, project_id) -> None:
        self.deleted = True

    async def save_chunks(self, chunks) -> None:
        self.saved.extend(chunks)

    async def count_by_project_id(self, project_id) -> int:
        return self._existing


class _NoopEmbeddingGateway:
    async def generate(self, texts):
        return [[0.0] * 8 for _ in texts]


def _service(tmp_path, monkeypatch, repository):
    """Monta o serviço sem tocar no banco nem no provedor de embeddings."""
    from app import config

    monkeypatch.setattr(config.settings, "storage_base_path", str(tmp_path))
    service = IngestService.__new__(IngestService)
    service._repository = repository
    service._embedding_gateway = _NoopEmbeddingGateway()
    return service


def test_invalid_paths_do_not_delete_existing_index(tmp_path, monkeypatch):
    project_id = uuid4()
    (tmp_path / str(project_id)).mkdir()

    repository = _SpyRepository(existing_chunks=7)
    service = _service(tmp_path, monkeypatch, repository)

    status = _run(
        service.ingest(
            project_id,
            "tenant-1",
            ["../outro-projeto/App.java", "/etc/passwd", "nao-existe.java"],
        )
    )

    assert repository.deleted is False, "índice não deve ser apagado sem nada para reindexar"
    assert repository.saved == []
    assert isinstance(status, IngestStatus)
    assert status.status == "no_files"
    assert status.total_files == 0
    # Reporta o que continua indexado, para o chamador ver que foi preservado.
    assert status.total_chunks == 7


def test_only_sensitive_files_do_not_delete_existing_index(tmp_path, monkeypatch):
    project_id = uuid4()
    base = tmp_path / str(project_id)
    base.mkdir()
    (base / ".env").write_text("API_KEY=abc")
    (base / "secrets.yml").write_text("pw: x")

    repository = _SpyRepository(existing_chunks=3)
    service = _service(tmp_path, monkeypatch, repository)

    status = _run(service.ingest(project_id, "tenant-1", [".env", "secrets.yml"]))

    assert repository.deleted is False
    assert status.status == "no_files"
    assert status.total_chunks == 3


def test_valid_files_still_reindex(tmp_path, monkeypatch):
    project_id = uuid4()
    base = tmp_path / str(project_id)
    base.mkdir()
    (base / "App.java").write_text("class App {}")

    repository = _SpyRepository()
    service = _service(tmp_path, monkeypatch, repository)

    status = _run(service.ingest(project_id, "tenant-1", ["App.java"]))

    assert repository.deleted is True, "reindexação legítima troca o índice"
    assert status.status == "completed"
    assert status.total_files == 1
    assert repository.saved, "chunks do arquivo válido devem ser persistidos"
