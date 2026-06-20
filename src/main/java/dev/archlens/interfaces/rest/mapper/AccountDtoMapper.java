package dev.archlens.interfaces.rest.mapper;

import dev.archlens.application.service.QuotaService.UsageSnapshot;
import dev.archlens.interfaces.rest.dto.response.AccountUsageResponse;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class AccountDtoMapper {

    public AccountUsageResponse toResponse(UsageSnapshot snapshot) {
        return new AccountUsageResponse(
                snapshot.tenantId(),
                snapshot.plan().name(),
                snapshot.plan().displayName(),
                snapshot.status().name(),
                snapshot.projectsUsed(),
                snapshot.projectsLimit(),
                snapshot.analysesUsed(),
                snapshot.analysesLimit(),
                snapshot.uploadBytesUsed() / (1024 * 1024),
                snapshot.uploadMbLimit(),
                snapshot.usagePeriodStart());
    }
}
