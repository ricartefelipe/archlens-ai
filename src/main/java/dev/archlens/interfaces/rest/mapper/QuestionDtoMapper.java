package dev.archlens.interfaces.rest.mapper;

import dev.archlens.domain.model.Question;
import dev.archlens.interfaces.rest.dto.response.QuestionResponse;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class QuestionDtoMapper {

    public QuestionResponse toResponse(Question domain) {
        return new QuestionResponse(
                domain.getId(),
                domain.getAnalysisId(),
                domain.getQuestionText(),
                domain.getAnswerText(),
                domain.getSources(),
                domain.getCreatedAt());
    }
}
