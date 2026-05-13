package dev.archlens.interfaces.rest;

import java.util.List;
import java.util.UUID;

import org.jboss.logging.Logger;

import dev.archlens.application.port.in.GetAdrsUseCase;
import dev.archlens.domain.model.Adr;
import dev.archlens.interfaces.rest.dto.response.AdrResponse;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("/v1/projects/{projectId}/analyses/{analysisId}/adrs")
@Produces(MediaType.APPLICATION_JSON)
public class AdrResource {

    private static final Logger LOG = Logger.getLogger(AdrResource.class);

    private final GetAdrsUseCase getAdrsUseCase;

    @Inject
    public AdrResource(GetAdrsUseCase getAdrsUseCase) {
        this.getAdrsUseCase = getAdrsUseCase;
    }

    @GET
    public List<AdrResponse> getAdrs(@PathParam("projectId") UUID projectId,
                                     @PathParam("analysisId") UUID analysisId) {
        LOG.infof("Getting ADRs for analysis %s", analysisId);
        return getAdrsUseCase.getByAnalysisId(analysisId).stream()
                .map(this::toResponse)
                .toList();
    }

    private AdrResponse toResponse(Adr adr) {
        return new AdrResponse(
                adr.getId(),
                adr.getAnalysisId(),
                adr.getTitle(),
                adr.getContext(),
                adr.getDecision(),
                adr.getConsequences(),
                adr.getStatus() != null ? adr.getStatus().name() : null,
                adr.getRelatedFindings(),
                adr.getCreatedAt());
    }
}
