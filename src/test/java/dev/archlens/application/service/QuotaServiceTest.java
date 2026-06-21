package dev.archlens.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import dev.archlens.application.port.out.ProjectRepositoryPort;
import dev.archlens.application.port.out.TenantAccountRepositoryPort;
import dev.archlens.domain.exception.QuotaExceededException;
import dev.archlens.domain.model.CommercialPlan;
import dev.archlens.domain.model.Project;
import dev.archlens.domain.model.ProjectStatus;
import dev.archlens.domain.model.TenantAccount;
import dev.archlens.domain.model.TenantAccountStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class QuotaServiceTest {

    private InMemoryTenantAccountRepository accountRepository;
    private InMemoryProjectRepository projectRepository;
    private QuotaService quotaService;

    @BeforeEach
    void setUp() {
        accountRepository = new InMemoryTenantAccountRepository();
        projectRepository = new InMemoryProjectRepository();
        quotaService = new QuotaService(accountRepository, projectRepository, true);
    }

    @Test
    @DisplayName("provisiona conta PILOT no primeiro uso")
    void provisionsPilotOnFirstUse() {
        QuotaService.UsageSnapshot snapshot = quotaService.usageSnapshot("tenant-a");

        assertEquals("tenant-a", snapshot.tenantId());
        assertEquals(CommercialPlan.PILOT, snapshot.plan());
        assertEquals(TenantAccountStatus.ACTIVE, snapshot.status());
        assertEquals(1, snapshot.projectsLimit());
    }

    @Test
    @DisplayName("bloqueia segundo projeto no plano PILOT")
    void blocksSecondProjectOnPilot() {
        quotaService.checkCanCreateProject("tenant-a");
        projectRepository.addProject("tenant-a", "proj-1");

        assertThrows(QuotaExceededException.class, () -> quotaService.checkCanCreateProject("tenant-a"));
    }

    @Test
    @DisplayName("upgrade de plano libera mais projetos")
    void upgradePlanIncreasesLimits() {
        quotaService.checkCanCreateProject("tenant-a");
        projectRepository.addProject("tenant-a", "proj-1");
        quotaService.upgradePlan("tenant-a", CommercialPlan.DIAGNOSTICO, "contrato assinado");

        quotaService.checkCanCreateProject("tenant-a");
        projectRepository.addProject("tenant-a", "proj-2");
    }

    @Test
    @DisplayName("bloqueia análise quando cota mensal esgotada")
    void blocksAnalysisWhenMonthlyQuotaReached() {
        TenantAccount account = quotaService.ensureAccount("tenant-a");
        account.setAnalysesUsedPeriod(1);
        accountRepository.save(account);

        assertThrows(QuotaExceededException.class, () -> quotaService.checkCanRunAnalysis("tenant-a"));
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

        private void addProject(String tenantId, String name) {
            Project project = new Project();
            project.setId(UUID.randomUUID());
            project.setTenantId(tenantId);
            project.setName(name);
            project.setStatus(ProjectStatus.CREATED);
            store.add(project);
        }

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
