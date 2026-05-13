package dev.archlens.application.port.out;

import java.util.Optional;
import java.util.UUID;

import dev.archlens.domain.model.Question;

public interface QuestionRepositoryPort {

    Question save(Question question);

    Optional<Question> findById(UUID id);
}
