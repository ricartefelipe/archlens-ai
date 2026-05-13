package dev.archlens.infrastructure.persistence.adapter;

import java.util.List;
import java.util.UUID;

import dev.archlens.application.port.out.ProjectFileRepositoryPort;
import dev.archlens.domain.model.ProjectFile;
import dev.archlens.infrastructure.persistence.mapper.ProjectFilePersistenceMapper;
import dev.archlens.infrastructure.persistence.panache.ProjectFilePanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

@ApplicationScoped
public class ProjectFileRepositoryAdapter implements ProjectFileRepositoryPort {

    private final ProjectFilePanacheRepository repository;
    private final ProjectFilePersistenceMapper mapper;

    @Inject
    public ProjectFileRepositoryAdapter(ProjectFilePanacheRepository repository,
                                         ProjectFilePersistenceMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    @Transactional
    public ProjectFile save(ProjectFile file) {
        var entity = mapper.toEntity(file);
        repository.persist(entity);
        return mapper.toDomain(entity);
    }

    @Override
    @Transactional
    public List<ProjectFile> saveAll(List<ProjectFile> files) {
        var entities = files.stream().map(mapper::toEntity).toList();
        repository.persist(entities);
        return entities.stream().map(mapper::toDomain).toList();
    }

    @Override
    public List<ProjectFile> findByProjectId(UUID projectId) {
        return repository.findByProjectId(projectId).stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    @Transactional
    public void deleteByProjectId(UUID projectId) {
        repository.deleteByProjectId(projectId);
    }
}
