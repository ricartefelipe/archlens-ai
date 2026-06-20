package dev.archlens.infrastructure.persistence.mapper;

import dev.archlens.domain.model.CommercialPlan;
import dev.archlens.domain.model.TenantAccount;
import dev.archlens.domain.model.TenantAccountStatus;
import dev.archlens.infrastructure.persistence.entity.TenantAccountEntity;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class TenantAccountPersistenceMapper {

    public TenantAccountEntity toEntity(TenantAccount account) {
        TenantAccountEntity entity = new TenantAccountEntity();
        entity.tenantId = account.getTenantId();
        entity.plan = account.getPlan().name();
        entity.status = account.getStatus().name();
        entity.analysesUsedPeriod = account.getAnalysesUsedPeriod();
        entity.uploadBytesPeriod = account.getUploadBytesPeriod();
        entity.usagePeriodStart = account.getUsagePeriodStart();
        entity.notes = account.getNotes();
        entity.createdAt = account.getCreatedAt();
        entity.updatedAt = account.getUpdatedAt();
        return entity;
    }

    public TenantAccount toDomain(TenantAccountEntity entity) {
        TenantAccount account = new TenantAccount();
        account.setTenantId(entity.tenantId);
        account.setPlan(CommercialPlan.valueOf(entity.plan));
        account.setStatus(TenantAccountStatus.valueOf(entity.status));
        account.setAnalysesUsedPeriod(entity.analysesUsedPeriod);
        account.setUploadBytesPeriod(entity.uploadBytesPeriod);
        account.setUsagePeriodStart(entity.usagePeriodStart);
        account.setNotes(entity.notes);
        account.setCreatedAt(entity.createdAt);
        account.setUpdatedAt(entity.updatedAt);
        return account;
    }
}
