package dev.archlens.infrastructure.messaging;

import java.util.UUID;

public record AnalysisEvent(
        UUID analysisId,
        UUID projectId,
        String tenantId) {
}
