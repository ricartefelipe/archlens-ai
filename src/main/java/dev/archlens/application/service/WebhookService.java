package dev.archlens.application.service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import dev.archlens.application.port.out.TenantWebhookRepositoryPort;
import dev.archlens.domain.exception.WebhookNotFoundException;
import dev.archlens.domain.model.TenantWebhook;
import dev.archlens.infrastructure.persistence.rls.TenantScopedRls;
import dev.archlens.infrastructure.security.SecureTokenHasher;
import io.vertx.core.json.JsonObject;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.jboss.logging.Logger;

@TenantScopedRls
@ApplicationScoped
public class WebhookService {

    public static final String EVENT_ANALYSIS_COMPLETED = "analysis.completed";
    public static final String EVENT_ANALYSIS_FAILED = "analysis.failed";

    private static final Logger LOG = Logger.getLogger(WebhookService.class);

    private final TenantWebhookRepositoryPort repository;
    private final SecureTokenHasher tokenHasher;
    private final HttpClient httpClient;

    @Inject
    public WebhookService(TenantWebhookRepositoryPort repository, SecureTokenHasher tokenHasher) {
        this.repository = repository;
        this.tokenHasher = tokenHasher;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    public record CreatedWebhook(TenantWebhook webhook, String secret) {
    }

    public List<TenantWebhook> list(String tenantId) {
        return repository.findAllByTenantId(tenantId);
    }

    @Transactional
    public CreatedWebhook create(String tenantId, String url, List<String> events) {
        String secret = tokenHasher.randomToken(32);
        Instant now = Instant.now();

        TenantWebhook webhook = new TenantWebhook();
        webhook.setId(UUID.randomUUID());
        webhook.setTenantId(tenantId);
        webhook.setUrl(url);
        webhook.setSecretHash(secret);
        webhook.setEvents(joinEvents(events));
        webhook.setEnabled(true);
        webhook.setCreatedAt(now);
        webhook.setUpdatedAt(now);

        TenantWebhook saved = repository.save(webhook);
        return new CreatedWebhook(saved, secret);
    }

    @Transactional
    public TenantWebhook update(String tenantId, UUID webhookId, String url, List<String> events, Boolean enabled) {
        TenantWebhook webhook = repository.findByIdAndTenantId(webhookId, tenantId)
                .orElseThrow(() -> new WebhookNotFoundException(webhookId));

        if (url != null && !url.isBlank()) {
            webhook.setUrl(url);
        }
        if (events != null && !events.isEmpty()) {
            webhook.setEvents(joinEvents(events));
        }
        if (enabled != null) {
            webhook.setEnabled(enabled);
        }
        webhook.setUpdatedAt(Instant.now());
        return repository.save(webhook);
    }

    @Transactional
    public void delete(String tenantId, UUID webhookId) {
        if (repository.findByIdAndTenantId(webhookId, tenantId).isEmpty()) {
            throw new WebhookNotFoundException(webhookId);
        }
        repository.deleteByIdAndTenantId(webhookId, tenantId);
    }

    public void dispatch(String tenantId, String event, Map<String, Object> payload) {
        List<TenantWebhook> webhooks = repository.findAllByTenantId(tenantId);
        for (TenantWebhook webhook : webhooks) {
            if (!webhook.isEnabled() || !webhook.subscribesTo(event)) {
                continue;
            }
            sendWebhook(webhook, event, payload);
        }
    }

    private void sendWebhook(TenantWebhook webhook, String event, Map<String, Object> payload) {
        try {
            JsonObject body = new JsonObject();
            body.put("event", event);
            body.put("timestamp", Instant.now().toString());
            body.put("data", payload);

            String json = body.encode();
            String signature = sign(json, webhook.getSecretHash());

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(webhook.getUrl()))
                    .timeout(Duration.ofSeconds(15))
                    .header("Content-Type", "application/json")
                    .header("X-ArchLens-Event", event)
                    .header("X-ArchLens-Signature", signature)
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 400) {
                LOG.warnf("Webhook %s returned status %d for event %s", webhook.getId(),
                        response.statusCode(), event);
            }
        } catch (Exception e) {
            LOG.warnf(e, "Failed to dispatch webhook %s for event %s", webhook.getId(), event);
        }
    }

    private String sign(String payload, String secret) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] hash = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            return bytesToHex(hash);
        } catch (Exception e) {
            throw new IllegalStateException("HMAC signing failed", e);
        }
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    private static String joinEvents(List<String> events) {
        return String.join(",", events);
    }
}
