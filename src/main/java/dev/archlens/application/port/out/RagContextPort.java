package dev.archlens.application.port.out;

import java.util.List;
import java.util.UUID;

public interface RagContextPort {

    RagContext retrieveContext(UUID projectId, String query, int maxChunks);

    record RagContext(
            String assembledContext,
            List<SourceReference> sources,
            int totalChunks) {
    }

    record SourceReference(
            String filePath,
            int chunkIndex,
            String snippet,
            double score) {
    }
}
