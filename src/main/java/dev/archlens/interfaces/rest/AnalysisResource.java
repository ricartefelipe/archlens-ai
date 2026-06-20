package dev.archlens.interfaces.rest;

import java.util.List;
import java.util.UUID;

import dev.archlens.application.port.in.CompareAnalysesUseCase;
import dev.archlens.application.port.in.CreateAnalysisUseCase;
import dev.archlens.application.port.in.GetAnalysisUseCase;
import dev.archlens.application.port.in.ListAnalysesForProjectUseCase;
import dev.archlens.domain.model.Analysis;
import dev.archlens.domain.model.AnalysisComparisonResult;
import dev.archlens.interfaces.rest.dto.response.AnalysisComparisonResponse;
import dev.archlens.interfaces.rest.dto.response.AnalysisResponse;
import dev.archlens.interfaces.rest.mapper.AnalysisDtoMapper;
import dev.archlens.interfaces.rest.mapper.ComparisonDtoMapper;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
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
    private final ListAnalysesForProjectUseCase listUseCase;
    private final CompareAnalysesUseCase compareUseCase;
    private final AnalysisDtoMapper mapper;
    private final ComparisonDtoMapper comparisonMapper;

    @Inject
    public AnalysisResource(CreateAnalysisUseCase createUseCase,
                            GetAnalysisUseCase getUseCase,
                            ListAnalysesForProjectUseCase listUseCase,
                            CompareAnalysesUseCase compareUseCase,
                            AnalysisDtoMapper mapper,
                            ComparisonDtoMapper comparisonMapper) {
        this.createUseCase = createUseCase;
        this.getUseCase = getUseCase;
        this.listUseCase = listUseCase;
        this.compareUseCase = compareUseCase;
        this.mapper = mapper;
        this.comparisonMapper = comparisonMapper;
    }

    @GET
    public List<AnalysisResponse> list(@PathParam("projectId") UUID projectId) {
        LOG.infof("Listing analyses for projectId=%s", projectId);
        return listUseCase.listByProject(projectId).stream()
                .map(mapper::toResponse)
                .toList();
    }

    @POST
    public Response create(@PathParam("projectId") UUID projectId) {
        LOG.infof("Creating analysis for project: projectId=%s", projectId);
        Analysis analysis = createUseCase.create(projectId);
        AnalysisResponse response = mapper.toResponse(analysis);
        return Response.status(Response.Status.ACCEPTED).entity(response).build();
    }

    @GET
    @Path("/compare")
    public AnalysisComparisonResponse compare(@PathParam("projectId") UUID projectId,
                                              @QueryParam("baseline") UUID baselineAnalysisId,
                                              @QueryParam("current") UUID currentAnalysisId) {
        LOG.infof("Comparing analyses projectId=%s baseline=%s current=%s",
                projectId, baselineAnalysisId, currentAnalysisId);
        AnalysisComparisonResult result = compareUseCase.compare(
                projectId, baselineAnalysisId, currentAnalysisId);
        return comparisonMapper.toResponse(result);
    }

    @GET
    @Path("/{analysisId}")
    public AnalysisResponse getById(@PathParam("projectId") UUID projectId,
                                    @PathParam("analysisId") UUID analysisId) {
        LOG.infof("Fetching analysis: projectId=%s, analysisId=%s", projectId, analysisId);
        return mapper.toResponse(getUseCase.getById(projectId, analysisId));
    }
}
