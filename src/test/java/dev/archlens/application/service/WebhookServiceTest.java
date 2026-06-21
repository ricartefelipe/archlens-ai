package dev.archlens.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import dev.archlens.application.port.out.TenantWebhookRepositoryPort;
import dev.archlens.domain.exception.WebhookNotFoundException;
import dev.archlens.domain.model.TenantWebhook;
import dev.archlens.infrastructure.security.SecureTokenHasher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class WebhookServiceTest {

    private InMemoryWebhookRepository repository;
    private WebhookService webhookService;

    @BeforeEach
    void setUp() {
        repository = new InMemoryWebhookRepository();
        webhookService = new WebhookService(repository, new SecureTokenHasher());
    }

    @Test
    @DisplayName("criação retorna secret plain text uma vez")
    void createReturnsSecretOnce() {
        WebhookService.CreatedWebhook created = webhookService.create(
                "tenant-a",
                "https://example.com/hook",
                List.of(WebhookService.EVENT_ANALYSIS_COMPLETED));

        assertNotNull(created.secret());
        assertTrue(created.secret().length() >= 32);
        assertEquals("https://example.com/hook", created.webhook().getUrl());
        assertTrue(created.webhook().isEnabled());
    }

    @Test
    @DisplayName("atualização altera url e eventos")
    void updateChangesUrlAndEvents() {
        WebhookService.CreatedWebhook created = webhookService.create(
                "tenant-a",
                "https://example.com/hook",
                List.of(WebhookService.EVENT_ANALYSIS_COMPLETED));

        TenantWebhook updated = webhookService.update(
                "tenant-a",
                created.webhook().getId(),
                "https://example.com/new",
                List.of(WebhookService.EVENT_ANALYSIS_FAILED),
                false);

        assertEquals("https://example.com/new", updated.getUrl());
        assertFalse(updated.isEnabled());
        assertTrue(updated.getEvents().contains(WebhookService.EVENT_ANALYSIS_FAILED));
    }

    @Test
    @DisplayName("exclusão remove webhook do tenant")
    void deleteRemovesWebhook() {
        WebhookService.CreatedWebhook created = webhookService.create(
                "tenant-a",
                "https://example.com/hook",
                List.of(WebhookService.EVENT_ANALYSIS_COMPLETED));

        webhookService.delete("tenant-a", created.webhook().getId());

        assertTrue(webhookService.list("tenant-a").isEmpty());
    }

    @Test
    @DisplayName("excluir webhook inexistente lança exceção")
    void deleteMissingThrows() {
        assertThrows(WebhookNotFoundException.class,
                () -> webhookService.delete("tenant-a", UUID.randomUUID()));
    }

    @Test
    @DisplayName("listagem retorna webhooks do tenant")
    void listReturnsTenantWebhooks() {
        webhookService.create("tenant-a", "https://a.example/hook", List.of(WebhookService.EVENT_ANALYSIS_COMPLETED));
        webhookService.create("tenant-a", "https://a.example/hook2", List.of(WebhookService.EVENT_ANALYSIS_FAILED));
        webhookService.create("tenant-b", "https://b.example/hook", List.of(WebhookService.EVENT_ANALYSIS_COMPLETED));

        assertEquals(2, webhookService.list("tenant-a").size());
    }

    private static final class InMemoryWebhookRepository implements TenantWebhookRepositoryPort {
        private final List<TenantWebhook> store = new ArrayList<>();

        @Override
        public TenantWebhook save(TenantWebhook webhook) {
            store.removeIf(w -> w.getId().equals(webhook.getId()));
            store.add(webhook);
            return webhook;
        }

        @Override
        public Optional<TenantWebhook> findById(UUID id) {
            return store.stream().filter(w -> w.getId().equals(id)).findFirst();
        }

        @Override
        public Optional<TenantWebhook> findByIdAndTenantId(UUID id, String tenantId) {
            return store.stream()
                    .filter(w -> w.getId().equals(id) && tenantId.equals(w.getTenantId()))
                    .findFirst();
        }

        @Override
        public List<TenantWebhook> findAllByTenantId(String tenantId) {
            return store.stream().filter(w -> tenantId.equals(w.getTenantId())).toList();
        }

        @Override
        public void deleteByIdAndTenantId(UUID id, String tenantId) {
            store.removeIf(w -> w.getId().equals(id) && tenantId.equals(w.getTenantId()));
        }
    }
}
