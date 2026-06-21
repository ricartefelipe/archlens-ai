package dev.archlens.application.service;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import org.jboss.logging.Logger;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import dev.archlens.application.port.in.AskQuestionUseCase;
import dev.archlens.application.port.in.ListQuestionsForAnalysisUseCase;
import dev.archlens.application.port.out.AnalysisRepositoryPort;
import dev.archlens.application.port.out.LlmGateway;
import dev.archlens.application.port.out.QuestionRepositoryPort;
import dev.archlens.application.port.out.RagContextPort;
import dev.archlens.application.port.out.TenantProvider;
import dev.archlens.domain.exception.AnalysisNotFoundException;
import dev.archlens.domain.model.Analysis;
import dev.archlens.domain.model.ArchitecturalRisk;
import dev.archlens.domain.model.Question;
import dev.archlens.domain.model.RiskSeverity;
import dev.archlens.infrastructure.persistence.rls.TenantScopedRls;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

@TenantScopedRls
@ApplicationScoped
public class QuestionService implements AskQuestionUseCase, ListQuestionsForAnalysisUseCase {

    private static final Logger LOG = Logger.getLogger(QuestionService.class);
    private static final int RAG_MAX_CHUNKS = 5;

    private final QuestionRepositoryPort questionRepository;
    private final AnalysisRepositoryPort analysisRepository;
    private final LlmGateway llmGateway;
    private final RagContextPort ragContextPort;
    private final TenantProvider tenantProvider;
    private final ObjectMapper objectMapper;

    public QuestionService(QuestionRepositoryPort questionRepository,
                           AnalysisRepositoryPort analysisRepository,
                           LlmGateway llmGateway,
                           RagContextPort ragContextPort,
                           TenantProvider tenantProvider,
                           ObjectMapper objectMapper) {
        this.questionRepository = questionRepository;
        this.analysisRepository = analysisRepository;
        this.llmGateway = llmGateway;
        this.ragContextPort = ragContextPort;
        this.tenantProvider = tenantProvider;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional
    public Question ask(UUID projectId, UUID analysisId, String questionText) {
        String tenantId = tenantProvider.getCurrentTenantId();

        Analysis analysis = analysisRepository.findByProjectIdAndId(projectId, analysisId)
                .orElseThrow(() -> new AnalysisNotFoundException(analysisId));

        if (!tenantId.equals(analysis.getTenantId())) {
            throw new AnalysisNotFoundException(analysisId);
        }

        LOG.infof("Processing question for analysis %s: %s", analysisId, questionText);

        RagContextPort.RagContext ragContext = ragContextPort.retrieveContext(
                analysis.getProjectId(), questionText, RAG_MAX_CHUNKS);

        String enrichedContext = buildEnrichedContext(analysis, ragContext);

        String answer = llmGateway.answerQuestion(questionText, enrichedContext);

        String sourcesJson = serializeSources(ragContext.sources());

        Question question = new Question();
        question.setId(UUID.randomUUID());
        question.setAnalysisId(analysisId);
        question.setTenantId(tenantId);
        question.setQuestionText(questionText);
        question.setAnswerText(answer);
        question.setSources(sourcesJson);
        question.setCreatedAt(Instant.now());

        LOG.infof("Answer generated with %d sources for question %s on analysis %s",
                ragContext.sources().size(), question.getId(), analysisId);

        return questionRepository.save(question);
    }

    @Override
    @Transactional
    public List<Question> list(UUID projectId, UUID analysisId) {
        String tenantId = tenantProvider.getCurrentTenantId();
        Analysis analysis = analysisRepository.findByProjectIdAndId(projectId, analysisId)
                .orElseThrow(() -> new AnalysisNotFoundException(analysisId));
        if (!tenantId.equals(analysis.getTenantId())) {
            throw new AnalysisNotFoundException(analysisId);
        }
        return questionRepository.findByAnalysisIdAndTenantId(analysisId, tenantId);
    }

    private String buildEnrichedContext(Analysis analysis, RagContextPort.RagContext ragContext) {
        StringBuilder sb = new StringBuilder();
        sb.append("=== Resumo da Análise ===\n");
        sb.append(analysis.getSummary() != null ? analysis.getSummary() : "Sem resumo disponível");
        sb.append("\n\n");

        if (analysis.getRisks() != null && !analysis.getRisks().isEmpty()) {
            sb.append("=== Riscos Identificados ===\n");
            analysis.getRisks().stream()
                    .sorted(Comparator.comparingInt(r -> severityRank(r.getSeverity())))
                    .forEach(risk -> appendRiskLine(sb, risk));
            sb.append('\n');
        }

        if (!ragContext.assembledContext().isEmpty()) {
            sb.append("=== Trechos Relevantes do Código-Fonte ===\n");
            sb.append(ragContext.assembledContext());
        }

        return sb.toString();
    }

    private static void appendRiskLine(StringBuilder sb, ArchitecturalRisk risk) {
        sb.append('[').append(risk.getSeverity()).append("] ")
                .append(risk.getTitle()).append(" | ")
                .append(risk.getFilePath() != null ? risk.getFilePath() : "-").append(" | ")
                .append(risk.getCategory()).append('\n');
        if (risk.getDescription() != null && !risk.getDescription().isBlank()) {
            sb.append("  ").append(risk.getDescription().replace('\n', ' ')).append('\n');
        }
        if (risk.getSuggestion() != null && !risk.getSuggestion().isBlank()) {
            sb.append("  Sugestão: ").append(risk.getSuggestion().replace('\n', ' ')).append('\n');
        }
    }

    private static int severityRank(RiskSeverity severity) {
        return switch (severity) {
            case CRITICAL -> 0;
            case HIGH -> 1;
            case MEDIUM -> 2;
            case LOW -> 3;
        };
    }

    private String serializeSources(List<RagContextPort.SourceReference> sources) {
        if (sources == null || sources.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(
                    sources.stream()
                            .map(s -> new SourceDto(s.filePath(), s.chunkIndex(), s.snippet(), s.score()))
                            .toList());
        } catch (JsonProcessingException e) {
            LOG.warnf("Failed to serialize sources: %s", e.getMessage());
            return null;
        }
    }

    private record SourceDto(String filePath, int chunkIndex, String snippet, double score) {
    }
}
