package dev.archlens.infrastructure.persistence.adapter;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import dev.archlens.application.port.out.ProjectRepositoryPort;
import dev.archlens.domain.model.Project;
import dev.archlens.infrastructure.persistence.entity.ProjectEntity;
import dev.archlens.infrastructure.persistence.mapper.ProjectPersistenceMapper;
import dev.archlens.infrastructure.persistence.panache.ProjectPanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

@ApplicationScoped
public class ProjectRepositoryAdapter implements ProjectRepositoryPort {

    private final ProjectPanacheRepository repository;
    private final ProjectPersistenceMapper mapper;

    @Inject
    public ProjectRepositoryAdapter(ProjectPanacheRepository repository, ProjectPersistenceMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    @Transactional
    public Project save(Project project) {
        if (project.getId() != null) {
            Optional<ProjectEntity> existing = repository.findByIdOptional(project.getId());
            if (existing.isPresent()) {
                ProjectEntity entity = existing.get();
                entity.setName(project.getName());
                entity.setDescription(project.getDescription());
                entity.setTenantId(project.getTenantId());
                entity.setStatus(project.getStatus() != null ? project.getStatus().name() : entity.getStatus());
                entity.setFileCount(project.getFileCount());
                return mapper.toDomain(entity);
            }
        }
        ProjectEntity entity = mapper.toEntity(project);
        repository.persist(entity);
        return mapper.toDomain(entity);
    }

    @Override
    public Optional<Project> findById(UUID id) {
        return repository.findByIdOptional(id)
                .map(mapper::toDomain);
    }

    @Override
    public List<Project> findAllByTenantId(String tenantId) {
        return repository.findByTenantId(tenantId).stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public boolean existsById(UUID id) {
        return repository.findByIdOptional(id).isPresent();
    }
}
