package dev.archlens.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import dev.archlens.application.port.out.ProjectRepositoryPort;
import dev.archlens.application.port.out.TenantAccountRepositoryPort;
import dev.archlens.domain.model.CommercialPlan;
import dev.archlens.domain.model.Project;
import dev.archlens.domain.model.ProjectStatus;
import dev.archlens.domain.model.TenantAccount;
import dev.archlens.domain.model.TenantAccountStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class AdminTenantServiceTest {

    private InMemoryTenantAccountRepository accountRepository;
    private InMemoryProjectRepository projectRepository;
    private AdminTenantService adminTenantService;

    @BeforeEach
    void setUp() {
        accountRepository = new InMemoryTenantAccountRepository();
        projectRepository = new InMemoryProjectRepository();
        QuotaService quotaService = new QuotaService(accountRepository, projectRepository, true);
        adminTenantService = new AdminTenantService(accountRepository, quotaService);
    }

    @Test
    @DisplayName("listAll retorna visão admin para cada tenant")
    void listAllReturnsViews() {
        TenantAccount tenantA = new TenantAccount();
        tenantA.setTenantId("tenant-a");
        tenantA.setPlan(CommercialPlan.PILOT);
        tenantA.setStatus(TenantAccountStatus.ACTIVE);
        accountRepository.save(tenantA);

        TenantAccount tenantB = new TenantAccount();
        tenantB.setTenantId("tenant-b");
        tenantB.setPlan(CommercialPlan.DIAGNOSTICO);
        tenantB.setStatus(TenantAccountStatus.ACTIVE);
        accountRepository.save(tenantB);

        assertEquals(2, adminTenantService.listAll().size());
    }

    @Test
    @DisplayName("getOne provisiona tenant inexistente")
    void getOneProvisionsMissingTenant() {
        AdminTenantService.TenantAdminView view = adminTenantService.getOne("tenant-new");

        assertEquals("tenant-new", view.usage().tenantId());
        assertEquals(CommercialPlan.PILOT, view.usage().plan());
    }

    @Test
    @DisplayName("updateStatus altera status da conta")
    void updateStatusChangesAccount() {
        adminTenantService.getOne("tenant-a");

        AdminTenantService.TenantAdminView view =
                adminTenantService.updateStatus("tenant-a", TenantAccountStatus.SUSPENDED);

        assertEquals(TenantAccountStatus.SUSPENDED, view.usage().status());
    }

    @Test
    @DisplayName("updateStatus persiste notes existentes")
    void updateStatusKeepsNotes() {
        TenantAccount account = new TenantAccount();
        account.setTenantId("tenant-a");
        account.setPlan(CommercialPlan.PILOT);
        account.setStatus(TenantAccountStatus.ACTIVE);
        account.setNotes("cliente estratégico");
        accountRepository.save(account);

        AdminTenantService.TenantAdminView view =
                adminTenantService.updateStatus("tenant-a", TenantAccountStatus.SUSPENDED);

        assertEquals("cliente estratégico", view.notes());
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

        @Override
        public List<TenantAccount> findAll() {
            return List.copyOf(store);
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
        public Optional<Project> findById(java.util.UUID id) {
            return store.stream().filter(p -> p.getId().equals(id)).findFirst();
        }

        @Override
        public Optional<Project> findByIdAndTenantId(java.util.UUID id, String tenantId) {
            return store.stream()
                    .filter(p -> p.getId().equals(id) && tenantId.equals(p.getTenantId()))
                    .findFirst();
        }

        @Override
        public List<Project> findAllByTenantId(String tenantId) {
            return store.stream().filter(p -> tenantId.equals(p.getTenantId())).toList();
        }

        @Override
        public boolean existsById(java.util.UUID id) {
            return store.stream().anyMatch(p -> p.getId().equals(id));
        }

        @Override
        public boolean existsByIdAndTenantId(java.util.UUID id, String tenantId) {
            return store.stream().anyMatch(p -> p.getId().equals(id) && tenantId.equals(p.getTenantId()));
        }

        void addProject(String tenantId, String name) {
            Project project = new Project();
            project.setId(java.util.UUID.randomUUID());
            project.setTenantId(tenantId);
            project.setName(name);
            project.setStatus(ProjectStatus.CREATED);
            store.add(project);
        }
    }
}
