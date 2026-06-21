package dev.archlens.infrastructure.persistence.entity;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "tenant_webhooks")
public class TenantWebhookEntity {

    @Id
    @Column(name = "id", nullable = false)
    public UUID id;

    @Column(name = "tenant_id", nullable = false, length = 128)
    public String tenantId;

    @Column(name = "url", nullable = false, length = 2048)
    public String url;

    @Column(name = "secret_hash", nullable = false, length = 64)
    public String secretHash;

    @Column(name = "events", nullable = false, length = 256)
    public String events;

    @Column(name = "enabled", nullable = false)
    public boolean enabled;

    @Column(name = "created_at", nullable = false)
    public Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    public Instant updatedAt;
}
