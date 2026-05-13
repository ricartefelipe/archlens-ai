package dev.archlens.application.port.out;

import dev.archlens.domain.model.RiskCategory;
import dev.archlens.domain.model.RiskSeverity;

public record LlmRiskFinding(
        RiskCategory category,
        RiskSeverity severity,
        String title,
        String description,
        String filePath,
        String evidence,
        String suggestion) {
}
