package dev.archlens.infrastructure.persistence.panache;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import dev.archlens.infrastructure.persistence.entity.ApiKeyEntity;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class ApiKeyPanacheRepository implements PanacheRepositoryBase<ApiKeyEntity, UUID> {

    public Optional<ApiKeyEntity> findByKeyHash(String keyHash) {
        return find("keyHash", keyHash).firstResultOptional();
    }

    public List<ApiKeyEntity> findAllByTenantId(String tenantId) {
        return list("tenantId", tenantId);
    }

    public Optional<ApiKeyEntity> findByIdAndTenantId(UUID id, String tenantId) {
        return find("id = ?1 and tenantId = ?2", id, tenantId).firstResultOptional();
    }
}
