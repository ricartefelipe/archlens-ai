package dev.archlens.infrastructure.persistence.entity;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "api_keys")
public class ApiKeyEntity {

    @Id
    @Column(name = "id", nullable = false)
    public UUID id;

    @Column(name = "tenant_id", nullable = false, length = 128)
    public String tenantId;

    @Column(name = "name", nullable = false, length = 128)
    public String name;

    @Column(name = "key_prefix", nullable = false, length = 16)
    public String keyPrefix;

    @Column(name = "key_hash", nullable = false, length = 64)
    public String keyHash;

    @Column(name = "scopes", nullable = false, length = 128)
    public String scopes;

    @Column(name = "created_at", nullable = false)
    public Instant createdAt;

    @Column(name = "revoked_at")
    public Instant revokedAt;

    @Column(name = "last_used_at")
    public Instant lastUsedAt;
}
