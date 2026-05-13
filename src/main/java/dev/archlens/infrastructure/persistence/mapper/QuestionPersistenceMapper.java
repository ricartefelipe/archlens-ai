package dev.archlens.infrastructure.persistence.mapper;

import dev.archlens.domain.model.Question;
import dev.archlens.infrastructure.persistence.entity.QuestionEntity;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class QuestionPersistenceMapper {

    public QuestionEntity toEntity(Question domain) {
        QuestionEntity entity = new QuestionEntity();
        entity.setId(domain.getId());
        entity.setAnalysisId(domain.getAnalysisId());
        entity.setTenantId(domain.getTenantId());
        entity.setQuestionText(domain.getQuestionText());
        entity.setAnswerText(domain.getAnswerText());
        entity.setSources(domain.getSources());
        entity.setCreatedAt(domain.getCreatedAt());
        return entity;
    }

    public Question toDomain(QuestionEntity entity) {
        Question domain = new Question();
        domain.setId(entity.getId());
        domain.setAnalysisId(entity.getAnalysisId());
        domain.setTenantId(entity.getTenantId());
        domain.setQuestionText(entity.getQuestionText());
        domain.setAnswerText(entity.getAnswerText());
        domain.setSources(entity.getSources());
        domain.setCreatedAt(entity.getCreatedAt());
        return domain;
    }
}
