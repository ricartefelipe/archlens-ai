package dev.archlens.application.port.out;

import java.util.List;
import java.util.UUID;

public interface IngestGateway {

    IngestJobStatus triggerIngest(UUID projectId, String tenantId, List<String> filePaths);

    IngestJobStatus getStatus(UUID projectId);

    /**
     * Apaga chunks, vetores e estado de ingestão do projeto no worker.
     * Parte do erasure: precisa concluir antes de remover o projeto do banco,
     * senão o índice vira órfão sem dono para apagá-lo depois.
     */
    void deleteProjectData(UUID projectId);

    record IngestJobStatus(
            UUID projectId,
            String status,
            int totalFiles,
            int processedFiles,
            int totalChunks) {
    }
}
