package dev.archlens.infrastructure.persistence.mapper;

import java.util.List;
import java.util.stream.Collectors;

import dev.archlens.domain.model.Analysis;
import dev.archlens.domain.model.AnalysisStatus;
import dev.archlens.domain.model.ArchitecturalRisk;
import dev.archlens.domain.model.RiskCategory;
import dev.archlens.domain.model.RiskSeverity;
import dev.archlens.infrastructure.persistence.entity.AnalysisEntity;
import dev.archlens.infrastructure.persistence.entity.ArchitecturalRiskEntity;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class AnalysisPersistenceMapper {

    public AnalysisEntity toEntity(Analysis domain) {
        AnalysisEntity entity = new AnalysisEntity();
        entity.setId(domain.getId());
        entity.setProjectId(domain.getProjectId());
        entity.setTenantId(domain.getTenantId());
        entity.setStatus(domain.getStatus() != null ? domain.getStatus().name() : null);
        entity.setSummary(domain.getSummary());
        entity.setCreatedAt(domain.getCreatedAt());
        entity.setUpdatedAt(domain.getUpdatedAt());

        if (domain.getRisks() != null) {
            List<ArchitecturalRiskEntity> riskEntities = domain.getRisks().stream()
                    .map(this::riskToEntity)
                    .collect(Collectors.toList());
            entity.setRisks(riskEntities);
        }

        return entity;
    }

    public Analysis toDomain(AnalysisEntity entity) {
        Analysis domain = new Analysis();
        domain.setId(entity.getId());
        domain.setProjectId(entity.getProjectId());
        domain.setTenantId(entity.getTenantId());
        domain.setStatus(entity.getStatus() != null ? AnalysisStatus.valueOf(entity.getStatus()) : null);
        domain.setSummary(entity.getSummary());
        domain.setCreatedAt(entity.getCreatedAt());
        domain.setUpdatedAt(entity.getUpdatedAt());

        if (entity.getRisks() != null) {
            List<ArchitecturalRisk> risks = entity.getRisks().stream()
                    .map(this::riskToDomain)
                    .collect(Collectors.toList());
            domain.setRisks(risks);
        }

        return domain;
    }

    public ArchitecturalRiskEntity riskToEntity(ArchitecturalRisk domain) {
        ArchitecturalRiskEntity entity = new ArchitecturalRiskEntity();
        entity.setId(domain.getId());
        entity.setTenantId(domain.getTenantId());
        entity.setCategory(domain.getCategory() != null ? domain.getCategory().name() : null);
        entity.setSeverity(domain.getSeverity() != null ? domain.getSeverity().name() : null);
        entity.setTitle(domain.getTitle());
        entity.setDescription(domain.getDescription());
        entity.setFilePath(domain.getFilePath());
        entity.setEvidence(domain.getEvidence());
        entity.setSuggestion(domain.getSuggestion());
        return entity;
    }

    public ArchitecturalRisk riskToDomain(ArchitecturalRiskEntity entity) {
        ArchitecturalRisk domain = new ArchitecturalRisk();
        domain.setId(entity.getId());
        domain.setAnalysisId(entity.getAnalysisId());
        domain.setTenantId(entity.getTenantId());
        domain.setCategory(entity.getCategory() != null ? RiskCategory.valueOf(entity.getCategory()) : null);
        domain.setSeverity(entity.getSeverity() != null ? RiskSeverity.valueOf(entity.getSeverity()) : null);
        domain.setTitle(entity.getTitle());
        domain.setDescription(entity.getDescription());
        domain.setFilePath(entity.getFilePath());
        domain.setEvidence(entity.getEvidence());
        domain.setSuggestion(entity.getSuggestion());
        return domain;
    }
}
