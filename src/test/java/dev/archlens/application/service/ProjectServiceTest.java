package dev.archlens.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import dev.archlens.application.port.out.ProjectRepositoryPort;
import dev.archlens.application.port.out.TenantAccountRepositoryPort;
import dev.archlens.application.port.out.TenantProvider;
import dev.archlens.domain.exception.ProjectNotFoundException;
import dev.archlens.domain.model.Project;
import dev.archlens.domain.model.ProjectStatus;
import dev.archlens.domain.model.TenantAccount;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ProjectServiceTest {

    private InMemoryProjectRepository repository;
    private FixedTenantProvider tenantProvider;
    private QuotaService quotaService;
    private ProjectService service;

    @BeforeEach
    void setUp() {
        repository = new InMemoryProjectRepository();
        tenantProvider = new FixedTenantProvider("tenant-1");
        quotaService = new QuotaService(new InMemoryTenantAccountRepository(), repository, false);
        service = new ProjectService(repository, tenantProvider, quotaService);
    }

    @Test
    @DisplayName("create persiste projeto com tenant atual e status CREATED")
    void createPersistsProjectWithCurrentTenant() {
        Project created = service.create("checkout", "serviço de pagamento");

        assertNotNull(created.getId());
        assertEquals("tenant-1", created.getTenantId());
        assertEquals("checkout", created.getName());
        assertEquals(ProjectStatus.CREATED, created.getStatus());
        assertNotNull(created.getCreatedAt());
        assertTrue(repository.findById(created.getId()).isPresent());
    }

    @Test
    @DisplayName("listAll retorna apenas projetos do tenant atual")
    void listAllFiltersByTenant() {
        service.create("a", null);
        service.create("b", null);
        tenantProvider.tenantId = "tenant-2";
        service.create("c", null);

        tenantProvider.tenantId = "tenant-1";
        List<Project> result = service.listAll();

        assertEquals(2, result.size());
        assertTrue(result.stream().allMatch(p -> "tenant-1".equals(p.getTenantId())));
    }

    @Test
    @DisplayName("getById retorna o projeto quando existe")
    void getByIdReturnsProject() {
        Project created = service.create("api", null);

        Project found = service.getById(created.getId());

        assertEquals(created.getId(), found.getId());
    }

    @Test
    @DisplayName("getById não expõe projeto de outro tenant")
    void getByIdRejectsOtherTenant() {
        Project created = service.create("api", null);
        tenantProvider.tenantId = "tenant-2";
        assertThrows(ProjectNotFoundException.class, () -> service.getById(created.getId()));
    }

    @Test
    @DisplayName("getById lança ProjectNotFoundException quando ausente")
    void getByIdThrowsWhenMissing() {
        UUID unknown = UUID.randomUUID();
        assertThrows(ProjectNotFoundException.class, () -> service.getById(unknown));
    }

    private static final class FixedTenantProvider implements TenantProvider {
        private String tenantId;

        private FixedTenantProvider(String tenantId) {
            this.tenantId = tenantId;
        }

        @Override
        public String getCurrentTenantId() {
            return tenantId;
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

    private static final class InMemoryTenantAccountRepository implements TenantAccountRepositoryPort {
        private final List<TenantAccount> store = new ArrayList<>();

        @Override
        public TenantAccount save(TenantAccount account) {
            store.removeIf(a -> a.getTenantId().equals(account.getTenantId()));
            store.add(account);
            return account;
        }

        @Override
        public Optional<TenantAccount> findByTenantId(String tenantId) {
            return store.stream().filter(a -> tenantId.equals(a.getTenantId())).findFirst();
        }
    }
}
