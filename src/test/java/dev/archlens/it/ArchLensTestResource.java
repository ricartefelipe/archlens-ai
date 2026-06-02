package dev.archlens.it;

import java.util.HashMap;
import java.util.Map;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Sobe PostgreSQL com pgvector e RabbitMQ em containers efêmeros para os testes
 * de integração, substituindo as conexões locais sem alterar a configuração de
 * dev/prod.
 */
public class ArchLensTestResource implements QuarkusTestResourceLifecycleManager {

    private static final DockerImageName POSTGRES_IMAGE =
            DockerImageName.parse("pgvector/pgvector:pg16").asCompatibleSubstituteFor("postgres");
    private static final DockerImageName RABBITMQ_IMAGE =
            DockerImageName.parse("rabbitmq:3.13-management-alpine");

    private PostgreSQLContainer<?> postgres;
    private RabbitMQContainer rabbitmq;

    @Override
    public Map<String, String> start() {
        postgres = new PostgreSQLContainer<>(POSTGRES_IMAGE)
                .withDatabaseName("archlens")
                .withUsername("archlens")
                .withPassword("archlens");
        postgres.start();

        rabbitmq = new RabbitMQContainer(RABBITMQ_IMAGE);
        rabbitmq.start();

        Map<String, String> config = new HashMap<>();
        config.put("quarkus.datasource.jdbc.url", postgres.getJdbcUrl());
        config.put("quarkus.datasource.username", postgres.getUsername());
        config.put("quarkus.datasource.password", postgres.getPassword());
        config.put("rabbitmq-host", rabbitmq.getHost());
        config.put("rabbitmq-port", String.valueOf(rabbitmq.getAmqpPort()));
        config.put("rabbitmq-username", rabbitmq.getAdminUsername());
        config.put("rabbitmq-password", rabbitmq.getAdminPassword());
        return config;
    }

    @Override
    public void stop() {
        if (rabbitmq != null) {
            rabbitmq.stop();
        }
        if (postgres != null) {
            postgres.stop();
        }
    }
}
