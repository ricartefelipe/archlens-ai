package dev.archlens.interfaces.rest.dto.response;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record AnalysisResponse(
        UUID id,
        UUID projectId,
        String status,
        String summary,
        List<ArchitecturalRiskResponse> risks,
        Instant createdAt,
        Instant updatedAt) {
}
