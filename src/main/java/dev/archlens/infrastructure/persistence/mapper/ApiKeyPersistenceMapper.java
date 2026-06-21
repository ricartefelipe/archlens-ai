package dev.archlens.infrastructure.persistence.mapper;

import dev.archlens.domain.model.ApiKeyRecord;
import dev.archlens.infrastructure.persistence.entity.ApiKeyEntity;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class ApiKeyPersistenceMapper {

    public ApiKeyEntity toEntity(ApiKeyRecord record) {
        ApiKeyEntity entity = new ApiKeyEntity();
        entity.id = record.getId();
        entity.tenantId = record.getTenantId();
        entity.name = record.getName();
        entity.keyPrefix = record.getKeyPrefix();
        entity.keyHash = record.getKeyHash();
        entity.scopes = record.getScopes();
        entity.createdAt = record.getCreatedAt();
        entity.revokedAt = record.getRevokedAt();
        entity.lastUsedAt = record.getLastUsedAt();
        return entity;
    }

    public ApiKeyRecord toDomain(ApiKeyEntity entity) {
        ApiKeyRecord record = new ApiKeyRecord();
        record.setId(entity.id);
        record.setTenantId(entity.tenantId);
        record.setName(entity.name);
        record.setKeyPrefix(entity.keyPrefix);
        record.setKeyHash(entity.keyHash);
        record.setScopes(entity.scopes);
        record.setCreatedAt(entity.createdAt);
        record.setRevokedAt(entity.revokedAt);
        record.setLastUsedAt(entity.lastUsedAt);
        return record;
    }
}
