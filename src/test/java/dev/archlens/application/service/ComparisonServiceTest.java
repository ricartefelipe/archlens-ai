package dev.archlens.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import dev.archlens.application.port.out.AnalysisRepositoryPort;
import dev.archlens.application.port.out.ProjectRepositoryPort;
import dev.archlens.application.port.out.TenantProvider;
import dev.archlens.domain.exception.AnalysisNotComparableException;
import dev.archlens.domain.model.Analysis;
import dev.archlens.domain.model.AnalysisComparisonResult;
import dev.archlens.domain.model.AnalysisStatus;
import dev.archlens.domain.model.ArchitecturalRisk;
import dev.archlens.domain.model.Project;
import dev.archlens.domain.model.ProjectStatus;
import dev.archlens.domain.model.RiskCategory;
import dev.archlens.domain.model.RiskSeverity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ComparisonServiceTest {

    private static final UUID PROJECT_ID = UUID.randomUUID();
    private static final UUID BASELINE_ID = UUID.randomUUID();
    private static final UUID CURRENT_ID = UUID.randomUUID();

    private InMemoryAnalysisRepository analysisRepository;
    private InMemoryProjectRepository projectRepository;
    private ComparisonService service;

    @BeforeEach
    void setUp() {
        analysisRepository = new InMemoryAnalysisRepository();
        projectRepository = new InMemoryProjectRepository();
        service = new ComparisonService(analysisRepository, projectRepository, () -> "tenant-1");

        Project project = new Project();
        project.setId(PROJECT_ID);
        project.setTenantId("tenant-1");
        project.setStatus(ProjectStatus.READY);
        projectRepository.save(project);
    }

    @Test
    @DisplayName("identifica riscos adicionados, removidos e com severidade alterada")
    void comparesRiskSets() {
        analysisRepository.save(completedAnalysis(BASELINE_ID, List.of(
                risk("Shared coupling", RiskSeverity.HIGH, "src/OrderService.java"),
                risk("Missing tests", RiskSeverity.MEDIUM, "src/Payment.java"))));

        analysisRepository.save(completedAnalysis(CURRENT_ID, List.of(
                risk("Shared coupling", RiskSeverity.CRITICAL, "src/OrderService.java"),
                risk("God class", RiskSeverity.HIGH, "src/Catalog.java"))));

        AnalysisComparisonResult result = service.compare(PROJECT_ID, BASELINE_ID, CURRENT_ID);

        assertEquals(1, result.getAdded().size());
        assertEquals("God class", result.getAdded().get(0).getTitle());
        assertEquals(1, result.getRemoved().size());
        assertEquals("Missing tests", result.getRemoved().get(0).getTitle());
        assertEquals(1, result.getSeverityChanged().size());
        assertEquals(RiskSeverity.HIGH,
                result.getSeverityChanged().get(0).getBaselineRisk().getSeverity());
        assertEquals(RiskSeverity.CRITICAL,
                result.getSeverityChanged().get(0).getCurrentRisk().getSeverity());
        assertEquals(0, result.getUnchanged().size());
        assertEquals(2, result.getBaselineSeverityCounts().get(RiskSeverity.HIGH)
                + result.getBaselineSeverityCounts().get(RiskSeverity.MEDIUM));
        assertEquals(1, result.getCurrentSeverityCounts().get(RiskSeverity.CRITICAL));
    }

    @Test
    @DisplayName("rejeita comparação quando análise não está COMPLETED")
    void rejectsNonCompletedAnalysis() {
        Analysis pending = completedAnalysis(CURRENT_ID, List.of());
        pending.setStatus(AnalysisStatus.PENDING);
        analysisRepository.save(pending);
        analysisRepository.save(completedAnalysis(BASELINE_ID, List.of()));

        assertThrows(AnalysisNotComparableException.class,
                () -> service.compare(PROJECT_ID, BASELINE_ID, CURRENT_ID));
    }

    private static Analysis completedAnalysis(UUID id, List<ArchitecturalRisk> risks) {
        Analysis analysis = new Analysis();
        analysis.setId(id);
        analysis.setProjectId(PROJECT_ID);
        analysis.setTenantId("tenant-1");
        analysis.setStatus(AnalysisStatus.COMPLETED);
        analysis.setCreatedAt(Instant.parse("2026-01-01T10:00:00Z"));
        analysis.setUpdatedAt(Instant.parse("2026-01-01T10:05:00Z"));
        analysis.setRisks(new ArrayList<>(risks));
        for (ArchitecturalRisk risk : analysis.getRisks()) {
            risk.setAnalysisId(id);
            risk.setTenantId("tenant-1");
        }
        return analysis;
    }

    private static ArchitecturalRisk risk(String title, RiskSeverity severity, String filePath) {
        ArchitecturalRisk risk = new ArchitecturalRisk();
        risk.setId(UUID.randomUUID());
        risk.setCategory(RiskCategory.EXCESSIVE_COUPLING);
        risk.setSeverity(severity);
        risk.setTitle(title);
        risk.setDescription("desc");
        risk.setFilePath(filePath);
        return risk;
    }

    private static final class InMemoryAnalysisRepository implements AnalysisRepositoryPort {
        private final List<Analysis> store = new ArrayList<>();

        @Override
        public Analysis save(Analysis analysis) {
            store.removeIf(a -> a.getId().equals(analysis.getId()));
            store.add(analysis);
            return analysis;
        }

        @Override
        public Optional<Analysis> findById(UUID id) {
            return store.stream().filter(a -> a.getId().equals(id)).findFirst();
        }

        @Override
        public Optional<Analysis> findByProjectIdAndId(UUID projectId, UUID analysisId) {
            return store.stream()
                    .filter(a -> a.getProjectId().equals(projectId) && a.getId().equals(analysisId))
                    .findFirst();
        }

        @Override
        public List<Analysis> findByProjectIdAndTenantId(UUID projectId, String tenantId) {
            return store.stream()
                    .filter(a -> a.getProjectId().equals(projectId) && tenantId.equals(a.getTenantId()))
                    .toList();
        }
    }

    private static final class InMemoryProjectRepository implements ProjectRepositoryPort {
        private final List<Project> store = new ArrayList<>();

        @Override
        public Project save(Project project) {
            store.removeIf(p -> p.getId().equals(project.getId()));
            store.add(project);
            return project;
        }

        @Override
        public Optional<Project> findById(UUID id) {
            return store.stream().filter(p -> p.getId().equals(id)).findFirst();
        }

        @Override
        public Optional<Project> findByIdAndTenantId(UUID id, String tenantId) {
            return store.stream()
                    .filter(p -> p.getId().equals(id) && tenantId.equals(p.getTenantId()))
                    .findFirst();
        }

        @Override
        public List<Project> findAllByTenantId(String tenantId) {
            return store.stream().filter(p -> tenantId.equals(p.getTenantId())).toList();
        }

        @Override
        public boolean existsById(UUID id) {
            return store.stream().anyMatch(p -> p.getId().equals(id));
        }

        @Override
        public boolean existsByIdAndTenantId(UUID id, String tenantId) {
            return store.stream().anyMatch(p -> p.getId().equals(id) && tenantId.equals(p.getTenantId()));
        }
    }
}
