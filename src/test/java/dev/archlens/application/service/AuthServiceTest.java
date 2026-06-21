package dev.archlens.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Base64;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import dev.archlens.application.port.out.KeycloakTokenGateway;
import dev.archlens.domain.exception.InvalidCredentialsException;
import dev.archlens.interfaces.rest.dto.request.LoginRequest;

class AuthServiceTest {

    private FakeKeycloakTokenGateway tokenGateway;
    private AuthService authService;

    @BeforeEach
    void setUp() {
        tokenGateway = new FakeKeycloakTokenGateway();
        authService = new AuthService(tokenGateway);
    }

    @Test
    void loginReturnsTenantAndTokenFromJwtClaims() {
        String token = jwtWithClaims("{\"tenant_id\":\"tenant-1\",\"email\":\"architect@archlens.dev\"}");
        tokenGateway.setNextResponse(
                new KeycloakTokenGateway.TokenResponse(token, "refresh", 300, "Bearer"));

        var response = authService.login(new LoginRequest("architect@archlens.dev", "secret"));

        assertEquals("tenant-1", response.tenantId());
        assertEquals("architect@archlens.dev", response.email());
        assertEquals(token, response.accessToken());
        assertEquals("architect@archlens.dev", tokenGateway.lastEmail());
        assertEquals("secret", tokenGateway.lastPassword());
    }

    @Test
    void loginFailsWhenTenantClaimMissing() {
        String token = jwtWithClaims("{\"email\":\"architect@archlens.dev\"}");
        tokenGateway.setNextResponse(
                new KeycloakTokenGateway.TokenResponse(token, null, 300, "Bearer"));

        assertThrows(InvalidCredentialsException.class,
                () -> authService.login(new LoginRequest("architect@archlens.dev", "secret")));
    }

    private static String jwtWithClaims(String jsonPayload) {
        String header = Base64.getUrlEncoder().withoutPadding()
                .encodeToString("{\"alg\":\"none\"}".getBytes());
        String payload = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(jsonPayload.getBytes());
        return header + "." + payload + ".sig";
    }

    private static final class FakeKeycloakTokenGateway implements KeycloakTokenGateway {

        private KeycloakTokenGateway.TokenResponse nextResponse;
        private String lastEmail;
        private String lastPassword;

        void setNextResponse(KeycloakTokenGateway.TokenResponse nextResponse) {
            this.nextResponse = nextResponse;
        }

        String lastEmail() {
            return lastEmail;
        }

        String lastPassword() {
            return lastPassword;
        }

        @Override
        public TokenResponse passwordGrant(String email, String password) {
            lastEmail = email;
            lastPassword = password;
            return nextResponse;
        }
    }
}
