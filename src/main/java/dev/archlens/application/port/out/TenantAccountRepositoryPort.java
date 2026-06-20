package dev.archlens.application.port.out;

import java.util.Optional;

import dev.archlens.domain.model.TenantAccount;

public interface TenantAccountRepositoryPort {

    TenantAccount save(TenantAccount account);

    Optional<TenantAccount> findByTenantId(String tenantId);
}
