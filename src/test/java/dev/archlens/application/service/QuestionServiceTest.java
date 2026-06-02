package dev.archlens.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.fasterxml.jackson.databind.ObjectMapper;

import dev.archlens.application.port.out.AnalysisRepositoryPort;
import dev.archlens.application.port.out.LlmAnalysisResult;
import dev.archlens.application.port.out.LlmGateway;
import dev.archlens.application.port.out.QuestionRepositoryPort;
import dev.archlens.application.port.out.RagContextPort;
import dev.archlens.domain.exception.AnalysisNotFoundException;
import dev.archlens.domain.model.Analysis;
import dev.archlens.domain.model.Question;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class QuestionServiceTest {

    private static final UUID PROJECT_ID = UUID.randomUUID();
    private static final UUID ANALYSIS_ID = UUID.randomUUID();

    private InMemoryQuestionRepository questionRepository;
    private StubAnalysisRepository analysisRepository;
    private RecordingLlmGateway llmGateway;
    private StubRagContextPort ragContextPort;
    private QuestionService service;

    @BeforeEach
    void setUp() {
        questionRepository = new InMemoryQuestionRepository();
        analysisRepository = new StubAnalysisRepository();
        llmGateway = new RecordingLlmGateway();
        ragContextPort = new StubRagContextPort();
        service = new QuestionService(
                questionRepository,
                analysisRepository,
                llmGateway,
                ragContextPort,
                () -> "tenant-1",
                new ObjectMapper());
    }

    @Test
    @DisplayName("ask persiste pergunta, resposta do LLM e fontes serializadas em JSON")
    void askPersistsAnswerAndSources() {
        analysisRepository.analysis = analysisWithTenant("tenant-1");
        ragContextPort.sources = List.of(
                new RagContextPort.SourceReference("src/App.java", 0, "class App {}", 0.91));

        Question question = service.ask(PROJECT_ID, ANALYSIS_ID, "Como funciona a camada de domínio?");

        assertNotNull(question.getId());
        assertEquals("tenant-1", question.getTenantId());
        assertEquals("Como funciona a camada de domínio?", question.getQuestionText());
        assertNotNull(question.getAnswerText());
        assertTrue(question.getSources().contains("src/App.java"));
        assertTrue(llmGateway.lastContext.contains("class App {}"));
        assertTrue(questionRepository.findById(question.getId()).isPresent());
    }

    @Test
    @DisplayName("ask deixa sources nulo quando não há contexto RAG")
    void askLeavesSourcesNullWithoutContext() {
        analysisRepository.analysis = analysisWithTenant("tenant-1");

        Question question = service.ask(PROJECT_ID, ANALYSIS_ID, "pergunta");

        assertEquals(null, question.getSources());
    }

    @Test
    @DisplayName("ask falha quando a análise não existe")
    void askFailsWhenAnalysisMissing() {
        analysisRepository.analysis = null;

        assertThrows(AnalysisNotFoundException.class,
                () -> service.ask(PROJECT_ID, ANALYSIS_ID, "pergunta"));
    }

    @Test
    @DisplayName("ask falha quando a análise pertence a outro tenant")
    void askFailsOnTenantMismatch() {
        analysisRepository.analysis = analysisWithTenant("outro-tenant");

        assertThrows(AnalysisNotFoundException.class,
                () -> service.ask(PROJECT_ID, ANALYSIS_ID, "pergunta"));
    }

    private static Analysis analysisWithTenant(String tenantId) {
        Analysis analysis = new Analysis();
        analysis.setId(ANALYSIS_ID);
        analysis.setProjectId(PROJECT_ID);
        analysis.setTenantId(tenantId);
        analysis.setSummary("resumo");
        analysis.setCreatedAt(Instant.now());
        analysis.setUpdatedAt(Instant.now());
        return analysis;
    }

    private static final class RecordingLlmGateway implements LlmGateway {
        private String lastContext;

        @Override
        public LlmAnalysisResult analyzeProject(String projectContext) {
            throw new UnsupportedOperationException("not used");
        }

        @Override
        public String answerQuestion(String question, String analysisContext) {
            this.lastContext = analysisContext;
            return "resposta para: " + question;
        }
    }

    private static final class StubRagContextPort implements RagContextPort {
        private List<SourceReference> sources = List.of();

        @Override
        public RagContext retrieveContext(UUID projectId, String query, int maxChunks) {
            String assembled = sources.isEmpty() ? "" : sources.get(0).snippet();
            return new RagContext(assembled, sources, sources.size());
        }
    }

    private static final class StubAnalysisRepository implements AnalysisRepositoryPort {
        private Analysis analysis;

        @Override
        public Analysis save(Analysis a) {
            return a;
        }

        @Override
        public Optional<Analysis> findById(UUID id) {
            return Optional.ofNullable(analysis);
        }

        @Override
        public Optional<Analysis> findByProjectIdAndId(UUID projectId, UUID analysisId) {
            return Optional.ofNullable(analysis);
        }

        @Override
        public List<Analysis> findByProjectIdAndTenantId(UUID projectId, String tenantId) {
            return analysis == null ? List.of() : List.of(analysis);
        }
    }

    private static final class InMemoryQuestionRepository implements QuestionRepositoryPort {
        private final List<Question> store = new ArrayList<>();

        @Override
        public Question save(Question question) {
            store.add(question);
            return question;
        }

        @Override
        public Optional<Question> findById(UUID id) {
            return store.stream().filter(q -> q.getId().equals(id)).findFirst();
        }

        @Override
        public List<Question> findByAnalysisIdAndTenantId(UUID analysisId, String tenantId) {
            return store.stream()
                    .filter(q -> q.getAnalysisId().equals(analysisId) && tenantId.equals(q.getTenantId()))
                    .toList();
        }
    }
}
