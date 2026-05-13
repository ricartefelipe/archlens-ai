package dev.archlens.domain.model;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public class Adr {

    private UUID id;
    private UUID analysisId;
    private String tenantId;
    private String title;
    private String context;
    private String decision;
    private String consequences;
    private AdrStatus status;
    private List<UUID> relatedFindings;
    private Instant createdAt;

    public Adr() {
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
    public AdrStatus getStatus() { return status; }
    public void setStatus(AdrStatus status) { this.status = status; }
    public List<UUID> getRelatedFindings() { return relatedFindings; }
    public void setRelatedFindings(List<UUID> relatedFindings) { this.relatedFindings = relatedFindings; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
