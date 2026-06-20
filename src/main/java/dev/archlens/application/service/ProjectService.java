package dev.archlens.application.service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import dev.archlens.application.port.in.CreateProjectUseCase;
import dev.archlens.application.port.in.GetProjectUseCase;
import dev.archlens.application.port.in.ListProjectsUseCase;
import dev.archlens.application.port.out.ProjectRepositoryPort;
import dev.archlens.application.port.out.TenantProvider;
import dev.archlens.domain.exception.ProjectNotFoundException;
import dev.archlens.domain.model.Project;
import dev.archlens.domain.model.ProjectStatus;

@ApplicationScoped
public class ProjectService implements CreateProjectUseCase, ListProjectsUseCase, GetProjectUseCase {

    private final ProjectRepositoryPort projectRepository;
    private final TenantProvider tenantProvider;

    public ProjectService(ProjectRepositoryPort projectRepository, TenantProvider tenantProvider) {
        this.projectRepository = projectRepository;
        this.tenantProvider = tenantProvider;
    }

    @Override
    @Transactional
    public Project create(String name, String description) {
        String tenantId = tenantProvider.getCurrentTenantId();

        Project project = new Project();
        project.setId(UUID.randomUUID());
        project.setTenantId(tenantId);
        project.setName(name);
        project.setDescription(description);
        project.setStatus(ProjectStatus.CREATED);
        project.setCreatedAt(Instant.now());
        project.setUpdatedAt(Instant.now());

        return projectRepository.save(project);
    }

    @Override
    public List<Project> listAll() {
        String tenantId = tenantProvider.getCurrentTenantId();
        return projectRepository.findAllByTenantId(tenantId);
    }

    @Override
    public Project getById(UUID id) {
        String tenantId = tenantProvider.getCurrentTenantId();
        return projectRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ProjectNotFoundException(id));
    }
}
