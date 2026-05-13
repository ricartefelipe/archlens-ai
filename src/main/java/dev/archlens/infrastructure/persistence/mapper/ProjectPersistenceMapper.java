package dev.archlens.infrastructure.persistence.mapper;

import dev.archlens.domain.model.Project;
import dev.archlens.infrastructure.persistence.entity.ProjectEntity;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class ProjectPersistenceMapper {

    public ProjectEntity toEntity(Project domain) {
        ProjectEntity entity = new ProjectEntity();
        entity.setId(domain.getId());
        entity.setTenantId(domain.getTenantId());
        entity.setName(domain.getName());
        entity.setDescription(domain.getDescription());
        entity.setCreatedAt(domain.getCreatedAt());
        entity.setUpdatedAt(domain.getUpdatedAt());
        return entity;
    }

    public Project toDomain(ProjectEntity entity) {
        Project domain = new Project();
        domain.setId(entity.getId());
        domain.setTenantId(entity.getTenantId());
        domain.setName(entity.getName());
        domain.setDescription(entity.getDescription());
        domain.setCreatedAt(entity.getCreatedAt());
        domain.setUpdatedAt(entity.getUpdatedAt());
        return domain;
    }
}
