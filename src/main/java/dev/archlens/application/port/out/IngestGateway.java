package dev.archlens.application.port.out;

import java.util.List;
import java.util.UUID;

public interface IngestGateway {

    IngestJobStatus triggerIngest(UUID projectId, String tenantId, List<String> filePaths);

    IngestJobStatus getStatus(UUID projectId);

    record IngestJobStatus(
            UUID projectId,
            String status,
            int totalFiles,
            int processedFiles,
            int totalChunks) {
    }
}
