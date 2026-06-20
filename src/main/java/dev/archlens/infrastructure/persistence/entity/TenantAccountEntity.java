package dev.archlens.infrastructure.persistence.entity;

import java.time.Instant;
import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "tenant_accounts")
public class TenantAccountEntity {

    @Id
    @Column(name = "tenant_id", length = 64)
    public String tenantId;

    @Column(name = "plan", nullable = false, length = 32)
    public String plan;

    @Column(name = "status", nullable = false, length = 16)
    public String status;

    @Column(name = "analyses_used_period", nullable = false)
    public int analysesUsedPeriod;

    @Column(name = "upload_bytes_period", nullable = false)
    public long uploadBytesPeriod;

    @Column(name = "usage_period_start", nullable = false)
    public LocalDate usagePeriodStart;

    @Column(name = "notes")
    public String notes;

    @Column(name = "created_at", nullable = false)
    public Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    public Instant updatedAt;
}
