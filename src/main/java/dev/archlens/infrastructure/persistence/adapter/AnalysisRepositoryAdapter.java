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
            // Sem isto, updated_at ficava congelado no valor do insert e não
            // refletia transições de status (PROCESSING, COMPLETED…).
            if (analysis.getUpdatedAt() != null) {
                existing.setUpdatedAt(analysis.getUpdatedAt());
            }

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

    @Override
    public List<Analysis> findInProgressOlderThan(java.time.Instant cutoff) {
        return repository.list("status in ('PENDING','PROCESSING') and updatedAt < ?1", cutoff)
                .stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public Optional<dev.archlens.domain.model.AnalysisStatus> findCurrentStatusFresh(UUID id) {
        // Query nativa escalar: não passa pelo cache de primeiro nível, então
        // enxerga um cancelamento commitado por outra transação.
        List<?> rows = repository.getEntityManager()
                .createNativeQuery("SELECT status FROM analyses WHERE id = :id")
                .setParameter("id", id)
                .getResultList();
        if (rows.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(dev.archlens.domain.model.AnalysisStatus.valueOf(String.valueOf(rows.get(0))));
    }

    @Override
    @Transactional
    public Optional<Analysis> cancelIfInProgress(UUID id, java.time.Instant when, String summary) {
        // Lock pessimista na linha: serializa a corrida com o consumidor, que
        // concluiria a mesma análise. Quem pega o lock primeiro decide, e o
        // outro lê o estado já gravado em vez de sobrescrevê-lo.
        AnalysisEntity entity = repository.getEntityManager()
                .find(AnalysisEntity.class, id, jakarta.persistence.LockModeType.PESSIMISTIC_WRITE);
        if (entity == null) {
            return Optional.empty();
        }

        dev.archlens.domain.model.AnalysisStatus current =
                dev.archlens.domain.model.AnalysisStatus.valueOf(entity.getStatus());
        if (current != dev.archlens.domain.model.AnalysisStatus.PENDING
                && current != dev.archlens.domain.model.AnalysisStatus.PROCESSING) {
            return Optional.empty();
        }

        entity.setStatus(dev.archlens.domain.model.AnalysisStatus.CANCELLED.name());
        entity.setSummary(summary);
        entity.setUpdatedAt(when);
        repository.getEntityManager().flush();
        return Optional.of(mapper.toDomain(entity));
    }

    @Override
    public List<Analysis> findByProjectIdAndTenantId(UUID projectId, String tenantId) {
        return repository.list("projectId = ?1 and tenantId = ?2 order by createdAt desc", projectId, tenantId)
                .stream()
                .map(mapper::toDomain)
                .toList();
    }
}
