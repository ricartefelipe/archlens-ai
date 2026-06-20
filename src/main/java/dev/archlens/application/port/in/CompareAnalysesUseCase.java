package dev.archlens.application.port.in;

import java.util.UUID;

import dev.archlens.domain.model.AnalysisComparisonResult;

public interface CompareAnalysesUseCase {

    AnalysisComparisonResult compare(UUID projectId, UUID baselineAnalysisId, UUID currentAnalysisId);
}
