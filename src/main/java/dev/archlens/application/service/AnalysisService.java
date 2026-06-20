package dev.archlens.application.service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.jboss.logging.Logger;

import dev.archlens.application.port.in.CreateAnalysisUseCase;
import dev.archlens.application.port.in.GetAdrsUseCase;
import dev.archlens.application.port.in.GetAnalysisUseCase;
import dev.archlens.application.port.in.ListAnalysesForProjectUseCase;
import dev.archlens.application.port.out.AdrRepositoryPort;
import dev.archlens.application.port.out.AnalysisRepositoryPort;
import dev.archlens.application.port.out.ProjectRepositoryPort;
import dev.archlens.application.port.out.TenantProvider;
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
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

@ApplicationScoped
public class AnalysisService implements CreateAnalysisUseCase, GetAnalysisUseCase, GetAdrsUseCase,
        ListAnalysesForProjectUseCase {

    private static final Logger LOG = Logger.getLogger(AnalysisService.class);

    private final AnalysisRepositoryPort analysisRepository;
    private final ProjectRepositoryPort projectRepository;
    private final AdrRepositoryPort adrRepository;
    private final AnalysisProducer analysisProducer;
    private final TenantProvider tenantProvider;

    @Inject
    public AnalysisService(AnalysisRepositoryPort analysisRepository,
                           ProjectRepositoryPort projectRepository,
                           AdrRepositoryPort adrRepository,
                           AnalysisProducer analysisProducer,
                           TenantProvider tenantProvider) {
        this.analysisRepository = analysisRepository;
        this.projectRepository = projectRepository;
        this.adrRepository = adrRepository;
        this.analysisProducer = analysisProducer;
        this.tenantProvider = tenantProvider;
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
        if (project.getStatus() != ProjectStatus.READY && project.getStatus() != ProjectStatus.UPLOADED) {
            throw new ProjectNotReadyException(projectId, project.getStatus());
        }

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
}
