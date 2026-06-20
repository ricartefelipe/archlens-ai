package dev.archlens.application.service;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import dev.archlens.application.port.in.CompareAnalysesUseCase;
import dev.archlens.application.port.out.AnalysisRepositoryPort;
import dev.archlens.application.port.out.ProjectRepositoryPort;
import dev.archlens.application.port.out.TenantProvider;
import dev.archlens.domain.exception.AnalysisNotComparableException;
import dev.archlens.domain.exception.AnalysisNotFoundException;
import dev.archlens.domain.exception.ProjectNotFoundException;
import dev.archlens.domain.model.Analysis;
import dev.archlens.domain.model.AnalysisComparisonResult;
import dev.archlens.domain.model.AnalysisComparisonResult.AnalysisRef;
import dev.archlens.domain.model.AnalysisComparisonResult.SeverityChangedRisk;
import dev.archlens.domain.model.AnalysisStatus;
import dev.archlens.domain.model.ArchitecturalRisk;
import dev.archlens.domain.model.RiskSeverity;
import dev.archlens.infrastructure.persistence.rls.TenantScopedRls;
import jakarta.enterprise.context.ApplicationScoped;

@TenantScopedRls
@ApplicationScoped
public class ComparisonService implements CompareAnalysesUseCase {

    private final AnalysisRepositoryPort analysisRepository;
    private final ProjectRepositoryPort projectRepository;
    private final TenantProvider tenantProvider;

    public ComparisonService(AnalysisRepositoryPort analysisRepository,
                             ProjectRepositoryPort projectRepository,
                             TenantProvider tenantProvider) {
        this.analysisRepository = analysisRepository;
        this.projectRepository = projectRepository;
        this.tenantProvider = tenantProvider;
    }

    @Override
    public AnalysisComparisonResult compare(UUID projectId,
                                            UUID baselineAnalysisId,
                                            UUID currentAnalysisId) {
        if (baselineAnalysisId.equals(currentAnalysisId)) {
            throw new AnalysisNotComparableException("Selecione duas análises distintas para comparar.");
        }

        String tenantId = tenantProvider.getCurrentTenantId();
        projectRepository.findByIdAndTenantId(projectId, tenantId)
                .orElseThrow(() -> new ProjectNotFoundException(projectId));

        Analysis baseline = loadCompletedAnalysis(projectId, baselineAnalysisId, tenantId);
        Analysis current = loadCompletedAnalysis(projectId, currentAnalysisId, tenantId);

        AnalysisComparisonResult result = new AnalysisComparisonResult();
        result.setProjectId(projectId);
        result.setBaseline(toRef(baseline));
        result.setCurrent(toRef(current));
        result.setBaselineSeverityCounts(countBySeverity(baseline.getRisks()));
        result.setCurrentSeverityCounts(countBySeverity(current.getRisks()));

        Map<String, ArchitecturalRisk> baselineByFingerprint = indexByFingerprint(baseline.getRisks());
        Map<String, ArchitecturalRisk> currentByFingerprint = indexByFingerprint(current.getRisks());

        for (Map.Entry<String, ArchitecturalRisk> entry : currentByFingerprint.entrySet()) {
            ArchitecturalRisk currentRisk = entry.getValue();
            ArchitecturalRisk baselineRisk = baselineByFingerprint.get(entry.getKey());
            if (baselineRisk == null) {
                result.getAdded().add(currentRisk);
            } else if (baselineRisk.getSeverity() == currentRisk.getSeverity()) {
                result.getUnchanged().add(currentRisk);
            } else {
                SeverityChangedRisk change = new SeverityChangedRisk();
                change.setBaselineRisk(baselineRisk);
                change.setCurrentRisk(currentRisk);
                result.getSeverityChanged().add(change);
            }
        }

        for (Map.Entry<String, ArchitecturalRisk> entry : baselineByFingerprint.entrySet()) {
            if (!currentByFingerprint.containsKey(entry.getKey())) {
                result.getRemoved().add(entry.getValue());
            }
        }

        return result;
    }

    private Analysis loadCompletedAnalysis(UUID projectId, UUID analysisId, String tenantId) {
        Analysis analysis = analysisRepository.findByProjectIdAndId(projectId, analysisId)
                .filter(a -> tenantId.equals(a.getTenantId()))
                .orElseThrow(() -> new AnalysisNotFoundException(analysisId));

        if (analysis.getStatus() != AnalysisStatus.COMPLETED) {
            throw new AnalysisNotComparableException(analysisId, analysis.getStatus());
        }
        return analysis;
    }

    private static AnalysisRef toRef(Analysis analysis) {
        int riskCount = analysis.getRisks() != null ? analysis.getRisks().size() : 0;
        return new AnalysisRef(analysis.getId(), analysis.getCreatedAt(), riskCount);
    }

    private static Map<String, ArchitecturalRisk> indexByFingerprint(List<ArchitecturalRisk> risks) {
        Map<String, ArchitecturalRisk> indexed = new HashMap<>();
        if (risks == null) {
            return indexed;
        }
        for (ArchitecturalRisk risk : risks) {
            indexed.put(RiskFingerprint.of(risk), risk);
        }
        return indexed;
    }

    private static Map<RiskSeverity, Integer> countBySeverity(List<ArchitecturalRisk> risks) {
        Map<RiskSeverity, Integer> counts = new EnumMap<>(RiskSeverity.class);
        for (RiskSeverity severity : RiskSeverity.values()) {
            counts.put(severity, 0);
        }
        if (risks != null) {
            for (ArchitecturalRisk risk : risks) {
                counts.merge(risk.getSeverity(), 1, Integer::sum);
            }
        }
        return counts;
    }
}
