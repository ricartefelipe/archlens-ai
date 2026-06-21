package dev.archlens.infrastructure.persistence.entity;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "org_invites")
public class OrgInviteEntity {

    @Id
    @Column(name = "id", nullable = false)
    public UUID id;

    @Column(name = "tenant_id", nullable = false, length = 128)
    public String tenantId;

    @Column(name = "email", nullable = false, length = 320)
    public String email;

    @Column(name = "role", nullable = false, length = 32)
    public String role;

    @Column(name = "token_hash", nullable = false, length = 64)
    public String tokenHash;

    @Column(name = "expires_at", nullable = false)
    public Instant expiresAt;

    @Column(name = "accepted_at")
    public Instant acceptedAt;

    @Column(name = "created_at", nullable = false)
    public Instant createdAt;
}
