package dev.archlens.interfaces.rest.dto.response;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record AdrResponse(
        UUID id,
        UUID analysisId,
        String title,
        String context,
        String decision,
        String consequences,
        String status,
        List<UUID> relatedFindings,
        Instant createdAt) {
}
