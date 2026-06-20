package dev.archlens.interfaces.rest;

import dev.archlens.application.service.QuotaService;
import dev.archlens.application.service.QuotaService.UsageSnapshot;
import dev.archlens.application.port.out.TenantProvider;
import dev.archlens.interfaces.rest.dto.response.AccountUsageResponse;
import dev.archlens.interfaces.rest.mapper.AccountDtoMapper;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("/v1/account")
@Produces(MediaType.APPLICATION_JSON)
public class AccountResource {

    private final QuotaService quotaService;
    private final TenantProvider tenantProvider;
    private final AccountDtoMapper mapper;

    @Inject
    public AccountResource(QuotaService quotaService,
                           TenantProvider tenantProvider,
                           AccountDtoMapper mapper) {
        this.quotaService = quotaService;
        this.tenantProvider = tenantProvider;
        this.mapper = mapper;
    }

    @GET
    @Path("/usage")
    public AccountUsageResponse usage() {
        String tenantId = tenantProvider.getCurrentTenantId();
        UsageSnapshot snapshot = quotaService.usageSnapshot(tenantId);
        return mapper.toResponse(snapshot);
    }
}
