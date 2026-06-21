package dev.archlens.infrastructure.gateway;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import dev.archlens.application.port.out.KeycloakTokenGateway;
import dev.archlens.domain.exception.InvalidCredentialsException;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class KeycloakTokenAdapter implements KeycloakTokenGateway {

    private static final Logger LOG = Logger.getLogger(KeycloakTokenAdapter.class);

    private final String tokenUrl;
    private final String clientId;
    private final String clientSecret;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public KeycloakTokenAdapter(
            @ConfigProperty(name = "archlens.auth.keycloak-token-url") String tokenUrl,
            @ConfigProperty(name = "archlens.auth.client-id", defaultValue = "archlens-bff") String clientId,
            @ConfigProperty(name = "archlens.auth.client-secret", defaultValue = "") String clientSecret) {
        this.tokenUrl = tokenUrl;
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public TokenResponse passwordGrant(String username, String password) {
        StringBuilder form = new StringBuilder();
        append(form, "grant_type", "password");
        append(form, "client_id", clientId);
        append(form, "username", username);
        append(form, "password", password);
        if (clientSecret != null && !clientSecret.isBlank()) {
            append(form, "client_secret", clientSecret);
        }
        return requestToken(form.toString());
    }

    @Override
    public TokenResponse refreshGrant(String refreshToken) {
        StringBuilder form = new StringBuilder();
        append(form, "grant_type", "refresh_token");
        append(form, "client_id", clientId);
        append(form, "refresh_token", refreshToken);
        if (clientSecret != null && !clientSecret.isBlank()) {
            append(form, "client_secret", clientSecret);
        }
        return requestToken(form.toString());
    }

    private TokenResponse requestToken(String formBody) {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(tokenUrl))
                .timeout(Duration.ofSeconds(15))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(formBody))
                .build();

        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 401 || response.statusCode() == 400) {
                throw new InvalidCredentialsException();
            }
            if (response.statusCode() >= 300) {
                LOG.warnf("Keycloak token endpoint returned %d", response.statusCode());
                throw new InvalidCredentialsException();
            }

            JsonNode body = objectMapper.readTree(response.body());
            return new TokenResponse(
                    body.path("access_token").asText(),
                    body.path("refresh_token").asText(null),
                    body.path("expires_in").asLong(300),
                    body.path("token_type").asText("Bearer"));
        } catch (InvalidCredentialsException e) {
            throw e;
        } catch (Exception e) {
            LOG.errorf(e, "Failed to obtain token from Keycloak");
            throw new InvalidCredentialsException();
        }
    }

    private static void append(StringBuilder form, String key, String value) {
        if (!form.isEmpty()) {
            form.append('&');
        }
        form.append(URLEncoder.encode(key, StandardCharsets.UTF_8));
        form.append('=');
        form.append(URLEncoder.encode(value, StandardCharsets.UTF_8));
    }
}
