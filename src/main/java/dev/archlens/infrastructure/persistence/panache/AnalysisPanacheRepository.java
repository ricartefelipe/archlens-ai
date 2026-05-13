package dev.archlens.infrastructure.persistence.panache;

import java.util.Optional;
import java.util.UUID;

import dev.archlens.infrastructure.persistence.entity.AnalysisEntity;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class AnalysisPanacheRepository implements PanacheRepositoryBase<AnalysisEntity, UUID> {

    public Optional<AnalysisEntity> findByProjectIdAndId(UUID projectId, UUID id) {
        return find("projectId = ?1 and id = ?2", projectId, id).firstResultOptional();
    }
}
