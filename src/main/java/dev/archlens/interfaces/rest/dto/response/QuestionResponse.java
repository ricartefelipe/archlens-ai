package dev.archlens.interfaces.rest.dto.response;

import java.time.Instant;
import java.util.UUID;

public record QuestionResponse(
        UUID id,
        UUID analysisId,
        String question,
        String answer,
        String sources,
        Instant createdAt) {
}
