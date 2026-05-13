package dev.archlens.infrastructure.config;

import io.smallrye.config.WithDefault;
import io.smallrye.config.ConfigMapping;

/** Dimensão alinhada com {@code document_chunks.embedding} (pgvector) e com o worker. */
@ConfigMapping(prefix = "archlens.embedding")
public interface ArchlensEmbeddingConfig {

    /** Deve coincidir com {@code EMBEDDING_DIMENSION} no worker e com {@code vector(N)} na BD. */
    @WithDefault("1536")
    int dimension();
}
