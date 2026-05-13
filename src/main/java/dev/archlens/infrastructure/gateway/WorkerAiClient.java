package dev.archlens.infrastructure.gateway;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

@RegisterRestClient(configKey = "worker-ai")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public interface WorkerAiClient {

    @POST
    @Path("/v1/context")
    ContextResponse buildContext(ContextRequest request);

    @POST
    @Path("/v1/search")
    SearchResponse semanticSearch(SearchRequest request);

    record ContextRequest(
            @JsonProperty("project_id") UUID projectId,
            String query,
            int limit) {
    }

    record ContextResponse(
            String context,
            List<SourceEntry> sources,
            @JsonProperty("total_chunks") int totalChunks) {
    }

    record SourceEntry(
            @JsonProperty("file_path") String filePath,
            @JsonProperty("chunk_index") int chunkIndex,
            String content,
            double score,
            Map<String, Object> metadata) {
    }

    record SearchRequest(
            @JsonProperty("project_id") UUID projectId,
            String query,
            int limit) {
    }

    record SearchResponse(
            @JsonProperty("project_id") UUID projectId,
            String query,
            List<ChunkResult> results,
            @JsonProperty("total_results") int totalResults) {
    }

    record ChunkResult(
            @JsonProperty("file_path") String filePath,
            @JsonProperty("chunk_index") int chunkIndex,
            String content,
            double score,
            Map<String, Object> metadata) {
    }
}
