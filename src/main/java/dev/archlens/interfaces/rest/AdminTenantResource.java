package dev.archlens.interfaces.rest;

import java.util.List;

import dev.archlens.application.service.AdminTenantService;
import dev.archlens.application.service.AdminTenantService.TenantAdminView;
import dev.archlens.application.service.QuotaService;
import dev.archlens.domain.model.CommercialPlan;
import dev.archlens.domain.model.TenantAccountStatus;
import dev.archlens.interfaces.rest.dto.request.UpgradeTenantPlanRequest;
import dev.archlens.interfaces.rest.dto.request.UpdateTenantStatusRequest;
import dev.archlens.interfaces.rest.dto.response.AccountUsageResponse;
import dev.archlens.interfaces.rest.dto.response.AdminTenantResponse;
import dev.archlens.interfaces.rest.mapper.AccountDtoMapper;
import dev.archlens.interfaces.rest.mapper.PlatformDtoMapper;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PATCH;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;

@Path("/v1/admin/tenants")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class AdminTenantResource {

    private final QuotaService quotaService;
    private final AdminTenantService adminTenantService;
    private final AccountDtoMapper accountMapper;
    private final PlatformDtoMapper platformMapper;

    @Inject
    public AdminTenantResource(QuotaService quotaService,
                               AdminTenantService adminTenantService,
                               AccountDtoMapper accountMapper,
                               PlatformDtoMapper platformMapper) {
        this.quotaService = quotaService;
        this.adminTenantService = adminTenantService;
        this.accountMapper = accountMapper;
        this.platformMapper = platformMapper;
    }

    @GET
    public List<AdminTenantResponse> listAll(@jakarta.ws.rs.core.Context SecurityContext securityContext) {
        if (!securityContext.isUserInRole("admin")) {
            throw new jakarta.ws.rs.ForbiddenException();
        }
        return adminTenantService.listAll().stream()
                .map(platformMapper::toAdminTenantResponse)
                .toList();
    }

    @GET
    @Path("/{tenantId}")
    public AdminTenantResponse getOne(@PathParam("tenantId") String tenantId,
                                      @jakarta.ws.rs.core.Context SecurityContext securityContext) {
        if (!securityContext.isUserInRole("admin")) {
            throw new jakarta.ws.rs.ForbiddenException();
        }
        return platformMapper.toAdminTenantResponse(adminTenantService.getOne(tenantId));
    }

    @PATCH
    @Path("/{tenantId}/status")
    public AdminTenantResponse updateStatus(@PathParam("tenantId") String tenantId,
                                            @Valid UpdateTenantStatusRequest request,
                                            @jakarta.ws.rs.core.Context SecurityContext securityContext) {
        if (!securityContext.isUserInRole("admin")) {
            throw new jakarta.ws.rs.ForbiddenException();
        }
        TenantAccountStatus status = TenantAccountStatus.valueOf(request.status());
        TenantAdminView view = adminTenantService.updateStatus(tenantId, status);
        return platformMapper.toAdminTenantResponse(view);
    }

    @PUT
    @Path("/{tenantId}/plan")
    public Response upgradePlan(@PathParam("tenantId") String tenantId,
                                UpgradeTenantPlanRequest request,
                                @jakarta.ws.rs.core.Context SecurityContext securityContext) {
        if (!securityContext.isUserInRole("admin")) {
            return Response.status(Response.Status.FORBIDDEN).build();
        }
        CommercialPlan plan = CommercialPlan.valueOf(request.plan());
        quotaService.upgradePlan(tenantId, plan, request.notes());
        AccountUsageResponse body = accountMapper.toResponse(quotaService.usageSnapshot(tenantId));
        return Response.ok(body).build();
    }
}
