package dev.archlens.interfaces.rest.dto.response;

import java.time.LocalDate;

public record AccountUsageResponse(
        String tenantId,
        String plan,
        String planDisplayName,
        String status,
        int projectsUsed,
        int projectsLimit,
        int analysesUsed,
        int analysesLimit,
        long uploadMbUsed,
        int uploadMbLimit,
        LocalDate usagePeriodStart) {
}
