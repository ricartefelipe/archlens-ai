package dev.archlens.interfaces.rest;

import dev.archlens.application.service.QuotaService;
import dev.archlens.domain.model.CommercialPlan;
import dev.archlens.interfaces.rest.dto.request.UpgradeTenantPlanRequest;
import dev.archlens.interfaces.rest.dto.response.AccountUsageResponse;
import dev.archlens.interfaces.rest.mapper.AccountDtoMapper;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;

@Path("/v1/admin/tenants")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class AdminTenantResource {

    private final QuotaService quotaService;
    private final AccountDtoMapper mapper;

    @Inject
    public AdminTenantResource(QuotaService quotaService, AccountDtoMapper mapper) {
        this.quotaService = quotaService;
        this.mapper = mapper;
    }

    @PUT
    @Path("/{tenantId}/plan")
    public Response upgradePlan(@PathParam("tenantId") String tenantId,
                                UpgradeTenantPlanRequest request,
                                @Context SecurityContext securityContext) {
        if (!securityContext.isUserInRole("admin")) {
            return Response.status(Response.Status.FORBIDDEN).build();
        }
        CommercialPlan plan = CommercialPlan.valueOf(request.plan());
        quotaService.upgradePlan(tenantId, plan, request.notes());
        AccountUsageResponse body = mapper.toResponse(quotaService.usageSnapshot(tenantId));
        return Response.ok(body).build();
    }
}
