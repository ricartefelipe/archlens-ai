package dev.archlens.infrastructure.persistence.mapper;

import dev.archlens.domain.model.FileType;
import dev.archlens.domain.model.ProjectFile;
import dev.archlens.infrastructure.persistence.entity.ProjectFileEntity;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class ProjectFilePersistenceMapper {

    public ProjectFileEntity toEntity(ProjectFile domain) {
        ProjectFileEntity entity = new ProjectFileEntity();
        entity.setId(domain.getId());
        entity.setProjectId(domain.getProjectId());
        entity.setTenantId(domain.getTenantId());
        entity.setFilePath(domain.getFilePath());
        entity.setFileType(domain.getFileType() != null ? domain.getFileType().name() : null);
        entity.setSizeBytes(domain.getSizeBytes());
        entity.setContentHash(domain.getContentHash());
        entity.setCreatedAt(domain.getCreatedAt());
        return entity;
    }

    public ProjectFile toDomain(ProjectFileEntity entity) {
        ProjectFile domain = new ProjectFile();
        domain.setId(entity.getId());
        domain.setProjectId(entity.getProjectId());
        domain.setTenantId(entity.getTenantId());
        domain.setFilePath(entity.getFilePath());
        domain.setFileType(entity.getFileType() != null ? FileType.valueOf(entity.getFileType()) : null);
        domain.setSizeBytes(entity.getSizeBytes());
        domain.setContentHash(entity.getContentHash());
        domain.setCreatedAt(entity.getCreatedAt());
        return domain;
    }
}
