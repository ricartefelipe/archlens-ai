package dev.archlens.infrastructure.persistence.panache;

import java.util.List;
import java.util.UUID;

import dev.archlens.infrastructure.persistence.entity.ProjectFileEntity;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class ProjectFilePanacheRepository implements PanacheRepositoryBase<ProjectFileEntity, UUID> {

    public List<ProjectFileEntity> findByProjectId(UUID projectId) {
        return find("projectId", projectId).list();
    }

    public long deleteByProjectId(UUID projectId) {
        return delete("projectId", projectId);
    }
}
