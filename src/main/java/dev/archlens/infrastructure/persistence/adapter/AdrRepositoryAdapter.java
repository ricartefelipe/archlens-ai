package dev.archlens.infrastructure.persistence.adapter;

import java.util.List;
import java.util.UUID;

import dev.archlens.application.port.out.AdrRepositoryPort;
import dev.archlens.domain.model.Adr;
import dev.archlens.infrastructure.persistence.mapper.AdrPersistenceMapper;
import dev.archlens.infrastructure.persistence.panache.AdrPanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

@ApplicationScoped
public class AdrRepositoryAdapter implements AdrRepositoryPort {

    private final AdrPanacheRepository repository;
    private final AdrPersistenceMapper mapper;

    @Inject
    public AdrRepositoryAdapter(AdrPanacheRepository repository, AdrPersistenceMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    @Transactional
    public Adr save(Adr adr) {
        var entity = mapper.toEntity(adr);
        repository.persist(entity);
        return mapper.toDomain(entity);
    }

    @Override
    @Transactional
    public List<Adr> saveAll(List<Adr> adrs) {
        var entities = adrs.stream().map(mapper::toEntity).toList();
        repository.persist(entities);
        return entities.stream().map(mapper::toDomain).toList();
    }

    @Override
    public List<Adr> findByAnalysisId(UUID analysisId) {
        return repository.findByAnalysisId(analysisId).stream()
                .map(mapper::toDomain)
                .toList();
    }
}
