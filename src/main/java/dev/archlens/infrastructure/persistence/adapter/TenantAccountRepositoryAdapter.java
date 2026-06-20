package dev.archlens.infrastructure.persistence.adapter;

import java.util.Optional;

import dev.archlens.application.port.out.TenantAccountRepositoryPort;
import dev.archlens.domain.model.TenantAccount;
import dev.archlens.infrastructure.persistence.entity.TenantAccountEntity;
import dev.archlens.infrastructure.persistence.mapper.TenantAccountPersistenceMapper;
import dev.archlens.infrastructure.persistence.panache.TenantAccountPanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

@ApplicationScoped
public class TenantAccountRepositoryAdapter implements TenantAccountRepositoryPort {

    private final TenantAccountPanacheRepository repository;
    private final TenantAccountPersistenceMapper mapper;

    @Inject
    public TenantAccountRepositoryAdapter(TenantAccountPanacheRepository repository,
                                          TenantAccountPersistenceMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    @Transactional
    public TenantAccount save(TenantAccount account) {
        TenantAccountEntity entity = mapper.toEntity(account);
        Optional<TenantAccountEntity> existing = repository.findByIdOptional(account.getTenantId());
        if (existing.isPresent()) {
            TenantAccountEntity current = existing.get();
            current.plan = entity.plan;
            current.status = entity.status;
            current.analysesUsedPeriod = entity.analysesUsedPeriod;
            current.uploadBytesPeriod = entity.uploadBytesPeriod;
            current.usagePeriodStart = entity.usagePeriodStart;
            current.notes = entity.notes;
            current.updatedAt = entity.updatedAt;
            return mapper.toDomain(current);
        }
        repository.persist(entity);
        return mapper.toDomain(entity);
    }

    @Override
    public Optional<TenantAccount> findByTenantId(String tenantId) {
        return repository.findByIdOptional(tenantId).map(mapper::toDomain);
    }
}
