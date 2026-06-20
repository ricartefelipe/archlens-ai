package dev.archlens.infrastructure.gateway;

import java.util.List;
import java.util.UUID;

import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.logging.Logger;

import com.fasterxml.jackson.annotation.JsonProperty;

import dev.archlens.application.port.out.IngestGateway;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@ApplicationScoped
public class WorkerAiIngestAdapter implements IngestGateway {

    private static final Logger LOG = Logger.getLogger(WorkerAiIngestAdapter.class);

    private final IngestClient client;

    @Inject
    public WorkerAiIngestAdapter(@RestClient IngestClient client) {
        this.client = client;
    }

    @Override
    public IngestJobStatus triggerIngest(UUID projectId, String tenantId, List<String> filePaths) {
        LOG.infof("Triggering ingest for project %s with %d files", projectId, filePaths.size());
        try {
            var request = new IngestRequestDto(projectId, tenantId, filePaths);
            var response = client.triggerIngest(projectId, request);
            return toStatus(response);
        } catch (Exception e) {
            LOG.errorf(e, "Failed to trigger ingest for project %s", projectId);
            throw new RuntimeException("Ingest trigger failed: " + e.getMessage(), e);
        }
    }

    @Override
    public IngestJobStatus getStatus(UUID projectId) {
        try {
            return toStatus(client.getStatus(projectId));
        } catch (Exception e) {
            LOG.warnf(e, "Failed to fetch ingest status for project %s", projectId);
            throw new RuntimeException("Ingest status unavailable: " + e.getMessage(), e);
        }
    }

    private static IngestJobStatus toStatus(IngestStatusDto dto) {
        return new IngestJobStatus(
                dto.projectId(),
                dto.status(),
                dto.totalFiles(),
                dto.processedFiles(),
                dto.totalChunks());
    }

    @RegisterRestClient(configKey = "worker-ai")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public interface IngestClient {

        @POST
        @Path("/v1/ingest/{projectId}")
        IngestStatusDto triggerIngest(@jakarta.ws.rs.PathParam("projectId") UUID projectId, IngestRequestDto request);

        @GET
        @Path("/v1/ingest/{projectId}/status")
        IngestStatusDto getStatus(@jakarta.ws.rs.PathParam("projectId") UUID projectId);
    }

    record IngestRequestDto(
            @JsonProperty("project_id") UUID projectId,
            @JsonProperty("tenant_id") String tenantId,
            @JsonProperty("file_paths") List<String> filePaths) {
    }

    record IngestStatusDto(
            @JsonProperty("project_id") UUID projectId,
            String status,
            @JsonProperty("total_files") int totalFiles,
            @JsonProperty("processed_files") int processedFiles,
            @JsonProperty("total_chunks") int totalChunks) {
    }
}
