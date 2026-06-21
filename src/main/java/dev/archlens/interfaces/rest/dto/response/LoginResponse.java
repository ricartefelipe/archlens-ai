package dev.archlens.interfaces.rest.dto.response;

public record LoginResponse(
        String accessToken,
        String refreshToken,
        long expiresIn,
        String tokenType,
        String tenantId,
        String email) {
}
