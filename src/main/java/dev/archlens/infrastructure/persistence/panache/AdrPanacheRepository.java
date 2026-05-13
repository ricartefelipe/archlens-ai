package dev.archlens.infrastructure.persistence.panache;

import java.util.List;
import java.util.UUID;

import dev.archlens.infrastructure.persistence.entity.AdrEntity;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class AdrPanacheRepository implements PanacheRepositoryBase<AdrEntity, UUID> {

    public List<AdrEntity> findByAnalysisId(UUID analysisId) {
        return find("analysisId", analysisId).list();
    }
}
