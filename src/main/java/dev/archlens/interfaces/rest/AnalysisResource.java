package dev.archlens.interfaces.rest;

import java.util.UUID;

import dev.archlens.application.port.in.CreateAnalysisUseCase;
import dev.archlens.application.port.in.GetAnalysisUseCase;
import dev.archlens.domain.model.Analysis;
import dev.archlens.interfaces.rest.dto.response.AnalysisResponse;
import dev.archlens.interfaces.rest.mapper.AnalysisDtoMapper;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.jboss.logging.Logger;

@Path("/v1/projects/{projectId}/analyses")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class AnalysisResource {

    private static final Logger LOG = Logger.getLogger(AnalysisResource.class);

    private final CreateAnalysisUseCase createUseCase;
    private final GetAnalysisUseCase getUseCase;
    private final AnalysisDtoMapper mapper;

    @Inject
    public AnalysisResource(CreateAnalysisUseCase createUseCase,
                            GetAnalysisUseCase getUseCase,
                            AnalysisDtoMapper mapper) {
        this.createUseCase = createUseCase;
        this.getUseCase = getUseCase;
        this.mapper = mapper;
    }

    @POST
    public Response create(@PathParam("projectId") UUID projectId) {
        LOG.infof("Creating analysis for project: projectId=%s", projectId);
        Analysis analysis = createUseCase.create(projectId);
        AnalysisResponse response = mapper.toResponse(analysis);
        return Response.status(Response.Status.CREATED).entity(response).build();
    }

    @GET
    @Path("/{analysisId}")
    public AnalysisResponse getById(@PathParam("projectId") UUID projectId,
                                    @PathParam("analysisId") UUID analysisId) {
        LOG.infof("Fetching analysis: projectId=%s, analysisId=%s", projectId, analysisId);
        return mapper.toResponse(getUseCase.getById(projectId, analysisId));
    }
}
