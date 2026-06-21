package dev.archlens.application.port.out;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import dev.archlens.domain.model.TenantWebhook;

public interface TenantWebhookRepositoryPort {

    TenantWebhook save(TenantWebhook webhook);

    Optional<TenantWebhook> findById(UUID id);

    Optional<TenantWebhook> findByIdAndTenantId(UUID id, String tenantId);

    List<TenantWebhook> findAllByTenantId(String tenantId);

    void deleteByIdAndTenantId(UUID id, String tenantId);
}
