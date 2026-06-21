package dev.archlens.infrastructure.persistence.entity;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "org_members")
public class OrgMemberEntity {

    @Id
    @Column(name = "id", nullable = false)
    public UUID id;

    @Column(name = "tenant_id", nullable = false, length = 128)
    public String tenantId;

    @Column(name = "email", nullable = false, length = 320)
    public String email;

    @Column(name = "role", nullable = false, length = 32)
    public String role;

    @Column(name = "status", nullable = false, length = 32)
    public String status;

    @Column(name = "created_at", nullable = false)
    public Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    public Instant updatedAt;
}
