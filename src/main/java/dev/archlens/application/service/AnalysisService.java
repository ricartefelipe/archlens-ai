package dev.archlens.application.service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.jboss.logging.Logger;

import dev.archlens.application.port.in.CancelAnalysisUseCase;
import dev.archlens.application.port.in.CreateAnalysisUseCase;
import dev.archlens.application.port.in.GetAdrsUseCase;
import dev.archlens.application.port.in.GetAnalysisUseCase;
import dev.archlens.application.port.in.ListAnalysesForProjectUseCase;
import dev.archlens.application.port.out.AdrRepositoryPort;
import dev.archlens.application.port.out.AnalysisRepositoryPort;
import dev.archlens.application.port.out.ProjectRepositoryPort;
import dev.archlens.application.port.out.TenantProvider;
import dev.archlens.domain.exception.AnalysisNotCancellableException;
import dev.archlens.domain.exception.AnalysisNotFoundException;
import dev.archlens.domain.exception.ProjectNotFoundException;
import dev.archlens.domain.exception.ProjectNotReadyException;
import dev.archlens.domain.model.Adr;
import dev.archlens.domain.model.Analysis;
import dev.archlens.domain.model.AnalysisStatus;
import dev.archlens.domain.model.Project;
import dev.archlens.domain.model.ProjectStatus;
import dev.archlens.infrastructure.messaging.AnalysisEvent;
import dev.archlens.infrastructure.messaging.AnalysisProducer;
import dev.archlens.infrastructure.persistence.rls.TenantScopedRls;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

@TenantScopedRls
@ApplicationScoped
public class AnalysisService implements CreateAnalysisUseCase, GetAnalysisUseCase, GetAdrsUseCase,
        ListAnalysesForProjectUseCase, CancelAnalysisUseCase {

    private static final Logger LOG = Logger.getLogger(AnalysisService.class);

    private final AnalysisRepositoryPort analysisRepository;
    private final ProjectRepositoryPort projectRepository;
    private final AdrRepositoryPort adrRepository;
    private final AnalysisProducer analysisProducer;
    private final TenantProvider tenantProvider;
    private final QuotaService quotaService;

    @Inject
    public AnalysisService(AnalysisRepositoryPort analysisRepository,
                           ProjectRepositoryPort projectRepository,
                           AdrRepositoryPort adrRepository,
                           AnalysisProducer analysisProducer,
                           TenantProvider tenantProvider,
                           QuotaService quotaService) {
        this.analysisRepository = analysisRepository;
        this.projectRepository = projectRepository;
        this.adrRepository = adrRepository;
        this.analysisProducer = analysisProducer;
        this.tenantProvider = tenantProvider;
        this.quotaService = quotaService;
    }

    @Override
    @Transactional
    public Analysis create(UUID projectId) {
        String tenantId = tenantProvider.getCurrentTenantId();

        if (!projectRepository.existsByIdAndTenantId(projectId, tenantId)) {
            throw new ProjectNotFoundException(projectId);
        }

        Project project = projectRepository.findByIdAndTenantId(projectId, tenantId)
                .orElseThrow(() -> new ProjectNotFoundException(projectId));
        if (!isReadyForAnalysis(project)) {
            throw new ProjectNotReadyException(projectId, project.getStatus());
        }

        quotaService.checkCanRunAnalysis(tenantId);

        Analysis analysis = new Analysis();
        analysis.setId(UUID.randomUUID());
        analysis.setProjectId(projectId);
        analysis.setTenantId(tenantId);
        analysis.setStatus(AnalysisStatus.PENDING);
        analysis.setCreatedAt(Instant.now());
        analysis.setUpdatedAt(Instant.now());
        analysisRepository.save(analysis);

        LOG.infof("Analysis %s created with status PENDING for project %s", analysis.getId(), projectId);

        analysisProducer.sendAnalysisRequest(
                new AnalysisEvent(analysis.getId(), projectId, tenantId));

        return analysis;
    }

    @Override
    public Analysis getById(UUID projectId, UUID analysisId) {
        String tenantId = tenantProvider.getCurrentTenantId();
        if (!projectRepository.existsByIdAndTenantId(projectId, tenantId)) {
            throw new ProjectNotFoundException(projectId);
        }
        return analysisRepository.findByProjectIdAndId(projectId, analysisId)
                .filter(a -> tenantId.equals(a.getTenantId()))
                .orElseThrow(() -> new AnalysisNotFoundException(analysisId));
    }

    /**
     * Cancela uma análise em andamento. O UPDATE condicional no repositório
     * decide a corrida com a conclusão: quem commitar primeiro vence, e o
     * consumidor descarta resultado tardio de análise cancelada. Cancelar o
     * que já está cancelado é idempotente; estado terminal responde 409.
     */
    @Override
    public Analysis cancel(UUID projectId, UUID analysisId) {
        Analysis analysis = getById(projectId, analysisId);

        if (analysis.getStatus() == AnalysisStatus.CANCELLED) {
            return analysis;
        }

        String summary = "Análise cancelada pelo usuário.";
        Analysis cancelled = analysisRepository
                .cancelIfInProgress(analysisId, Instant.now(), summary)
                .orElse(null);

        if (cancelled == null) {
            AnalysisStatus current = analysisRepository.findCurrentStatusFresh(analysisId)
                    .orElse(analysis.getStatus());
            if (current == AnalysisStatus.CANCELLED) {
                return analysis;
            }
            throw new AnalysisNotCancellableException(analysisId, current);
        }

        LOG.infof("Analysis %s cancelled by user", analysisId);
        return cancelled;
    }

    @Override
    public List<Analysis> listByProject(UUID projectId) {
        String tenantId = tenantProvider.getCurrentTenantId();
        if (!projectRepository.existsByIdAndTenantId(projectId, tenantId)) {
            throw new ProjectNotFoundException(projectId);
        }
        return analysisRepository.findByProjectIdAndTenantId(projectId, tenantId);
    }

    @Override
    public List<Adr> getByAnalysisId(UUID analysisId) {
        String tenantId = tenantProvider.getCurrentTenantId();
        Analysis analysis = analysisRepository.findById(analysisId)
                .filter(a -> tenantId.equals(a.getTenantId()))
                .orElseThrow(() -> new AnalysisNotFoundException(analysisId));
        return adrRepository.findByAnalysisId(analysis.getId());
    }

    private static boolean isReadyForAnalysis(Project project) {
        if (project.getFileCount() <= 0) {
            return false;
        }
        return switch (project.getStatus()) {
            case UPLOADED, INGESTING, READY -> true;
            case CREATED, UPLOADING, FAILED -> false;
        };
    }
}
