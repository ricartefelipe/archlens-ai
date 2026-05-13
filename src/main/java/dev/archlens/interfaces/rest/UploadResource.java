package dev.archlens.interfaces.rest;

import java.io.InputStream;
import java.util.UUID;

import org.jboss.logging.Logger;
import org.jboss.resteasy.reactive.multipart.FileUpload;

import dev.archlens.application.port.in.UploadProjectUseCase;
import dev.archlens.domain.model.Project;
import dev.archlens.interfaces.rest.dto.response.ProjectResponse;
import dev.archlens.interfaces.rest.mapper.ProjectDtoMapper;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/v1/projects/{projectId}/upload")
@Produces(MediaType.APPLICATION_JSON)
public class UploadResource {

    private static final Logger LOG = Logger.getLogger(UploadResource.class);

    private final UploadProjectUseCase uploadUseCase;
    private final ProjectDtoMapper mapper;

    @Inject
    public UploadResource(UploadProjectUseCase uploadUseCase, ProjectDtoMapper mapper) {
        this.uploadUseCase = uploadUseCase;
        this.mapper = mapper;
    }

    @POST
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    public Response upload(@PathParam("projectId") UUID projectId,
                           @org.jboss.resteasy.reactive.RestForm("file") FileUpload file) {
        LOG.infof("Receiving upload for project %s: %s", projectId, file.fileName());

        try (InputStream is = java.nio.file.Files.newInputStream(file.uploadedFile())) {
            Project project = uploadUseCase.upload(projectId, file.fileName(), is);
            ProjectResponse response = mapper.toResponse(project);
            return Response.ok(response).build();
        } catch (Exception e) {
            throw new RuntimeException("Upload processing failed: " + e.getMessage(), e);
        }
    }
}
