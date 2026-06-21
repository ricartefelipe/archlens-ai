package dev.archlens.interfaces.rest.dto.response;

import java.time.Instant;
import java.util.UUID;

public record OrgMemberResponse(
        UUID id,
        String email,
        String role,
        String status,
        Instant createdAt,
        Instant updatedAt) {
}
