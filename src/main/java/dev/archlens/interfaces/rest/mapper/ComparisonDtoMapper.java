package dev.archlens.interfaces.rest.mapper;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import dev.archlens.domain.model.AnalysisComparisonResult;
import dev.archlens.domain.model.AnalysisComparisonResult.SeverityChangedRisk;
import dev.archlens.domain.model.ArchitecturalRisk;
import dev.archlens.domain.model.RiskSeverity;
import dev.archlens.interfaces.rest.dto.response.AnalysisComparisonResponse;
import dev.archlens.interfaces.rest.dto.response.AnalysisComparisonResponse.AnalysisSummaryResponse;
import dev.archlens.interfaces.rest.dto.response.AnalysisComparisonResponse.SeverityChangedRiskResponse;
import dev.archlens.interfaces.rest.dto.response.ArchitecturalRiskResponse;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class ComparisonDtoMapper {

    private final AnalysisDtoMapper analysisDtoMapper;

    public ComparisonDtoMapper(AnalysisDtoMapper analysisDtoMapper) {
        this.analysisDtoMapper = analysisDtoMapper;
    }

    public AnalysisComparisonResponse toResponse(AnalysisComparisonResult result) {
        return new AnalysisComparisonResponse(
                result.getProjectId(),
                toSummary(result.getBaseline()),
                toSummary(result.getCurrent()),
                toSeverityMap(result.getBaselineSeverityCounts()),
                toSeverityMap(result.getCurrentSeverityCounts()),
                mapRisks(result.getAdded()),
                mapRisks(result.getRemoved()),
                mapSeverityChanges(result.getSeverityChanged()),
                mapRisks(result.getUnchanged()));
    }

    private AnalysisSummaryResponse toSummary(AnalysisComparisonResult.AnalysisRef ref) {
        return new AnalysisSummaryResponse(ref.id(), ref.createdAt(), ref.riskCount());
    }

    private Map<String, Integer> toSeverityMap(Map<RiskSeverity, Integer> counts) {
        Map<String, Integer> mapped = new LinkedHashMap<>();
        for (RiskSeverity severity : RiskSeverity.values()) {
            mapped.put(severity.name(), counts.getOrDefault(severity, 0));
        }
        return mapped;
    }

    private List<ArchitecturalRiskResponse> mapRisks(List<ArchitecturalRisk> risks) {
        return risks.stream().map(analysisDtoMapper::toRiskResponse).toList();
    }

    private List<SeverityChangedRiskResponse> mapSeverityChanges(List<SeverityChangedRisk> changes) {
        return changes.stream()
                .map(change -> new SeverityChangedRiskResponse(
                        analysisDtoMapper.toRiskResponse(change.getBaselineRisk()),
                        analysisDtoMapper.toRiskResponse(change.getCurrentRisk())))
                .toList();
    }
}
