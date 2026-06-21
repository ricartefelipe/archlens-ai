package dev.archlens.interfaces.rest.dto.response;

import java.time.Instant;
import java.util.UUID;

public record OrgInviteResponse(
        UUID id,
        String email,
        String role,
        Instant expiresAt,
        Instant createdAt) {
}
