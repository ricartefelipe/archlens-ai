package dev.archlens.infrastructure.persistence.adapter;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import dev.archlens.application.port.out.ApiKeyRepositoryPort;
import dev.archlens.domain.model.ApiKeyRecord;
import dev.archlens.infrastructure.persistence.entity.ApiKeyEntity;
import dev.archlens.infrastructure.persistence.mapper.ApiKeyPersistenceMapper;
import dev.archlens.infrastructure.persistence.panache.ApiKeyPanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

@ApplicationScoped
public class ApiKeyRepositoryAdapter implements ApiKeyRepositoryPort {

    private final ApiKeyPanacheRepository repository;
    private final ApiKeyPersistenceMapper mapper;

    @Inject
    public ApiKeyRepositoryAdapter(ApiKeyPanacheRepository repository,
                                   ApiKeyPersistenceMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    @Transactional
    public ApiKeyRecord save(ApiKeyRecord record) {
        ApiKeyEntity entity = mapper.toEntity(record);
        Optional<ApiKeyEntity> existing = repository.findByIdOptional(record.getId());
        if (existing.isPresent()) {
            ApiKeyEntity current = existing.get();
            current.revokedAt = entity.revokedAt;
            current.lastUsedAt = entity.lastUsedAt;
            return mapper.toDomain(current);
        }
        repository.persist(entity);
        return mapper.toDomain(entity);
    }

    @Override
    public Optional<ApiKeyRecord> findById(UUID id) {
        return repository.findByIdOptional(id).map(mapper::toDomain);
    }

    @Override
    public Optional<ApiKeyRecord> findByIdAndTenantId(UUID id, String tenantId) {
        return repository.findByIdAndTenantId(id, tenantId).map(mapper::toDomain);
    }

    @Override
    public Optional<ApiKeyRecord> findByKeyHash(String keyHash) {
        return repository.findByKeyHash(keyHash).map(mapper::toDomain);
    }

    @Override
    public List<ApiKeyRecord> findAllByTenantId(String tenantId) {
        return repository.findAllByTenantId(tenantId).stream().map(mapper::toDomain).toList();
    }
}
