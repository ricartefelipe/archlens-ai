package dev.archlens.application.service;

import java.util.Base64;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import dev.archlens.application.port.out.KeycloakTokenGateway;
import dev.archlens.domain.exception.InvalidCredentialsException;
import dev.archlens.interfaces.rest.dto.request.LoginRequest;
import dev.archlens.interfaces.rest.dto.response.LoginResponse;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class AuthService {

    private final KeycloakTokenGateway tokenGateway;
    private final ObjectMapper objectMapper;

    @Inject
    public AuthService(KeycloakTokenGateway tokenGateway) {
        this.tokenGateway = tokenGateway;
        this.objectMapper = new ObjectMapper();
    }

    public LoginResponse login(LoginRequest request) {
        KeycloakTokenGateway.TokenResponse token = tokenGateway.passwordGrant(
                request.email().trim(),
                request.password());

        JwtClaims claims = parseAccessToken(token.accessToken());
        if (claims.tenantId() == null || claims.tenantId().isBlank()) {
            throw new InvalidCredentialsException();
        }

        return new LoginResponse(
                token.accessToken(),
                token.refreshToken(),
                token.expiresIn(),
                token.tokenType(),
                claims.tenantId(),
                claims.email());
    }

    private JwtClaims parseAccessToken(String accessToken) {
        try {
            String[] parts = accessToken.split("\\.");
            if (parts.length < 2) {
                throw new InvalidCredentialsException();
            }
            byte[] payload = Base64.getUrlDecoder().decode(parts[1]);
            JsonNode json = objectMapper.readTree(payload);
            String tenantId = json.path("tenant_id").asText(null);
            String email = json.path("email").asText(null);
            if (email == null || email.isBlank()) {
                email = json.path("preferred_username").asText(null);
            }
            return new JwtClaims(tenantId, email);
        } catch (InvalidCredentialsException e) {
            throw e;
        } catch (Exception e) {
            throw new InvalidCredentialsException();
        }
    }

    private record JwtClaims(String tenantId, String email) {
    }
}
