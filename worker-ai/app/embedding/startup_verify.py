"""Validação opcional ao arranque: dimensão do vector devolve pelo gateway vs PostgreSQL/pgvector."""

import structlog

from app.config import settings
from app.embedding.gateway import create_embedding_gateway

log = structlog.get_logger()


async def verify_embedding_dimension_on_startup() -> None:
    if not settings.embedding_dimension_verify:
        log.info("embedding_dimension_verify_disabled")
        return

    gw = create_embedding_gateway()
    vectors = await gw.generate(["__archlens_embedding_probe__"])
    if not vectors or not vectors[0]:
        raise RuntimeError(
            "O gateway de embeddings devolveu um vector vazio. Verifique OPENAI_API_KEY / OLLAMA / provider."
        )
    actual = len(vectors[0])
    expected = settings.embedding_dimension
    if actual != expected:
        raise RuntimeError(
            f"Incompatibilidade de dimensão de embeddings: o modelo devolveu {actual}, "
            f"mas EMBEDDING_DIMENSION / ARCHLENS_EMBEDDING_DIMENSION está {expected}. "
            "Actualize o valor no worker e garanta que a coluna pgvector(document_chunks.embedding) "
            f"usa vector({expected}) coerente com o modelo escolhido — ver README (secção embeddings)."
        )
    log.info(
        "embedding_dimension_ok",
        dimension=actual,
        provider=settings.embedding_provider,
        model=settings.embedding_model,
    )
