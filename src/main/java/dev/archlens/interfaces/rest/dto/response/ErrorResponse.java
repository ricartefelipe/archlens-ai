package dev.archlens.interfaces.rest.dto.response;

import java.time.Instant;

public record ErrorResponse(
        String correlationId,
        int status,
        String error,
        String message,
        Instant timestamp) {
}
