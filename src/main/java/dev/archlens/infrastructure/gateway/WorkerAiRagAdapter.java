package dev.archlens.infrastructure.gateway;

import java.util.List;
import java.util.UUID;

import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.logging.Logger;

import dev.archlens.application.port.out.RagContextPort;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class WorkerAiRagAdapter implements RagContextPort {

    private static final Logger LOG = Logger.getLogger(WorkerAiRagAdapter.class);

    private final WorkerAiClient client;

    @Inject
    public WorkerAiRagAdapter(@RestClient WorkerAiClient client) {
        this.client = client;
    }

    @Override
    public RagContext retrieveContext(UUID projectId, String query, int maxChunks) {
        LOG.infof("Retrieving RAG context for project %s: %s", projectId, query);

        try {
            var request = new WorkerAiClient.ContextRequest(projectId, query, maxChunks);
            var response = client.buildContext(request);

            List<SourceReference> sources = response.sources().stream()
                    .map(s -> new SourceReference(
                            s.filePath(),
                            s.chunkIndex(),
                            truncate(s.content(), 200),
                            s.score()))
                    .toList();

            LOG.infof("RAG context retrieved: %d chunks, %d sources", response.totalChunks(), sources.size());
            return new RagContext(response.context(), sources, response.totalChunks());
        } catch (Exception e) {
            LOG.warnf(e, "Failed to retrieve RAG context, falling back to empty context");
            return new RagContext("", List.of(), 0);
        }
    }

    private String truncate(String text, int maxLength) {
        if (text == null || text.length() <= maxLength) {
            return text;
        }
        return text.substring(0, maxLength) + "...";
    }
}
