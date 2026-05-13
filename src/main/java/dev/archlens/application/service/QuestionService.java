package dev.archlens.application.service;

import java.time.Instant;
import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import org.jboss.logging.Logger;

import dev.archlens.application.port.in.AskQuestionUseCase;
import dev.archlens.application.port.out.AnalysisRepositoryPort;
import dev.archlens.application.port.out.LlmGateway;
import dev.archlens.application.port.out.QuestionRepositoryPort;
import dev.archlens.application.port.out.TenantProvider;
import dev.archlens.domain.exception.AnalysisNotFoundException;
import dev.archlens.domain.model.Analysis;
import dev.archlens.domain.model.Question;

@ApplicationScoped
public class QuestionService implements AskQuestionUseCase {

    private static final Logger LOG = Logger.getLogger(QuestionService.class);

    private final QuestionRepositoryPort questionRepository;
    private final AnalysisRepositoryPort analysisRepository;
    private final LlmGateway llmGateway;
    private final TenantProvider tenantProvider;

    public QuestionService(QuestionRepositoryPort questionRepository,
                           AnalysisRepositoryPort analysisRepository,
                           LlmGateway llmGateway,
                           TenantProvider tenantProvider) {
        this.questionRepository = questionRepository;
        this.analysisRepository = analysisRepository;
        this.llmGateway = llmGateway;
        this.tenantProvider = tenantProvider;
    }

    @Override
    @Transactional
    public Question ask(UUID analysisId, String questionText) {
        String tenantId = tenantProvider.getCurrentTenantId();

        Analysis analysis = analysisRepository.findById(analysisId)
                .orElseThrow(() -> new AnalysisNotFoundException(analysisId));

        LOG.infof("Processing question for analysis %s: %s", analysisId, questionText);

        Question question = new Question();
        question.setId(UUID.randomUUID());
        question.setAnalysisId(analysisId);
        question.setTenantId(tenantId);
        question.setQuestionText(questionText);
        question.setCreatedAt(Instant.now());

        String answer = llmGateway.answerQuestion(questionText, analysis.getSummary());
        question.setAnswerText(answer);

        LOG.infof("Answer generated for question %s on analysis %s", question.getId(), analysisId);

        return questionRepository.save(question);
    }
}
