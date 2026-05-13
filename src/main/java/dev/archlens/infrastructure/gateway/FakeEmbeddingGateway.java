package dev.archlens.infrastructure.gateway;

import java.util.Random;

import org.jboss.logging.Logger;

import dev.archlens.application.port.out.EmbeddingGateway;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class FakeEmbeddingGateway implements EmbeddingGateway {

    private static final Logger LOG = Logger.getLogger(FakeEmbeddingGateway.class);
    private static final int EMBEDDING_DIMENSION = 1536;

    @Override
    public float[] generate(String text) {
        LOG.infof("FakeEmbeddingGateway: gerando embedding fake para texto de %d caracteres", text.length());

        Random random = new Random(text.hashCode());
        float[] embedding = new float[EMBEDDING_DIMENSION];
        for (int i = 0; i < EMBEDDING_DIMENSION; i++) {
            embedding[i] = random.nextFloat() * 2.0f - 1.0f;
        }

        return embedding;
    }
}
