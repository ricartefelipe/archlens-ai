package dev.archlens.interfaces.rest.dto.response;

import java.time.Instant;
import java.util.UUID;

public record ProjectFileResponse(
        UUID id,
        UUID projectId,
        String filePath,
        String fileType,
        String fileTypeLabel,
        long sizeBytes,
        String contentHash,
        Instant createdAt) {
}
