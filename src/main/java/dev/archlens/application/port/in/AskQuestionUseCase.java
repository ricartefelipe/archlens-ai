package dev.archlens.application.port.in;

import java.util.UUID;

import dev.archlens.domain.model.Question;

public interface AskQuestionUseCase {

    Question ask(UUID analysisId, String questionText);
}
