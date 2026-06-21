package dev.archlens.application.service;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import dev.archlens.application.port.out.ApiKeyRepositoryPort;
import dev.archlens.domain.exception.ApiKeyNotFoundException;
import dev.archlens.domain.model.ApiKeyRecord;
import dev.archlens.infrastructure.persistence.rls.TenantScopedRls;
import dev.archlens.infrastructure.security.SecureTokenHasher;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

@TenantScopedRls
@ApplicationScoped
public class ApiKeyService {

    private static final String KEY_PREFIX_LABEL = "alk";

    private final ApiKeyRepositoryPort repository;
    private final SecureTokenHasher tokenHasher;

    @Inject
    public ApiKeyService(ApiKeyRepositoryPort repository, SecureTokenHasher tokenHasher) {
        this.repository = repository;
        this.tokenHasher = tokenHasher;
    }

    public record CreatedApiKey(ApiKeyRecord record, String plainKey) {
    }

    @Transactional
    public CreatedApiKey create(String tenantId, String name, String scopes) {
        String prefix = tokenHasher.randomToken(4);
        String secret = tokenHasher.randomToken(16);
        String plainKey = KEY_PREFIX_LABEL + "_" + prefix + "_" + secret;
        String keyHash = tokenHasher.sha256(plainKey);

        Instant now = Instant.now();
        ApiKeyRecord record = new ApiKeyRecord();
        record.setId(UUID.randomUUID());
        record.setTenantId(tenantId);
        record.setName(name);
        record.setKeyPrefix(prefix);
        record.setKeyHash(keyHash);
        record.setScopes(normalizeScopes(scopes));
        record.setCreatedAt(now);

        ApiKeyRecord saved = repository.save(record);
        return new CreatedApiKey(saved, plainKey);
    }

    public List<ApiKeyRecord> list(String tenantId) {
        return repository.findAllByTenantId(tenantId);
    }

    @Transactional
    public void revoke(String tenantId, UUID keyId) {
        ApiKeyRecord record = repository.findByIdAndTenantId(keyId, tenantId)
                .orElseThrow(() -> new ApiKeyNotFoundException(keyId));
        if (!record.isRevoked()) {
            record.setRevokedAt(Instant.now());
            repository.save(record);
        }
    }

    @Transactional
    public Optional<ApiKeyRecord> authenticate(String plainKey) {
        if (plainKey == null || plainKey.isBlank()) {
            return Optional.empty();
        }
        String keyHash = tokenHasher.sha256(plainKey);
        Optional<ApiKeyRecord> record = repository.findByKeyHash(keyHash);
        if (record.isEmpty() || record.get().isRevoked()) {
            return Optional.empty();
        }
        ApiKeyRecord active = record.get();
        active.setLastUsedAt(Instant.now());
        repository.save(active);
        return Optional.of(active);
    }

    private static String normalizeScopes(String scopes) {
        if (scopes == null || scopes.isBlank()) {
            return "read";
        }
        return scopes.trim().toLowerCase();
    }
}
