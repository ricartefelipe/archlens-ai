package dev.archlens.application.service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import org.jboss.logging.Logger;

import dev.archlens.application.port.in.CreateAnalysisUseCase;
import dev.archlens.application.port.in.GetAnalysisUseCase;
import dev.archlens.application.port.out.AnalysisRepositoryPort;
import dev.archlens.application.port.out.LlmAnalysisResult;
import dev.archlens.application.port.out.LlmGateway;
import dev.archlens.application.port.out.ProjectRepositoryPort;
import dev.archlens.application.port.out.TenantProvider;
import dev.archlens.domain.exception.AnalysisNotFoundException;
import dev.archlens.domain.exception.ProjectNotFoundException;
import dev.archlens.domain.model.Analysis;
import dev.archlens.domain.model.AnalysisStatus;
import dev.archlens.domain.model.ArchitecturalRisk;

@ApplicationScoped
public class AnalysisService implements CreateAnalysisUseCase, GetAnalysisUseCase {

    private static final Logger LOG = Logger.getLogger(AnalysisService.class);

    private final AnalysisRepositoryPort analysisRepository;
    private final ProjectRepositoryPort projectRepository;
    private final LlmGateway llmGateway;
    private final TenantProvider tenantProvider;

    public AnalysisService(AnalysisRepositoryPort analysisRepository,
                           ProjectRepositoryPort projectRepository,
                           LlmGateway llmGateway,
                           TenantProvider tenantProvider) {
        this.analysisRepository = analysisRepository;
        this.projectRepository = projectRepository;
        this.llmGateway = llmGateway;
        this.tenantProvider = tenantProvider;
    }

    @Override
    @Transactional
    public Analysis create(UUID projectId) {
        String tenantId = tenantProvider.getCurrentTenantId();

        if (!projectRepository.existsById(projectId)) {
            throw new ProjectNotFoundException(projectId);
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

        try {
            analysis.setStatus(AnalysisStatus.PROCESSING);
            analysis.setUpdatedAt(Instant.now());
            analysisRepository.save(analysis);
            LOG.infof("Analysis %s transitioned to PROCESSING", analysis.getId());

            LlmAnalysisResult result = llmGateway.analyzeProject("Project " + projectId);

            List<ArchitecturalRisk> risks = result.findings().stream()
                    .map(finding -> {
                        ArchitecturalRisk risk = new ArchitecturalRisk();
                        risk.setId(UUID.randomUUID());
                        risk.setAnalysisId(analysis.getId());
                        risk.setTenantId(tenantId);
                        risk.setCategory(finding.category());
                        risk.setSeverity(finding.severity());
                        risk.setTitle(finding.title());
                        risk.setDescription(finding.description());
                        risk.setFilePath(finding.filePath());
                        risk.setEvidence(finding.evidence());
                        risk.setSuggestion(finding.suggestion());
                        return risk;
                    })
                    .toList();

            analysis.setSummary(result.summary());
            analysis.setRisks(risks);
            analysis.setStatus(AnalysisStatus.COMPLETED);
            analysis.setUpdatedAt(Instant.now());
            analysisRepository.save(analysis);

            LOG.infof("Analysis %s transitioned to COMPLETED with %d risks", analysis.getId(), risks.size());
        } catch (Exception e) {
            LOG.errorf(e, "Analysis %s failed: %s", analysis.getId(), e.getMessage());

            analysis.setStatus(AnalysisStatus.FAILED);
            analysis.setSummary(e.getMessage());
            analysis.setUpdatedAt(Instant.now());
            analysisRepository.save(analysis);
        }

        return analysis;
    }

    @Override
    public Analysis getById(UUID projectId, UUID analysisId) {
        return analysisRepository.findByProjectIdAndId(projectId, analysisId)
                .orElseThrow(() -> new AnalysisNotFoundException(analysisId));
    }
}
