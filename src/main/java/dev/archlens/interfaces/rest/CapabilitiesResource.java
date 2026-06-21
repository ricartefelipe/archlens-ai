package dev.archlens.interfaces.rest;

import dev.archlens.application.service.CapabilitiesService;
import dev.archlens.interfaces.rest.dto.response.CapabilitiesResponse;
import jakarta.annotation.security.PermitAll;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/public/capabilities")
@Produces(MediaType.APPLICATION_JSON)
public class CapabilitiesResource {

    private final CapabilitiesService capabilitiesService;

    @Inject
    public CapabilitiesResource(CapabilitiesService capabilitiesService) {
        this.capabilitiesService = capabilitiesService;
    }

    @GET
    @PermitAll
    public Response capabilities() {
        CapabilitiesService.CapabilitiesSnapshot snapshot = capabilitiesService.snapshot();
        return Response.ok(new CapabilitiesResponse(
                snapshot.product(),
                snapshot.version(),
                snapshot.llmProvider(),
                snapshot.llmConfigured(),
                snapshot.enforceQuotas(),
                snapshot.reportBrandName(),
                snapshot.analysisLlmFallbackEnabled())).build();
    }
}
