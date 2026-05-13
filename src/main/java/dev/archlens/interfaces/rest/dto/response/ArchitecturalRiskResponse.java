package dev.archlens.interfaces.rest.dto.response;

import java.util.UUID;

public record ArchitecturalRiskResponse(
        UUID id,
        String category,
        String categoryLabel,
        String severity,
        String title,
        String description,
        String filePath,
        String evidence,
        String suggestion) {
}
