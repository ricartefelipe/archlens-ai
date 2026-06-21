package dev.archlens.interfaces.rest.dto.response;

import java.time.Instant;
import java.util.UUID;

public record WebhookCreatedResponse(
        UUID id,
        String url,
        String secret,
        String events,
        boolean enabled,
        Instant createdAt) {
}
