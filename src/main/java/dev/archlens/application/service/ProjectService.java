package dev.archlens.application.service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import dev.archlens.application.port.in.CreateProjectUseCase;
import dev.archlens.application.port.in.DeleteProjectUseCase;
import dev.archlens.application.port.in.GetProjectUseCase;
import dev.archlens.application.port.in.ListProjectsUseCase;
import dev.archlens.application.port.out.FileStoragePort;
import dev.archlens.application.port.out.IngestGateway;
import dev.archlens.application.port.out.ProjectRepositoryPort;
import dev.archlens.application.port.out.TenantProvider;
import dev.archlens.domain.exception.ProjectNotFoundException;
import dev.archlens.domain.model.Project;
import dev.archlens.domain.model.ProjectStatus;
import dev.archlens.infrastructure.persistence.rls.TenantScopedRls;

@TenantScopedRls
@ApplicationScoped
public class ProjectService implements CreateProjectUseCase, ListProjectsUseCase, GetProjectUseCase,
        DeleteProjectUseCase {

    private final ProjectRepositoryPort projectRepository;
    private final TenantProvider tenantProvider;
    private final QuotaService quotaService;
    private final IngestGateway ingestGateway;
    private final FileStoragePort fileStorage;

    public ProjectService(ProjectRepositoryPort projectRepository,
                          TenantProvider tenantProvider,
                          QuotaService quotaService,
                          IngestGateway ingestGateway,
                          FileStoragePort fileStorage) {
        this.projectRepository = projectRepository;
        this.tenantProvider = tenantProvider;
        this.quotaService = quotaService;
        this.ingestGateway = ingestGateway;
        this.fileStorage = fileStorage;
    }

    @Override
    @Transactional
    public Project create(String name, String description) {
        String tenantId = tenantProvider.getCurrentTenantId();
        quotaService.checkCanCreateProject(tenantId);

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

    /**
     * Erasure completo do projeto: índice/vetores no worker, arquivos extraídos
     * no storage e, por fim, as linhas relacionais (cascata leva análises,
     * riscos, ADRs, perguntas e arquivos).
     *
     * A ordem importa: o worker vem primeiro porque, se a remoção do banco
     * acontecesse antes e o worker falhasse, os embeddings ficariam órfãos sem
     * nenhum dono capaz de apagá-los depois. Uma falha no meio deixa o projeto
     * visível e a operação pode ser repetida — cada passo é idempotente.
     */
    @Override
    public void delete(UUID projectId) {
        String tenantId = tenantProvider.getCurrentTenantId();
        if (!projectRepository.existsByIdAndTenantId(projectId, tenantId)) {
            throw new ProjectNotFoundException(projectId);
        }

        ingestGateway.deleteProjectData(projectId);
        fileStorage.deleteProjectFiles(projectId);
        projectRepository.deleteById(projectId);
    }
}
