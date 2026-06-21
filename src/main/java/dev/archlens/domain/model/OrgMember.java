package dev.archlens.domain.model;

import java.time.Instant;
import java.util.UUID;

public class OrgMember {

    private UUID id;
    private String tenantId;
    private String email;
    private OrgMemberRole role;
    private OrgMemberStatus status;
    private Instant createdAt;
    private Instant updatedAt;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public OrgMemberRole getRole() {
        return role;
    }

    public void setRole(OrgMemberRole role) {
        this.role = role;
    }

    public OrgMemberStatus getStatus() {
        return status;
    }

    public void setStatus(OrgMemberStatus status) {
        this.status = status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
