package dev.archlens.infrastructure.gateway;

import java.util.Random;

import org.jboss.logging.Logger;

import dev.archlens.application.port.out.EmbeddingGateway;
import dev.archlens.infrastructure.config.ArchlensEmbeddingConfig;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class LocalEmbeddingGateway implements EmbeddingGateway {

    private static final Logger LOG = Logger.getLogger(LocalEmbeddingGateway.class);

    private final int embeddingDimension;

    @Inject
    public LocalEmbeddingGateway(ArchlensEmbeddingConfig embeddingConfig) {
        this.embeddingDimension = embeddingConfig.dimension();
    }

    @Override
    public float[] generate(String text) {
        LOG.debugf("LocalEmbeddingGateway: vetor determinístico (%d chars)", text.length());

        Random random = new Random(text.hashCode());
        float[] embedding = new float[embeddingDimension];
        for (int i = 0; i < embeddingDimension; i++) {
            embedding[i] = random.nextFloat() * 2.0f - 1.0f;
        }

        return embedding;
    }
}
