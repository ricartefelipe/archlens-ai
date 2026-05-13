package dev.archlens.interfaces.rest.mapper;

import java.util.Collections;
import java.util.List;

import dev.archlens.domain.model.Analysis;
import dev.archlens.domain.model.ArchitecturalRisk;
import dev.archlens.interfaces.rest.dto.response.AnalysisResponse;
import dev.archlens.interfaces.rest.dto.response.ArchitecturalRiskResponse;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class AnalysisDtoMapper {

    public AnalysisResponse toResponse(Analysis domain) {
        List<ArchitecturalRiskResponse> riskResponses = domain.getRisks() != null
                ? domain.getRisks().stream().map(this::toRiskResponse).toList()
                : Collections.emptyList();

        return new AnalysisResponse(
                domain.getId(),
                domain.getProjectId(),
                domain.getStatus().name(),
                domain.getSummary(),
                riskResponses,
                domain.getCreatedAt(),
                domain.getUpdatedAt());
    }

    private ArchitecturalRiskResponse toRiskResponse(ArchitecturalRisk risk) {
        return new ArchitecturalRiskResponse(
                risk.getId(),
                risk.getCategory().name(),
                risk.getCategory().getLabel(),
                risk.getSeverity().name(),
                risk.getTitle(),
                risk.getDescription(),
                risk.getFilePath(),
                risk.getEvidence(),
                risk.getSuggestion());
    }
}
