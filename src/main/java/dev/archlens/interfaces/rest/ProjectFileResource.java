package dev.archlens.interfaces.rest;

import java.util.List;
import java.util.UUID;

import org.jboss.logging.Logger;

import dev.archlens.application.port.in.ListProjectFilesUseCase;
import dev.archlens.domain.model.ProjectFile;
import dev.archlens.interfaces.rest.dto.response.ProjectFileResponse;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("/v1/projects/{projectId}/files")
@Produces(MediaType.APPLICATION_JSON)
public class ProjectFileResource {

    private static final Logger LOG = Logger.getLogger(ProjectFileResource.class);

    private final ListProjectFilesUseCase listFilesUseCase;

    @Inject
    public ProjectFileResource(ListProjectFilesUseCase listFilesUseCase) {
        this.listFilesUseCase = listFilesUseCase;
    }

    @GET
    public List<ProjectFileResponse> listFiles(@PathParam("projectId") UUID projectId) {
        LOG.infof("Listing files for project %s", projectId);
        return listFilesUseCase.listByProjectId(projectId).stream()
                .map(this::toResponse)
                .toList();
    }

    private ProjectFileResponse toResponse(ProjectFile f) {
        return new ProjectFileResponse(
                f.getId(),
                f.getProjectId(),
                f.getFilePath(),
                f.getFileType() != null ? f.getFileType().name() : null,
                f.getFileType() != null ? f.getFileType().getLabel() : null,
                f.getSizeBytes(),
                f.getContentHash(),
                f.getCreatedAt());
    }
}
