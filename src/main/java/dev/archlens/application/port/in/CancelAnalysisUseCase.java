package dev.archlens.application.port.in;

import java.util.UUID;

import dev.archlens.domain.model.Analysis;

public interface CancelAnalysisUseCase {

    Analysis cancel(UUID projectId, UUID analysisId);
}
