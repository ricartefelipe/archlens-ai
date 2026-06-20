package dev.archlens.application.service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import dev.archlens.application.port.out.ProjectRepositoryPort;
import dev.archlens.application.port.out.TenantAccountRepositoryPort;
import dev.archlens.domain.exception.QuotaExceededException;
import dev.archlens.domain.model.CommercialPlan;
import dev.archlens.domain.model.TenantAccount;
import dev.archlens.domain.model.TenantAccountStatus;
import dev.archlens.infrastructure.persistence.rls.TenantScopedRls;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

@TenantScopedRls
@ApplicationScoped
public class QuotaService {

    private final TenantAccountRepositoryPort accountRepository;
    private final ProjectRepositoryPort projectRepository;

    @ConfigProperty(name = "archlens.commercial.enforce-quotas", defaultValue = "true")
    boolean enforceQuotas;

    @Inject
    public QuotaService(TenantAccountRepositoryPort accountRepository,
                        ProjectRepositoryPort projectRepository) {
        this.accountRepository = accountRepository;
        this.projectRepository = projectRepository;
    }

    /** Construtor para testes unitários com enforcement explícito. */
    QuotaService(TenantAccountRepositoryPort accountRepository,
                 ProjectRepositoryPort projectRepository,
                 boolean enforceQuotas) {
        this.accountRepository = accountRepository;
        this.projectRepository = projectRepository;
        this.enforceQuotas = enforceQuotas;
    }

    @Transactional
    public TenantAccount ensureAccount(String tenantId) {
        return accountRepository.findByTenantId(tenantId)
                .map(this::resetPeriodIfNeeded)
                .orElseGet(() -> createDefaultAccount(tenantId));
    }

    @Transactional
    public void checkCanCreateProject(String tenantId) {
        if (!enforceQuotas) {
            return;
        }
        TenantAccount account = ensureAccount(tenantId);
        assertActive(account);
        CommercialPlan plan = account.getPlan();
        if (plan.isUnlimitedProjects()) {
            return;
        }
        int current = projectRepository.findAllByTenantId(tenantId).size();
        if (current >= plan.maxProjects()) {
            throw new QuotaExceededException(
                    "Limite de projetos atingido para o plano " + plan.displayName()
                            + " (" + plan.maxProjects() + "). Solicite upgrade comercial.");
        }
    }

    @Transactional
    public void checkCanUpload(String tenantId, long additionalBytes) {
        if (!enforceQuotas) {
            return;
        }
        TenantAccount account = ensureAccount(tenantId);
        assertActive(account);
        long maxBytes = (long) account.getPlan().maxUploadMb() * 1024 * 1024;
        if (account.getUploadBytesPeriod() + additionalBytes > maxBytes) {
            throw new QuotaExceededException(
                    "Limite de upload do período excedido (" + account.getPlan().maxUploadMb() + " MB).");
        }
    }

    @Transactional
    public void recordUpload(String tenantId, long bytes) {
        TenantAccount account = ensureAccount(tenantId);
        account.setUploadBytesPeriod(account.getUploadBytesPeriod() + bytes);
        account.setUpdatedAt(Instant.now());
        accountRepository.save(account);
    }

    @Transactional
    public void checkCanRunAnalysis(String tenantId) {
        if (!enforceQuotas) {
            return;
        }
        TenantAccount account = ensureAccount(tenantId);
        assertActive(account);
        CommercialPlan plan = account.getPlan();
        if (plan.isUnlimitedAnalyses()) {
            return;
        }
        if (account.getAnalysesUsedPeriod() >= plan.maxAnalysesPerMonth()) {
            throw new QuotaExceededException(
                    "Limite de análises do período atingido (" + plan.maxAnalysesPerMonth()
                            + "). Plano: " + plan.displayName() + ".");
        }
    }

    @Transactional
    public void recordAnalysis(String tenantId) {
        TenantAccount account = ensureAccount(tenantId);
        account.setAnalysesUsedPeriod(account.getAnalysesUsedPeriod() + 1);
        account.setUpdatedAt(Instant.now());
        accountRepository.save(account);
    }

    public UsageSnapshot usageSnapshot(String tenantId) {
        TenantAccount account = ensureAccount(tenantId);
        CommercialPlan plan = account.getPlan();
        int projectsUsed = projectRepository.findAllByTenantId(tenantId).size();
        return new UsageSnapshot(
                account.getTenantId(),
                plan,
                account.getStatus(),
                projectsUsed,
                plan.maxProjects(),
                account.getAnalysesUsedPeriod(),
                plan.maxAnalysesPerMonth(),
                account.getUploadBytesPeriod(),
                plan.maxUploadMb(),
                account.getUsagePeriodStart());
    }

    @Transactional
    public TenantAccount upgradePlan(String tenantId, CommercialPlan plan, String notes) {
        TenantAccount account = ensureAccount(tenantId);
        account.setPlan(plan);
        account.setNotes(notes);
        account.setUpdatedAt(Instant.now());
        return accountRepository.save(account);
    }

    private TenantAccount createDefaultAccount(String tenantId) {
        TenantAccount account = new TenantAccount();
        account.setTenantId(tenantId);
        account.setPlan(CommercialPlan.PILOT);
        account.setStatus(TenantAccountStatus.ACTIVE);
        account.setAnalysesUsedPeriod(0);
        account.setUploadBytesPeriod(0);
        account.setUsagePeriodStart(firstDayOfCurrentMonth());
        account.setCreatedAt(Instant.now());
        account.setUpdatedAt(Instant.now());
        return accountRepository.save(account);
    }

    private TenantAccount resetPeriodIfNeeded(TenantAccount account) {
        LocalDate currentPeriod = firstDayOfCurrentMonth();
        if (currentPeriod.equals(account.getUsagePeriodStart())) {
            return account;
        }
        account.setUsagePeriodStart(currentPeriod);
        account.setAnalysesUsedPeriod(0);
        account.setUploadBytesPeriod(0);
        account.setUpdatedAt(Instant.now());
        return accountRepository.save(account);
    }

    private static LocalDate firstDayOfCurrentMonth() {
        return YearMonth.now().atDay(1);
    }

    private static void assertActive(TenantAccount account) {
        if (account.getStatus() != TenantAccountStatus.ACTIVE) {
            throw new QuotaExceededException("Conta comercial suspensa. Entre em contato com o consultor.");
        }
    }

    public record UsageSnapshot(
            String tenantId,
            CommercialPlan plan,
            TenantAccountStatus status,
            int projectsUsed,
            int projectsLimit,
            int analysesUsed,
            int analysesLimit,
            long uploadBytesUsed,
            int uploadMbLimit,
            LocalDate usagePeriodStart) {
    }
}
