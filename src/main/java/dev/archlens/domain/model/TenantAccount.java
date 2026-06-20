package dev.archlens.domain.model;

import java.time.Instant;
import java.time.LocalDate;

public class TenantAccount {

    private String tenantId;
    private CommercialPlan plan;
    private TenantAccountStatus status;
    private int analysesUsedPeriod;
    private long uploadBytesPeriod;
    private LocalDate usagePeriodStart;
    private String notes;
    private Instant createdAt;
    private Instant updatedAt;

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public CommercialPlan getPlan() {
        return plan;
    }

    public void setPlan(CommercialPlan plan) {
        this.plan = plan;
    }

    public TenantAccountStatus getStatus() {
        return status;
    }

    public void setStatus(TenantAccountStatus status) {
        this.status = status;
    }

    public int getAnalysesUsedPeriod() {
        return analysesUsedPeriod;
    }

    public void setAnalysesUsedPeriod(int analysesUsedPeriod) {
        this.analysesUsedPeriod = analysesUsedPeriod;
    }

    public long getUploadBytesPeriod() {
        return uploadBytesPeriod;
    }

    public void setUploadBytesPeriod(long uploadBytesPeriod) {
        this.uploadBytesPeriod = uploadBytesPeriod;
    }

    public LocalDate getUsagePeriodStart() {
        return usagePeriodStart;
    }

    public void setUsagePeriodStart(LocalDate usagePeriodStart) {
        this.usagePeriodStart = usagePeriodStart;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
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
