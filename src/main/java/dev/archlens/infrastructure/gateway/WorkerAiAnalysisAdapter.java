package dev.archlens.infrastructure.gateway;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonProperty;

import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.logging.Logger;

import dev.archlens.application.port.out.AnalysisGateway;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

@ApplicationScoped
public class WorkerAiAnalysisAdapter implements AnalysisGateway {

    private static final Logger LOG = Logger.getLogger(WorkerAiAnalysisAdapter.class);

    private final AnalysisClient client;

    @Inject
    public WorkerAiAnalysisAdapter(@RestClient AnalysisClient client) {
        this.client = client;
    }

    @Override
    public AnalysisGatewayResult analyzeProject(UUID projectId, String tenantId) {
        LOG.infof("Requesting static analysis for project %s", projectId);
        try {
            var request = new AnalyzeDto(projectId, tenantId);
            var response = client.analyze(projectId, request);

            List<RiskFindingDto> findings = response.findings().stream()
                    .map(f -> new RiskFindingDto(
                            f.id(), f.category(), f.severity(), f.title(),
                            f.description(), f.filePath(), f.evidence(), f.suggestion()))
                    .toList();

            return new AnalysisGatewayResult(response.summary(), findings, response.totalFilesAnalyzed());
        } catch (Exception e) {
            LOG.warnf(e, "Static analysis via worker failed, returning empty result");
            return new AnalysisGatewayResult("Análise estática indisponível", List.of(), 0);
        }
    }

    @Override
    public List<AdrSuggestion> generateAdrs(UUID projectId, List<RiskFindingDto> findings) {
        LOG.infof("Requesting ADR generation for project %s with %d findings", projectId, findings.size());
        try {
            var findingDtos = findings.stream()
                    .map(f -> new FindingDto(f.id(), f.category(), f.severity(), f.title(),
                            f.description(), f.filePath(), f.evidence(), f.suggestion()))
                    .toList();
            var request = new AdrRequestDto(projectId, findingDtos);
            var adrs = client.generateAdrs(projectId, request);

            return adrs.stream()
                    .map(a -> new AdrSuggestion(a.id(), a.title(), a.context(), a.decision(),
                            a.consequences(), a.status(), a.relatedFindings()))
                    .toList();
        } catch (Exception e) {
            LOG.warnf(e, "ADR generation via worker failed");
            return List.of();
        }
    }

    @RegisterRestClient(configKey = "worker-ai")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public interface AnalysisClient {

        @POST
        @Path("/v1/analyze/{projectId}")
        AnalysisResponseDto analyze(@jakarta.ws.rs.PathParam("projectId") UUID projectId, AnalyzeDto request);

        @POST
        @Path("/v1/analyze/{projectId}/adrs")
        List<AdrResponseDto> generateAdrs(@jakarta.ws.rs.PathParam("projectId") UUID projectId, AdrRequestDto request);
    }

    record AnalyzeDto(@JsonProperty("project_id") UUID projectId, @JsonProperty("tenant_id") String tenantId) {}

    record AnalysisResponseDto(
            @JsonProperty("project_id") UUID projectId,
            String summary,
            List<FindingDto> findings,
            @JsonProperty("total_files_analyzed") int totalFilesAnalyzed) {}

    record FindingDto(
            UUID id, String category, String severity, String title,
            String description,
            @JsonProperty("file_path") String filePath,
            String evidence, String suggestion) {}

    record AdrRequestDto(@JsonProperty("project_id") UUID projectId, List<FindingDto> findings) {}

    record AdrResponseDto(
            UUID id, String title, String context, String decision,
            String consequences, String status,
            @JsonProperty("related_findings") List<UUID> relatedFindings) {}
}
