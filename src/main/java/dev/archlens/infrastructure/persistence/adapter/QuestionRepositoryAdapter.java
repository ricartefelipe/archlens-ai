package dev.archlens.infrastructure.persistence.adapter;

import java.util.Optional;
import java.util.UUID;

import dev.archlens.application.port.out.QuestionRepositoryPort;
import dev.archlens.domain.model.Question;
import dev.archlens.infrastructure.persistence.entity.QuestionEntity;
import dev.archlens.infrastructure.persistence.mapper.QuestionPersistenceMapper;
import dev.archlens.infrastructure.persistence.panache.QuestionPanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

@ApplicationScoped
public class QuestionRepositoryAdapter implements QuestionRepositoryPort {

    private final QuestionPanacheRepository repository;
    private final QuestionPersistenceMapper mapper;

    @Inject
    public QuestionRepositoryAdapter(QuestionPanacheRepository repository, QuestionPersistenceMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    @Transactional
    public Question save(Question question) {
        QuestionEntity entity = mapper.toEntity(question);
        repository.persist(entity);
        return mapper.toDomain(entity);
    }

    @Override
    public Optional<Question> findById(UUID id) {
        return repository.findByIdOptional(id)
                .map(mapper::toDomain);
    }
}
