import json
from uuid import UUID

from pgvector.sqlalchemy import Vector
from sqlalchemy import Column, DateTime, Integer, MetaData, String, Table, Text, delete, func, select
from sqlalchemy.dialects.postgresql import JSONB, UUID as PG_UUID, insert as pg_insert
from sqlalchemy.ext.asyncio import AsyncSession, create_async_engine
from sqlalchemy.orm import sessionmaker

from app.config import settings
from app.domain.models import DocumentChunk, IngestStatus

_async_url = settings.database_url.replace("postgresql://", "postgresql+psycopg://", 1)

engine = create_async_engine(_async_url, echo=False)
async_session_factory = sessionmaker(engine, class_=AsyncSession, expire_on_commit=False)

metadata = MetaData()

document_chunks = Table(
    "document_chunks",
    metadata,
    Column("id", PG_UUID(as_uuid=True), primary_key=True),
    Column("project_id", PG_UUID(as_uuid=True), nullable=False, index=True),
    Column("tenant_id", String, nullable=False),
    Column("file_path", String, nullable=False),
    Column("chunk_index", Integer, nullable=False),
    Column("content", Text, nullable=False),
    Column("embedding", Vector(settings.embedding_dimension)),
    Column("metadata", JSONB, nullable=True),
    Column("created_at", DateTime(timezone=True), server_default="now()"),
    extend_existing=True,
)

ingestion_status = Table(
    "ingestion_status",
    metadata,
    Column("project_id", PG_UUID(as_uuid=True), primary_key=True),
    Column("tenant_id", String, nullable=False),
    Column("status", String, nullable=False),
    Column("total_files", Integer, nullable=False, default=0),
    Column("processed_files", Integer, nullable=False, default=0),
    Column("total_chunks", Integer, nullable=False, default=0),
    Column("updated_at", DateTime(timezone=True), server_default=func.now()),
    extend_existing=True,
)


class ChunkRepository:
    async def save_chunks(self, chunks: list[DocumentChunk]) -> None:
        async with async_session_factory() as session:
            async with session.begin():
                for chunk in chunks:
                    await session.execute(
                        document_chunks.insert().values(
                            id=chunk.id,
                            project_id=chunk.project_id,
                            tenant_id=chunk.tenant_id,
                            file_path=chunk.file_path,
                            chunk_index=chunk.chunk_index,
                            content=chunk.content,
                            embedding=chunk.embedding,
                            metadata=chunk.metadata,
                        )
                    )

    async def delete_by_project_id(self, project_id: UUID) -> None:
        async with async_session_factory() as session:
            async with session.begin():
                await session.execute(
                    delete(document_chunks).where(
                        document_chunks.c.project_id == project_id
                    )
                )

    async def find_similar_with_scores(
        self, project_id: UUID, embedding: list[float], limit: int = 5
    ) -> list[tuple[DocumentChunk, float]]:
        async with async_session_factory() as session:
            distance_col = document_chunks.c.embedding.cosine_distance(embedding).label("distance")
            stmt = (
                select(document_chunks, distance_col)
                .where(document_chunks.c.project_id == project_id)
                .order_by(distance_col)
                .limit(limit)
            )
            result = await session.execute(stmt)
            rows = result.fetchall()

            return [
                (
                    DocumentChunk(
                        id=row.id,
                        project_id=row.project_id,
                        tenant_id=row.tenant_id,
                        file_path=row.file_path,
                        chunk_index=row.chunk_index,
                        content=row.content,
                        embedding=list(row.embedding) if row.embedding is not None else None,
                        metadata=json.loads(row.metadata) if isinstance(row.metadata, str) else row.metadata,
                    ),
                    1.0 - float(row.distance),
                )
                for row in rows
            ]

    async def find_similar(
        self, project_id: UUID, embedding: list[float], limit: int = 5
    ) -> list[DocumentChunk]:
        async with async_session_factory() as session:
            stmt = (
                select(document_chunks)
                .where(document_chunks.c.project_id == project_id)
                .order_by(document_chunks.c.embedding.cosine_distance(embedding))
                .limit(limit)
            )
            result = await session.execute(stmt)
            rows = result.fetchall()

            return [
                DocumentChunk(
                    id=row.id,
                    project_id=row.project_id,
                    tenant_id=row.tenant_id,
                    file_path=row.file_path,
                    chunk_index=row.chunk_index,
                    content=row.content,
                    embedding=list(row.embedding) if row.embedding is not None else None,
                    metadata=json.loads(row.metadata) if row.metadata else None,
                )
                for row in rows
            ]


class IngestStatusRepository:
    """Persiste o estado de ingestão por projeto, sobrevivendo a reinícios do worker."""

    async def upsert(self, status: IngestStatus, tenant_id: str) -> None:
        values = {
            "project_id": status.project_id,
            "tenant_id": tenant_id,
            "status": status.status,
            "total_files": status.total_files,
            "processed_files": status.processed_files,
            "total_chunks": status.total_chunks,
            "updated_at": func.now(),
        }
        stmt = pg_insert(ingestion_status).values(**values)
        stmt = stmt.on_conflict_do_update(
            index_elements=[ingestion_status.c.project_id],
            set_={
                "tenant_id": stmt.excluded.tenant_id,
                "status": stmt.excluded.status,
                "total_files": stmt.excluded.total_files,
                "processed_files": stmt.excluded.processed_files,
                "total_chunks": stmt.excluded.total_chunks,
                "updated_at": func.now(),
            },
        )
        async with async_session_factory() as session:
            async with session.begin():
                await session.execute(stmt)

    async def get(self, project_id: UUID) -> IngestStatus | None:
        async with async_session_factory() as session:
            stmt = select(ingestion_status).where(
                ingestion_status.c.project_id == project_id
            )
            row = (await session.execute(stmt)).first()

        if row is None:
            return None

        return IngestStatus(
            project_id=row.project_id,
            status=row.status,
            total_files=row.total_files,
            processed_files=row.processed_files,
            total_chunks=row.total_chunks,
        )
