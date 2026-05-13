package dev.archlens.application.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.jboss.logging.Logger;

import dev.archlens.application.port.in.CreateAnalysisUseCase;
import dev.archlens.application.port.in.GetAdrsUseCase;
import dev.archlens.application.port.in.GetAnalysisUseCase;
import dev.archlens.application.port.out.AdrRepositoryPort;
import dev.archlens.application.port.out.AnalysisGateway;
import dev.archlens.application.port.out.AnalysisRepositoryPort;
import dev.archlens.application.port.out.LlmAnalysisResult;
import dev.archlens.application.port.out.LlmGateway;
import dev.archlens.application.port.out.ProjectRepositoryPort;
import dev.archlens.application.port.out.TenantProvider;
import dev.archlens.domain.exception.AnalysisNotFoundException;
import dev.archlens.domain.exception.ProjectNotFoundException;
import dev.archlens.domain.model.Adr;
import dev.archlens.domain.model.AdrStatus;
import dev.archlens.domain.model.Analysis;
import dev.archlens.domain.model.AnalysisStatus;
import dev.archlens.domain.model.ArchitecturalRisk;
import dev.archlens.domain.model.RiskCategory;
import dev.archlens.domain.model.RiskSeverity;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

@ApplicationScoped
public class AnalysisService implements CreateAnalysisUseCase, GetAnalysisUseCase, GetAdrsUseCase {

    private static final Logger LOG = Logger.getLogger(AnalysisService.class);

    private final AnalysisRepositoryPort analysisRepository;
    private final ProjectRepositoryPort projectRepository;
    private final LlmGateway llmGateway;
    private final AnalysisGateway analysisGateway;
    private final AdrRepositoryPort adrRepository;
    private final TenantProvider tenantProvider;

    @Inject
    public AnalysisService(AnalysisRepositoryPort analysisRepository,
                           ProjectRepositoryPort projectRepository,
                           LlmGateway llmGateway,
                           AnalysisGateway analysisGateway,
                           AdrRepositoryPort adrRepository,
                           TenantProvider tenantProvider) {
        this.analysisRepository = analysisRepository;
        this.projectRepository = projectRepository;
        this.llmGateway = llmGateway;
        this.analysisGateway = analysisGateway;
        this.adrRepository = adrRepository;
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

            AnalysisGateway.AnalysisGatewayResult staticResult = analysisGateway.analyzeProject(projectId, tenantId);

            List<ArchitecturalRisk> risks = new ArrayList<>();

            for (AnalysisGateway.RiskFindingDto finding : staticResult.findings()) {
                ArchitecturalRisk risk = new ArchitecturalRisk();
                risk.setId(finding.id() != null ? finding.id() : UUID.randomUUID());
                risk.setAnalysisId(analysis.getId());
                risk.setTenantId(tenantId);
                risk.setCategory(safeCategory(finding.category()));
                risk.setSeverity(safeSeverity(finding.severity()));
                risk.setTitle(finding.title());
                risk.setDescription(finding.description());
                risk.setFilePath(finding.filePath());
                risk.setEvidence(finding.evidence());
                risk.setSuggestion(finding.suggestion());
                risks.add(risk);
            }

            if (risks.isEmpty()) {
                LlmAnalysisResult fallbackResult = llmGateway.analyzeProject("Project " + projectId);
                for (var finding : fallbackResult.findings()) {
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
                    risks.add(risk);
                }
                analysis.setSummary(fallbackResult.summary());
            } else {
                analysis.setSummary(staticResult.summary());
            }

            analysis.setRisks(risks);
            analysis.setStatus(AnalysisStatus.COMPLETED);
            analysis.setUpdatedAt(Instant.now());
            analysisRepository.save(analysis);

            LOG.infof("Analysis %s COMPLETED with %d risks", analysis.getId(), risks.size());

            generateAndSaveAdrs(analysis, staticResult.findings(), tenantId);

        } catch (Exception e) {
            LOG.errorf(e, "Analysis %s failed: %s", analysis.getId(), e.getMessage());
            analysis.setStatus(AnalysisStatus.FAILED);
            analysis.setSummary(e.getMessage());
            analysis.setUpdatedAt(Instant.now());
            analysisRepository.save(analysis);
        }

        return analysis;
    }

    private void generateAndSaveAdrs(Analysis analysis, List<AnalysisGateway.RiskFindingDto> findings, String tenantId) {
        try {
            List<AnalysisGateway.AdrSuggestion> adrSuggestions =
                    analysisGateway.generateAdrs(analysis.getProjectId(), findings);

            List<Adr> adrs = adrSuggestions.stream()
                    .map(s -> {
                        Adr adr = new Adr();
                        adr.setId(s.id() != null ? s.id() : UUID.randomUUID());
                        adr.setAnalysisId(analysis.getId());
                        adr.setTenantId(tenantId);
                        adr.setTitle(s.title());
                        adr.setContext(s.context());
                        adr.setDecision(s.decision());
                        adr.setConsequences(s.consequences());
                        adr.setStatus(AdrStatus.PROPOSED);
                        adr.setRelatedFindings(s.relatedFindings());
                        adr.setCreatedAt(Instant.now());
                        return adr;
                    })
                    .toList();

            if (!adrs.isEmpty()) {
                adrRepository.saveAll(adrs);
                LOG.infof("Generated %d ADRs for analysis %s", adrs.size(), analysis.getId());
            }
        } catch (Exception e) {
            LOG.warnf(e, "ADR generation failed for analysis %s", analysis.getId());
        }
    }

    @Override
    public Analysis getById(UUID projectId, UUID analysisId) {
        return analysisRepository.findByProjectIdAndId(projectId, analysisId)
                .orElseThrow(() -> new AnalysisNotFoundException(analysisId));
    }

    @Override
    public List<Adr> getByAnalysisId(UUID analysisId) {
        return adrRepository.findByAnalysisId(analysisId);
    }

    private RiskCategory safeCategory(String category) {
        try {
            return RiskCategory.valueOf(category);
        } catch (IllegalArgumentException e) {
            return RiskCategory.LAYER_SEPARATION_ISSUE;
        }
    }

    private RiskSeverity safeSeverity(String severity) {
        try {
            return RiskSeverity.valueOf(severity);
        } catch (IllegalArgumentException e) {
            return RiskSeverity.MEDIUM;
        }
    }
}
