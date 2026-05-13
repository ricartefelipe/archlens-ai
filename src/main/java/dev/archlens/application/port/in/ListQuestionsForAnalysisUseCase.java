package dev.archlens.application.port.in;

import java.util.List;
import java.util.UUID;

import dev.archlens.domain.model.Question;

public interface ListQuestionsForAnalysisUseCase {

    List<Question> list(UUID projectId, UUID analysisId);
}
