package dev.archlens.application.service;

import java.util.List;
import java.util.UUID;

import dev.archlens.application.port.in.ListProjectFilesUseCase;
import dev.archlens.application.port.out.ProjectFileRepositoryPort;
import dev.archlens.application.port.out.ProjectRepositoryPort;
import dev.archlens.application.port.out.TenantProvider;
import dev.archlens.domain.exception.ProjectNotFoundException;
import dev.archlens.domain.model.ProjectFile;
import dev.archlens.infrastructure.persistence.rls.TenantScopedRls;
import jakarta.enterprise.context.ApplicationScoped;

@TenantScopedRls
@ApplicationScoped
public class ProjectFileService implements ListProjectFilesUseCase {

    private final ProjectFileRepositoryPort fileRepository;
    private final ProjectRepositoryPort projectRepository;
    private final TenantProvider tenantProvider;

    public ProjectFileService(ProjectFileRepositoryPort fileRepository,
                              ProjectRepositoryPort projectRepository,
                              TenantProvider tenantProvider) {
        this.fileRepository = fileRepository;
        this.projectRepository = projectRepository;
        this.tenantProvider = tenantProvider;
    }

    @Override
    public List<ProjectFile> listByProjectId(UUID projectId) {
        String tenantId = tenantProvider.getCurrentTenantId();
        if (!projectRepository.existsByIdAndTenantId(projectId, tenantId)) {
            throw new ProjectNotFoundException(projectId);
        }
        return fileRepository.findByProjectId(projectId).stream()
                .filter(f -> tenantId.equals(f.getTenantId()))
                .toList();
    }
}
