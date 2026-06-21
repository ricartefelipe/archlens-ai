package dev.archlens.infrastructure.persistence.mapper;

import dev.archlens.domain.model.TenantWebhook;
import dev.archlens.infrastructure.persistence.entity.TenantWebhookEntity;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class TenantWebhookPersistenceMapper {

    public TenantWebhookEntity toEntity(TenantWebhook webhook) {
        TenantWebhookEntity entity = new TenantWebhookEntity();
        entity.id = webhook.getId();
        entity.tenantId = webhook.getTenantId();
        entity.url = webhook.getUrl();
        entity.secretHash = webhook.getSecretHash();
        entity.events = webhook.getEvents();
        entity.enabled = webhook.isEnabled();
        entity.createdAt = webhook.getCreatedAt();
        entity.updatedAt = webhook.getUpdatedAt();
        return entity;
    }

    public TenantWebhook toDomain(TenantWebhookEntity entity) {
        TenantWebhook webhook = new TenantWebhook();
        webhook.setId(entity.id);
        webhook.setTenantId(entity.tenantId);
        webhook.setUrl(entity.url);
        webhook.setSecretHash(entity.secretHash);
        webhook.setEvents(entity.events);
        webhook.setEnabled(entity.enabled);
        webhook.setCreatedAt(entity.createdAt);
        webhook.setUpdatedAt(entity.updatedAt);
        return webhook;
    }
}
