package dev.archlens.infrastructure.persistence.entity;

import java.time.Instant;
import java.util.UUID;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

@Entity
@Table(name = "adrs")
public class AdrEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "analysis_id", nullable = false)
    private UUID analysisId;

    @Column(name = "tenant_id", nullable = false)
    private String tenantId;

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "context", nullable = false, columnDefinition = "TEXT")
    private String context;

    @Column(name = "decision", nullable = false, columnDefinition = "TEXT")
    private String decision;

    @Column(name = "consequences", columnDefinition = "TEXT")
    private String consequences;

    @Column(name = "status", nullable = false)
    private String status;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "related_findings", columnDefinition = "jsonb")
    private String relatedFindings;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public AdrEntity() {
    }

    @PrePersist
    void onPrePersist() {
        if (this.id == null) {
            this.id = UUID.randomUUID();
        }
        if (this.createdAt == null) {
            this.createdAt = Instant.now();
        }
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getAnalysisId() { return analysisId; }
    public void setAnalysisId(UUID analysisId) { this.analysisId = analysisId; }
    public String getTenantId() { return tenantId; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getContext() { return context; }
    public void setContext(String context) { this.context = context; }
    public String getDecision() { return decision; }
    public void setDecision(String decision) { this.decision = decision; }
    public String getConsequences() { return consequences; }
    public void setConsequences(String consequences) { this.consequences = consequences; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getRelatedFindings() { return relatedFindings; }
    public void setRelatedFindings(String relatedFindings) { this.relatedFindings = relatedFindings; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
