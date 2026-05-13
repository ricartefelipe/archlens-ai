package dev.archlens.infrastructure.persistence.panache;

import java.util.List;
import java.util.UUID;

import dev.archlens.infrastructure.persistence.entity.ProjectEntity;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class ProjectPanacheRepository implements PanacheRepositoryBase<ProjectEntity, UUID> {

    public List<ProjectEntity> findByTenantId(String tenantId) {
        return find("tenantId", tenantId).list();
    }
}
