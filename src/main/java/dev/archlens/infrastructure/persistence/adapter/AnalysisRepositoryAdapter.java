package dev.archlens.infrastructure.persistence.adapter;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import dev.archlens.application.port.out.AnalysisRepositoryPort;
import dev.archlens.domain.model.Analysis;
import dev.archlens.infrastructure.persistence.entity.AnalysisEntity;
import dev.archlens.infrastructure.persistence.entity.ArchitecturalRiskEntity;
import dev.archlens.infrastructure.persistence.mapper.AnalysisPersistenceMapper;
import dev.archlens.infrastructure.persistence.panache.AnalysisPanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

@ApplicationScoped
public class AnalysisRepositoryAdapter implements AnalysisRepositoryPort {

    private final AnalysisPanacheRepository repository;
    private final AnalysisPersistenceMapper mapper;

    @Inject
    public AnalysisRepositoryAdapter(AnalysisPanacheRepository repository, AnalysisPersistenceMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    @Transactional
    public Analysis save(Analysis analysis) {
        if (analysis.getId() != null && repository.findByIdOptional(analysis.getId()).isPresent()) {
            AnalysisEntity existing = repository.findByIdOptional(analysis.getId()).get();
            existing.setProjectId(analysis.getProjectId());
            existing.setTenantId(analysis.getTenantId());
            existing.setStatus(analysis.getStatus() != null ? analysis.getStatus().name() : null);
            existing.setSummary(analysis.getSummary());

            existing.getRisks().clear();
            if (analysis.getRisks() != null) {
                List<ArchitecturalRiskEntity> riskEntities = analysis.getRisks().stream()
                        .map(mapper::riskToEntity)
                        .collect(Collectors.toList());
                existing.getRisks().addAll(riskEntities);
            }

            return mapper.toDomain(existing);
        }

        AnalysisEntity entity = mapper.toEntity(analysis);
        repository.persist(entity);
        return mapper.toDomain(entity);
    }

    @Override
    public Optional<Analysis> findById(UUID id) {
        return repository.findByIdOptional(id)
                .map(mapper::toDomain);
    }

    @Override
    public Optional<Analysis> findByProjectIdAndId(UUID projectId, UUID analysisId) {
        return repository.findByProjectIdAndId(projectId, analysisId)
                .map(mapper::toDomain);
    }
}
