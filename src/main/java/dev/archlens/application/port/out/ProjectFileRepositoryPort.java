package dev.archlens.application.port.out;

import java.util.List;
import java.util.UUID;

import dev.archlens.domain.model.ProjectFile;

public interface ProjectFileRepositoryPort {

    ProjectFile save(ProjectFile file);

    List<ProjectFile> saveAll(List<ProjectFile> files);

    List<ProjectFile> findByProjectId(UUID projectId);

    void deleteByProjectId(UUID projectId);
}
