package dev.archlens.interfaces.rest.dto.response;

import java.time.Instant;
import java.util.UUID;

public record ApiKeyResponse(
        UUID id,
        String name,
        String keyPrefix,
        String scopes,
        Instant createdAt,
        Instant revokedAt,
        Instant lastUsedAt) {
}
