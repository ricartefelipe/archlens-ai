package dev.archlens.infrastructure.messaging;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import dev.archlens.application.port.out.AdrRepositoryPort;
import dev.archlens.application.port.out.AnalysisGateway;
import dev.archlens.application.port.out.AnalysisRepositoryPort;
import dev.archlens.application.port.out.LlmAnalysisResult;
import dev.archlens.application.port.out.LlmGateway;
import dev.archlens.domain.model.Adr;
import dev.archlens.domain.model.AdrStatus;
import dev.archlens.domain.model.Analysis;
import dev.archlens.domain.model.AnalysisStatus;
import dev.archlens.domain.model.ArchitecturalRisk;
import dev.archlens.domain.model.RiskCategory;
import dev.archlens.domain.model.RiskSeverity;
import io.smallrye.common.annotation.Blocking;
import io.vertx.core.json.JsonObject;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

@ApplicationScoped
public class AnalysisConsumer {

    private static final Logger LOG = Logger.getLogger(AnalysisConsumer.class);

    private final AnalysisRepositoryPort analysisRepository;
    private final AnalysisGateway analysisGateway;
    private final LlmGateway llmGateway;
    private final AdrRepositoryPort adrRepository;

    @ConfigProperty(name = "archlens.analysis.llm-fallback-enabled", defaultValue = "true")
    boolean llmFallbackEnabled;

    @Inject
    public AnalysisConsumer(AnalysisRepositoryPort analysisRepository,
                            AnalysisGateway analysisGateway,
                            LlmGateway llmGateway,
                            AdrRepositoryPort adrRepository) {
        this.analysisRepository = analysisRepository;
        this.analysisGateway = analysisGateway;
        this.llmGateway = llmGateway;
        this.adrRepository = adrRepository;
    }

    @Incoming("analysis-requests-in")
    @Blocking
    @Transactional
    public void processAnalysis(JsonObject payload) {
        LOG.infof("Received analysis message: %s", payload.encode());

        AnalysisEvent event = new AnalysisEvent(
                UUID.fromString(payload.getString("analysisId")),
                UUID.fromString(payload.getString("projectId")),
                payload.getString("tenantId"));

        LOG.infof("Processing analysis event: analysisId=%s, projectId=%s", event.analysisId(), event.projectId());

        Analysis analysis = analysisRepository.findById(event.analysisId())
                .orElse(null);

        if (analysis == null) {
            LOG.warnf("Analysis %s not found, skipping", event.analysisId());
            return;
        }

        try {
            analysis.setStatus(AnalysisStatus.PROCESSING);
            analysis.setUpdatedAt(Instant.now());
            analysisRepository.save(analysis);

            AnalysisGateway.AnalysisGatewayResult staticResult =
                    analysisGateway.analyzeProject(event.projectId(), event.tenantId());

            List<ArchitecturalRisk> risks = new ArrayList<>();
            for (AnalysisGateway.RiskFindingDto finding : staticResult.findings()) {
                ArchitecturalRisk risk = new ArchitecturalRisk();
                risk.setId(finding.id() != null ? finding.id() : UUID.randomUUID());
                risk.setAnalysisId(analysis.getId());
                risk.setTenantId(event.tenantId());
                risk.setCategory(safeCategory(finding.category()));
                risk.setSeverity(safeSeverity(finding.severity()));
                risk.setTitle(finding.title());
                risk.setDescription(finding.description());
                risk.setFilePath(finding.filePath());
                risk.setEvidence(finding.evidence());
                risk.setSuggestion(finding.suggestion());
                risks.add(risk);
            }

            if (risks.isEmpty() && llmFallbackEnabled) {
                LlmAnalysisResult fallback = llmGateway.analyzeProject("Project " + event.projectId());
                for (var finding : fallback.findings()) {
                    ArchitecturalRisk risk = new ArchitecturalRisk();
                    risk.setId(UUID.randomUUID());
                    risk.setAnalysisId(analysis.getId());
                    risk.setTenantId(event.tenantId());
                    risk.setCategory(finding.category());
                    risk.setSeverity(finding.severity());
                    risk.setTitle(finding.title());
                    risk.setDescription(finding.description());
                    risk.setFilePath(finding.filePath());
                    risk.setEvidence(finding.evidence());
                    risk.setSuggestion(finding.suggestion());
                    risks.add(risk);
                }
                analysis.setSummary(fallback.summary());
            } else if (risks.isEmpty()) {
                analysis.setSummary(staticResult.summary() != null
                        ? staticResult.summary()
                        : "Nenhum risco identificado pela análise estática.");
            } else {
                analysis.setSummary(staticResult.summary());
            }

            analysis.setRisks(risks);
            analysis.setStatus(AnalysisStatus.COMPLETED);
            analysis.setUpdatedAt(Instant.now());
            analysisRepository.save(analysis);

            LOG.infof("Analysis %s COMPLETED with %d risks (async)", analysis.getId(), risks.size());

            generateAdrs(analysis, staticResult.findings(), event.tenantId());

        } catch (Exception e) {
            LOG.errorf(e, "Async analysis %s failed", event.analysisId());
            analysis.setStatus(AnalysisStatus.FAILED);
            analysis.setSummary(e.getMessage());
            analysis.setUpdatedAt(Instant.now());
            analysisRepository.save(analysis);
        }
    }

    private void generateAdrs(Analysis analysis, List<AnalysisGateway.RiskFindingDto> findings, String tenantId) {
        try {
            List<AnalysisGateway.AdrSuggestion> suggestions =
                    analysisGateway.generateAdrs(analysis.getProjectId(), findings);

            List<Adr> adrs = suggestions.stream()
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
            }
        } catch (Exception e) {
            LOG.warnf(e, "ADR generation failed for analysis %s", analysis.getId());
        }
    }

    private RiskCategory safeCategory(String category) {
        try { return RiskCategory.valueOf(category); }
        catch (IllegalArgumentException e) { return RiskCategory.LAYER_SEPARATION_ISSUE; }
    }

    private RiskSeverity safeSeverity(String severity) {
        try { return RiskSeverity.valueOf(severity); }
        catch (IllegalArgumentException e) { return RiskSeverity.MEDIUM; }
    }
}
