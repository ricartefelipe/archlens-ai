package dev.archlens.application.service;

import java.time.Instant;
import java.util.List;

import dev.archlens.application.port.out.TenantAccountRepositoryPort;
import dev.archlens.application.service.QuotaService.UsageSnapshot;
import dev.archlens.domain.model.TenantAccount;
import dev.archlens.domain.model.TenantAccountStatus;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

@ApplicationScoped
public class AdminTenantService {

    private final TenantAccountRepositoryPort accountRepository;
    private final QuotaService quotaService;

    @Inject
    public AdminTenantService(TenantAccountRepositoryPort accountRepository, QuotaService quotaService) {
        this.accountRepository = accountRepository;
        this.quotaService = quotaService;
    }

    public record TenantAdminView(UsageSnapshot usage, String notes) {
    }

    public List<TenantAdminView> listAll() {
        return accountRepository.findAll().stream()
                .map(account -> toView(account.getTenantId()))
                .toList();
    }

    public TenantAdminView getOne(String tenantId) {
        quotaService.ensureAccount(tenantId);
        return toView(tenantId);
    }

    @Transactional
    public TenantAdminView updateStatus(String tenantId, TenantAccountStatus status) {
        TenantAccount account = quotaService.ensureAccount(tenantId);
        account.setStatus(status);
        account.setUpdatedAt(Instant.now());
        accountRepository.save(account);
        return toView(tenantId);
    }

    private TenantAdminView toView(String tenantId) {
        UsageSnapshot usage = quotaService.usageSnapshot(tenantId);
        String notes = accountRepository.findByTenantId(tenantId)
                .map(TenantAccount::getNotes)
                .orElse(null);
        return new TenantAdminView(usage, notes);
    }
}
