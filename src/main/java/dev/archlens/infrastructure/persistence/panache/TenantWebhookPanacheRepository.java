package dev.archlens.infrastructure.persistence.panache;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import dev.archlens.infrastructure.persistence.entity.TenantWebhookEntity;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class TenantWebhookPanacheRepository implements PanacheRepositoryBase<TenantWebhookEntity, UUID> {

    public List<TenantWebhookEntity> findAllByTenantId(String tenantId) {
        return list("tenantId", tenantId);
    }

    public Optional<TenantWebhookEntity> findByIdAndTenantId(UUID id, String tenantId) {
        return find("id = ?1 and tenantId = ?2", id, tenantId).firstResultOptional();
    }

    public void deleteByIdAndTenantId(UUID id, String tenantId) {
        delete("id = ?1 and tenantId = ?2", id, tenantId);
    }
}
