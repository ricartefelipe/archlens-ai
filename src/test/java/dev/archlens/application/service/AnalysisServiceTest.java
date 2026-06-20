package dev.archlens.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import dev.archlens.application.port.out.AdrRepositoryPort;
import dev.archlens.application.port.out.AnalysisRepositoryPort;
import dev.archlens.application.port.out.ProjectRepositoryPort;
import dev.archlens.application.port.out.TenantProvider;
import dev.archlens.domain.exception.ProjectNotFoundException;
import dev.archlens.domain.exception.ProjectNotReadyException;
import dev.archlens.domain.model.Adr;
import dev.archlens.domain.model.Analysis;
import dev.archlens.domain.model.AnalysisStatus;
import dev.archlens.domain.model.Project;
import dev.archlens.domain.model.ProjectStatus;
import dev.archlens.infrastructure.messaging.AnalysisEvent;
import dev.archlens.infrastructure.messaging.AnalysisProducer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class AnalysisServiceTest {

    private InMemoryAnalysisRepository analysisRepository;
    private InMemoryProjectRepository projectRepository;
    private RecordingProducer producer;
    private AnalysisService service;

    @BeforeEach
    void setUp() {
        analysisRepository = new InMemoryAnalysisRepository();
        projectRepository = new InMemoryProjectRepository();
        producer = new RecordingProducer();
        service = new AnalysisService(
                analysisRepository,
                projectRepository,
                new NoopAdrRepository(),
                producer,
                () -> "tenant-1");
    }

    @Test
    @DisplayName("create persiste análise PENDING e publica evento")
    void createPersistsPendingAndEmitsEvent() {
        UUID projectId = UUID.randomUUID();
        projectRepository.add(projectId);

        Analysis analysis = service.create(projectId);

        assertNotNull(analysis.getId());
        assertEquals(AnalysisStatus.PENDING, analysis.getStatus());
        assertEquals("tenant-1", analysis.getTenantId());
        assertTrue(analysisRepository.findById(analysis.getId()).isPresent());

        assertEquals(1, producer.events.size());
        AnalysisEvent event = producer.events.get(0);
        assertEquals(analysis.getId(), event.analysisId());
        assertEquals(projectId, event.projectId());
        assertEquals("tenant-1", event.tenantId());
    }

    @Test
    @DisplayName("create falha quando o projeto não existe e não publica evento")
    void createFailsWhenProjectMissing() {
        UUID projectId = UUID.randomUUID();

        assertThrows(ProjectNotFoundException.class, () -> service.create(projectId));
        assertTrue(producer.events.isEmpty());
    }

    @Test
    @DisplayName("listByProject falha quando o projeto não existe")
    void listByProjectFailsWhenProjectMissing() {
        assertThrows(ProjectNotFoundException.class, () -> service.listByProject(UUID.randomUUID()));
    }

    @Test
    @DisplayName("create falha quando projeto ainda não está pronto")
    void createFailsWhenProjectNotReady() {
        UUID projectId = UUID.randomUUID();
        projectRepository.addWithStatus(projectId, ProjectStatus.INGESTING);

        assertThrows(ProjectNotReadyException.class, () -> service.create(projectId));
    }

    private static final class RecordingProducer extends AnalysisProducer {
        private final List<AnalysisEvent> events = new ArrayList<>();

        @Override
        public void sendAnalysisRequest(AnalysisEvent event) {
            events.add(event);
        }
    }

    private static final class NoopAdrRepository implements AdrRepositoryPort {
        @Override
        public Adr save(Adr adr) {
            return adr;
        }

        @Override
        public List<Adr> saveAll(List<Adr> adrs) {
            return adrs;
        }

        @Override
        public List<Adr> findByAnalysisId(UUID analysisId) {
            return List.of();
        }
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
        private final List<Project> projects = new ArrayList<>();

        private void add(UUID id) {
            addWithStatus(id, ProjectStatus.READY);
        }

        private void addWithStatus(UUID id, ProjectStatus status) {
            Project project = new Project();
            project.setId(id);
            project.setTenantId("tenant-1");
            project.setStatus(status);
            projects.add(project);
        }

        @Override
        public Project save(Project project) {
            return project;
        }

        @Override
        public Optional<Project> findById(UUID id) {
            return projects.stream().filter(p -> p.getId().equals(id)).findFirst();
        }

        @Override
        public Optional<Project> findByIdAndTenantId(UUID id, String tenantId) {
            return projects.stream()
                    .filter(p -> p.getId().equals(id) && tenantId.equals(p.getTenantId()))
                    .findFirst();
        }

        @Override
        public List<Project> findAllByTenantId(String tenantId) {
            return projects.stream().filter(p -> tenantId.equals(p.getTenantId())).toList();
        }

        @Override
        public boolean existsById(UUID id) {
            return projects.stream().anyMatch(p -> p.getId().equals(id));
        }

        @Override
        public boolean existsByIdAndTenantId(UUID id, String tenantId) {
            return projects.stream().anyMatch(p -> p.getId().equals(id) && tenantId.equals(p.getTenantId()));
        }
    }
}
