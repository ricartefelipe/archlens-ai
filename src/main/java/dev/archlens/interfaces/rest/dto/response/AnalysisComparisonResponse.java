package dev.archlens.interfaces.rest.dto.response;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record AnalysisComparisonResponse(
        UUID projectId,
        AnalysisSummaryResponse baseline,
        AnalysisSummaryResponse current,
        Map<String, Integer> baselineSeverityCounts,
        Map<String, Integer> currentSeverityCounts,
        java.util.List<ArchitecturalRiskResponse> added,
        java.util.List<ArchitecturalRiskResponse> removed,
        java.util.List<SeverityChangedRiskResponse> severityChanged,
        java.util.List<ArchitecturalRiskResponse> unchanged) {

    public record AnalysisSummaryResponse(UUID id, Instant createdAt, int riskCount) {
    }

    public record SeverityChangedRiskResponse(
            ArchitecturalRiskResponse baselineRisk,
            ArchitecturalRiskResponse currentRisk) {
    }
}
