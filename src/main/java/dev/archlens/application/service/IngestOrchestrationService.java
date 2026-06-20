package dev.archlens.application.service;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import dev.archlens.application.port.out.IngestGateway;
import dev.archlens.application.port.out.IngestGateway.IngestJobStatus;
import dev.archlens.application.port.out.ProjectRepositoryPort;
import dev.archlens.domain.model.ProjectStatus;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

@ApplicationScoped
public class IngestOrchestrationService {

    private static final Logger LOG = Logger.getLogger(IngestOrchestrationService.class);
    private static final Duration POLL_INTERVAL = Duration.ofSeconds(2);
    private static final int MAX_POLLS = 300;

    private final IngestGateway ingestGateway;
    private final ProjectRepositoryPort projectRepository;

    @ConfigProperty(name = "archlens.ingest.async", defaultValue = "true")
    boolean asyncIngest;

    @Inject
    public IngestOrchestrationService(IngestGateway ingestGateway,
                                      ProjectRepositoryPort projectRepository) {
        this.ingestGateway = ingestGateway;
        this.projectRepository = projectRepository;
    }

    public void startIngest(UUID projectId, String tenantId, List<String> filePaths) {
        markIngesting(projectId);

        if (filePaths.isEmpty()) {
            markReady(projectId);
            return;
        }

        try {
            ingestGateway.triggerIngest(projectId, tenantId, filePaths);
        } catch (Exception e) {
            LOG.errorf(e, "Ingest trigger failed for project %s", projectId);
            markFailed(projectId);
            return;
        }

        if (asyncIngest) {
            Thread.ofVirtual().name("ingest-poll-" + projectId).start(() -> pollUntilDone(projectId));
        } else {
            pollUntilDone(projectId);
        }
    }

    private void pollUntilDone(UUID projectId) {
        for (int attempt = 0; attempt < MAX_POLLS; attempt++) {
            try {
                Thread.sleep(POLL_INTERVAL.toMillis());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                markFailed(projectId);
                return;
            }

            try {
                IngestJobStatus status = ingestGateway.getStatus(projectId);
                if (isTerminal(status.status())) {
                    if ("completed".equalsIgnoreCase(status.status())) {
                        markReady(projectId);
                        LOG.infof("Ingest completed for project %s (%d chunks)", projectId, status.totalChunks());
                    } else {
                        markFailed(projectId);
                        LOG.warnf("Ingest ended with status %s for project %s", status.status(), projectId);
                    }
                    return;
                }
            } catch (Exception e) {
                LOG.warnf(e, "Ingest poll failed for project %s (attempt %d)", projectId, attempt + 1);
            }
        }

        LOG.errorf("Ingest polling timed out for project %s", projectId);
        markFailed(projectId);
    }

    private static boolean isTerminal(String status) {
        if (status == null) {
            return false;
        }
        return switch (status.toLowerCase()) {
            case "completed", "failed" -> true;
            default -> false;
        };
    }

    @Transactional
    void markIngesting(UUID projectId) {
        updateStatus(projectId, ProjectStatus.INGESTING);
    }

    @Transactional
    void markReady(UUID projectId) {
        updateStatus(projectId, ProjectStatus.READY);
    }

    @Transactional
    void markFailed(UUID projectId) {
        updateStatus(projectId, ProjectStatus.FAILED);
    }

    private void updateStatus(UUID projectId, ProjectStatus status) {
        projectRepository.findById(projectId).ifPresent(project -> {
            project.setStatus(status);
            projectRepository.save(project);
        });
    }
}
