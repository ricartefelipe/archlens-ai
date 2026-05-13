package dev.archlens.interfaces.rest;

import java.util.List;
import java.util.UUID;

import dev.archlens.application.port.in.CreateProjectUseCase;
import dev.archlens.application.port.in.GetProjectUseCase;
import dev.archlens.application.port.in.ListProjectsUseCase;
import dev.archlens.domain.model.Project;
import dev.archlens.interfaces.rest.dto.request.CreateProjectRequest;
import dev.archlens.interfaces.rest.dto.response.ProjectResponse;
import dev.archlens.interfaces.rest.mapper.ProjectDtoMapper;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.jboss.logging.Logger;

@Path("/v1/projects")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ProjectResource {

    private static final Logger LOG = Logger.getLogger(ProjectResource.class);

    private final CreateProjectUseCase createUseCase;
    private final ListProjectsUseCase listUseCase;
    private final GetProjectUseCase getUseCase;
    private final ProjectDtoMapper mapper;

    @Inject
    public ProjectResource(CreateProjectUseCase createUseCase,
                           ListProjectsUseCase listUseCase,
                           GetProjectUseCase getUseCase,
                           ProjectDtoMapper mapper) {
        this.createUseCase = createUseCase;
        this.listUseCase = listUseCase;
        this.getUseCase = getUseCase;
        this.mapper = mapper;
    }

    @POST
    public Response create(@Valid CreateProjectRequest request) {
        LOG.infof("Creating project: name=%s", request.name());
        Project project = createUseCase.create(request.name(), request.description());
        ProjectResponse response = mapper.toResponse(project);
        return Response.status(Response.Status.CREATED).entity(response).build();
    }

    @GET
    public List<ProjectResponse> listAll() {
        LOG.debug("Listing all projects");
        return mapper.toResponseList(listUseCase.listAll());
    }

    @GET
    @Path("/{projectId}")
    public ProjectResponse getById(@PathParam("projectId") UUID projectId) {
        LOG.infof("Fetching project: id=%s", projectId);
        return mapper.toResponse(getUseCase.getById(projectId));
    }
}
