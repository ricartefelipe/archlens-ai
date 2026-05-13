from pathlib import Path
from uuid import UUID

import structlog

from app.chunking.strategies import ChunkerFactory
from app.config import settings
from app.domain.models import DocumentChunk, IngestStatus
from app.embedding.gateway import create_embedding_gateway
from app.persistence.repository import ChunkRepository

log = structlog.get_logger()

EMBEDDING_BATCH_SIZE = 64


class IngestService:
    def __init__(self) -> None:
        self._repository = ChunkRepository()
        self._embedding_gateway = create_embedding_gateway()

    async def ingest(
        self,
        project_id: UUID,
        tenant_id: str,
        file_paths: list[str] | None = None,
    ) -> IngestStatus:
        base = Path(settings.storage_base_path) / str(project_id)
        log.info("ingest_start", project_id=str(project_id), base_path=str(base))

        if file_paths:
            resolved_paths = [base / fp for fp in file_paths]
        else:
            resolved_paths = [p for p in base.rglob("*") if p.is_file()]

        total_files = len(resolved_paths)
        log.info("files_discovered", project_id=str(project_id), total=total_files)

        await self._repository.delete_by_project_id(project_id)
        log.info("old_chunks_deleted", project_id=str(project_id))

        all_chunks: list[DocumentChunk] = []
        processed = 0

        for file_path in resolved_paths:
            try:
                content = file_path.read_text(encoding="utf-8", errors="replace")
            except Exception:
                log.warning("file_read_error", file_path=str(file_path))
                continue

            relative_path = str(file_path.relative_to(base))
            chunker = ChunkerFactory.get_chunker(relative_path)
            raw_chunks = chunker.chunk_file(relative_path, content)

            for raw in raw_chunks:
                all_chunks.append(
                    DocumentChunk(
                        project_id=project_id,
                        tenant_id=tenant_id,
                        file_path=relative_path,
                        chunk_index=raw["chunk_index"],
                        content=raw["content"],
                        metadata=raw.get("metadata"),
                    )
                )

            processed += 1
            log.debug(
                "file_chunked",
                file_path=relative_path,
                chunks=len(raw_chunks),
                processed=processed,
                total=total_files,
            )

        log.info(
            "embedding_start",
            project_id=str(project_id),
            total_chunks=len(all_chunks),
        )

        texts = [c.content for c in all_chunks]
        for i in range(0, len(texts), EMBEDDING_BATCH_SIZE):
            batch_texts = texts[i : i + EMBEDDING_BATCH_SIZE]
            batch_embeddings = await self._embedding_gateway.generate(batch_texts)
            for j, emb in enumerate(batch_embeddings):
                all_chunks[i + j].embedding = emb

        log.info("embedding_done", project_id=str(project_id))

        await self._repository.save_chunks(all_chunks)
        log.info(
            "chunks_saved",
            project_id=str(project_id),
            total_chunks=len(all_chunks),
        )

        return IngestStatus(
            project_id=project_id,
            status="completed",
            total_files=total_files,
            processed_files=processed,
            total_chunks=len(all_chunks),
        )
