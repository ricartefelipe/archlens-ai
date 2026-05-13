package dev.archlens.application.port.out;

import java.util.List;
import java.util.UUID;

public interface AnalysisGateway {

    AnalysisGatewayResult analyzeProject(UUID projectId, String tenantId);

    List<AdrSuggestion> generateAdrs(UUID projectId, List<RiskFindingDto> findings);

    record AnalysisGatewayResult(
            String summary,
            List<RiskFindingDto> findings,
            int totalFilesAnalyzed) {
    }

    record RiskFindingDto(
            UUID id,
            String category,
            String severity,
            String title,
            String description,
            String filePath,
            String evidence,
            String suggestion) {
    }

    record AdrSuggestion(
            UUID id,
            String title,
            String context,
            String decision,
            String consequences,
            String status,
            List<UUID> relatedFindings) {
    }
}
