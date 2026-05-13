package dev.archlens.interfaces.rest.dto.response;

import java.time.Instant;
import java.util.UUID;

public record ProjectResponse(
        UUID id,
        String tenantId,
        String name,
        String description,
        String status,
        int fileCount,
        Instant createdAt,
        Instant updatedAt) {
}
