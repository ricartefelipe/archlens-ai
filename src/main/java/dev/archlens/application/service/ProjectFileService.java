package dev.archlens.application.service;

import java.util.List;
import java.util.UUID;

import dev.archlens.application.port.in.ListProjectFilesUseCase;
import dev.archlens.application.port.out.ProjectFileRepositoryPort;
import dev.archlens.domain.model.ProjectFile;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class ProjectFileService implements ListProjectFilesUseCase {

    private final ProjectFileRepositoryPort fileRepository;

    public ProjectFileService(ProjectFileRepositoryPort fileRepository) {
        this.fileRepository = fileRepository;
    }

    @Override
    public List<ProjectFile> listByProjectId(UUID projectId) {
        return fileRepository.findByProjectId(projectId);
    }
}
