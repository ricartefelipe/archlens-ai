package dev.archlens.infrastructure.persistence.adapter;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import dev.archlens.application.port.out.TenantWebhookRepositoryPort;
import dev.archlens.domain.model.TenantWebhook;
import dev.archlens.infrastructure.persistence.entity.TenantWebhookEntity;
import dev.archlens.infrastructure.persistence.mapper.TenantWebhookPersistenceMapper;
import dev.archlens.infrastructure.persistence.panache.TenantWebhookPanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

@ApplicationScoped
public class TenantWebhookRepositoryAdapter implements TenantWebhookRepositoryPort {

    private final TenantWebhookPanacheRepository repository;
    private final TenantWebhookPersistenceMapper mapper;

    @Inject
    public TenantWebhookRepositoryAdapter(TenantWebhookPanacheRepository repository,
                                          TenantWebhookPersistenceMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    @Transactional
    public TenantWebhook save(TenantWebhook webhook) {
        TenantWebhookEntity entity = mapper.toEntity(webhook);
        Optional<TenantWebhookEntity> existing = repository.findByIdOptional(webhook.getId());
        if (existing.isPresent()) {
            TenantWebhookEntity current = existing.get();
            current.url = entity.url;
            current.events = entity.events;
            current.enabled = entity.enabled;
            current.updatedAt = entity.updatedAt;
            return mapper.toDomain(current);
        }
        repository.persist(entity);
        return mapper.toDomain(entity);
    }

    @Override
    public Optional<TenantWebhook> findById(UUID id) {
        return repository.findByIdOptional(id).map(mapper::toDomain);
    }

    @Override
    public Optional<TenantWebhook> findByIdAndTenantId(UUID id, String tenantId) {
        return repository.findByIdAndTenantId(id, tenantId).map(mapper::toDomain);
    }

    @Override
    public List<TenantWebhook> findAllByTenantId(String tenantId) {
        return repository.findAllByTenantId(tenantId).stream().map(mapper::toDomain).toList();
    }

    @Override
    @Transactional
    public void deleteByIdAndTenantId(UUID id, String tenantId) {
        repository.deleteByIdAndTenantId(id, tenantId);
    }
}
