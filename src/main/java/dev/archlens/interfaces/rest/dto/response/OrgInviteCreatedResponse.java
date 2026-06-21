package dev.archlens.interfaces.rest.dto.response;

import java.time.Instant;
import java.util.UUID;

public record OrgInviteCreatedResponse(
        UUID id,
        String email,
        String role,
        String token,
        Instant expiresAt) {
}
