package dev.archlens.interfaces.rest.dto.response;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record WebhookResponse(
        UUID id,
        String url,
        List<String> events,
        boolean enabled,
        Instant createdAt,
        Instant updatedAt) {
}
