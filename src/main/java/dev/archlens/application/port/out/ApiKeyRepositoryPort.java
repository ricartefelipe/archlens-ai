package dev.archlens.application.port.out;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import dev.archlens.domain.model.ApiKeyRecord;

public interface ApiKeyRepositoryPort {

    ApiKeyRecord save(ApiKeyRecord record);

    Optional<ApiKeyRecord> findById(UUID id);

    Optional<ApiKeyRecord> findByIdAndTenantId(UUID id, String tenantId);

    Optional<ApiKeyRecord> findByKeyHash(String keyHash);

    List<ApiKeyRecord> findAllByTenantId(String tenantId);
}
