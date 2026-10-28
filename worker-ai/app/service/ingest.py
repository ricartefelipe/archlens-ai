from pathlib import Path
from uuid import UUID

import structlog

from app.chunking.strategies import ChunkerFactory
from app.config import settings
from app.domain.models import DocumentChunk, IngestStatus
from app.embedding.gateway import create_embedding_gateway
from app.persistence.repository import ChunkRepository
from app.service.file_filter import selectable_paths

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
        base = (Path(settings.storage_base_path) / str(project_id)).resolve()
        log.info("ingest_start", project_id=str(project_id), base_path=str(base))

        # Os caminhos chegam pela API: precisam ficar contidos no diretório do
        # projeto (sem travessia para outro tenant) e excluir credenciais.
        requested = len(file_paths) if file_paths else 0
        resolved_paths = selectable_paths(base, file_paths)

        total_files = len(resolved_paths)
        log.info(
            "files_discovered",
            project_id=str(project_id),
            total=total_files,
            requested=requested,
            skipped=max(requested - total_files, 0),
        )

        # Nenhum caminho utilizável: preserva o índice atual em vez de apagá-lo.
        # A ordem anterior (apagar e depois descobrir) deixava o projeto sem
        # índice quando a lista recebida era inválida ou só tinha arquivos
        # sensíveis, sem nada para reindexar em seguida.
        if not resolved_paths:
            log.warning(
                "ingest_skipped_no_usable_files",
                project_id=str(project_id),
                requested=requested,
            )
            return await self._status_for_empty_ingest(project_id, requested)

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

    async def _status_for_empty_ingest(
        self, project_id: UUID, requested: int
    ) -> IngestStatus:
        """Status de uma ingestão sem arquivos utilizáveis, sem tocar no índice.

        `total_chunks` reporta o que o projeto ainda tem indexado, para que o
        chamador veja que o índice anterior foi preservado.
        """
        try:
            existing = await self._repository.count_by_project_id(project_id)
        except Exception:  # noqa: BLE001 - contagem é informativa
            log.warning("chunk_count_failed", project_id=str(project_id))
            existing = 0

        return IngestStatus(
            project_id=project_id,
            status="no_files",
            total_files=0,
            processed_files=0,
            total_chunks=existing,
        )
