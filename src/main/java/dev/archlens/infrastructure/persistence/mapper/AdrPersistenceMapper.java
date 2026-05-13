package dev.archlens.infrastructure.persistence.mapper;

import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import dev.archlens.domain.model.Adr;
import dev.archlens.domain.model.AdrStatus;
import dev.archlens.infrastructure.persistence.entity.AdrEntity;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class AdrPersistenceMapper {

    private final ObjectMapper objectMapper;

    @Inject
    public AdrPersistenceMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public AdrEntity toEntity(Adr domain) {
        AdrEntity entity = new AdrEntity();
        entity.setId(domain.getId());
        entity.setAnalysisId(domain.getAnalysisId());
        entity.setTenantId(domain.getTenantId());
        entity.setTitle(domain.getTitle());
        entity.setContext(domain.getContext());
        entity.setDecision(domain.getDecision());
        entity.setConsequences(domain.getConsequences());
        entity.setStatus(domain.getStatus() != null ? domain.getStatus().name() : AdrStatus.PROPOSED.name());
        entity.setRelatedFindings(serializeUuids(domain.getRelatedFindings()));
        entity.setCreatedAt(domain.getCreatedAt());
        return entity;
    }

    public Adr toDomain(AdrEntity entity) {
        Adr domain = new Adr();
        domain.setId(entity.getId());
        domain.setAnalysisId(entity.getAnalysisId());
        domain.setTenantId(entity.getTenantId());
        domain.setTitle(entity.getTitle());
        domain.setContext(entity.getContext());
        domain.setDecision(entity.getDecision());
        domain.setConsequences(entity.getConsequences());
        domain.setStatus(entity.getStatus() != null ? AdrStatus.valueOf(entity.getStatus()) : null);
        domain.setRelatedFindings(deserializeUuids(entity.getRelatedFindings()));
        domain.setCreatedAt(entity.getCreatedAt());
        return domain;
    }

    private String serializeUuids(List<UUID> uuids) {
        if (uuids == null || uuids.isEmpty()) return null;
        try {
            return objectMapper.writeValueAsString(uuids);
        } catch (JsonProcessingException e) {
            return null;
        }
    }

    private List<UUID> deserializeUuids(String json) {
        if (json == null || json.isBlank()) return List.of();
        try {
            return objectMapper.readValue(json, new TypeReference<>() {});
        } catch (JsonProcessingException e) {
            return List.of();
        }
    }
}
