package dev.archlens.application.port.out;

public interface KeycloakTokenGateway {

    TokenResponse passwordGrant(String username, String password);

    record TokenResponse(
            String accessToken,
            String refreshToken,
            long expiresIn,
            String tokenType) {
    }
}
