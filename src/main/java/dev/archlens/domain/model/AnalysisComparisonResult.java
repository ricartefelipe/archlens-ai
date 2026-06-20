package dev.archlens.domain.model;

import java.time.Instant;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class AnalysisComparisonResult {

    private UUID projectId;
    private AnalysisRef baseline;
    private AnalysisRef current;
    private Map<RiskSeverity, Integer> baselineSeverityCounts;
    private Map<RiskSeverity, Integer> currentSeverityCounts;
    private List<ArchitecturalRisk> added;
    private List<ArchitecturalRisk> removed;
    private List<SeverityChangedRisk> severityChanged;
    private List<ArchitecturalRisk> unchanged;

    public AnalysisComparisonResult() {
        this.baselineSeverityCounts = new EnumMap<>(RiskSeverity.class);
        this.currentSeverityCounts = new EnumMap<>(RiskSeverity.class);
        this.added = new ArrayList<>();
        this.removed = new ArrayList<>();
        this.severityChanged = new ArrayList<>();
        this.unchanged = new ArrayList<>();
    }

    public UUID getProjectId() {
        return projectId;
    }

    public void setProjectId(UUID projectId) {
        this.projectId = projectId;
    }

    public AnalysisRef getBaseline() {
        return baseline;
    }

    public void setBaseline(AnalysisRef baseline) {
        this.baseline = baseline;
    }

    public AnalysisRef getCurrent() {
        return current;
    }

    public void setCurrent(AnalysisRef current) {
        this.current = current;
    }

    public Map<RiskSeverity, Integer> getBaselineSeverityCounts() {
        return baselineSeverityCounts;
    }

    public void setBaselineSeverityCounts(Map<RiskSeverity, Integer> baselineSeverityCounts) {
        this.baselineSeverityCounts = baselineSeverityCounts;
    }

    public Map<RiskSeverity, Integer> getCurrentSeverityCounts() {
        return currentSeverityCounts;
    }

    public void setCurrentSeverityCounts(Map<RiskSeverity, Integer> currentSeverityCounts) {
        this.currentSeverityCounts = currentSeverityCounts;
    }

    public List<ArchitecturalRisk> getAdded() {
        return added;
    }

    public void setAdded(List<ArchitecturalRisk> added) {
        this.added = added;
    }

    public List<ArchitecturalRisk> getRemoved() {
        return removed;
    }

    public void setRemoved(List<ArchitecturalRisk> removed) {
        this.removed = removed;
    }

    public List<SeverityChangedRisk> getSeverityChanged() {
        return severityChanged;
    }

    public void setSeverityChanged(List<SeverityChangedRisk> severityChanged) {
        this.severityChanged = severityChanged;
    }

    public List<ArchitecturalRisk> getUnchanged() {
        return unchanged;
    }

    public void setUnchanged(List<ArchitecturalRisk> unchanged) {
        this.unchanged = unchanged;
    }

    public record AnalysisRef(UUID id, Instant createdAt, int riskCount) {
    }

    public static final class SeverityChangedRisk {
        private ArchitecturalRisk baselineRisk;
        private ArchitecturalRisk currentRisk;

        public ArchitecturalRisk getBaselineRisk() {
            return baselineRisk;
        }

        public void setBaselineRisk(ArchitecturalRisk baselineRisk) {
            this.baselineRisk = baselineRisk;
        }

        public ArchitecturalRisk getCurrentRisk() {
            return currentRisk;
        }

        public void setCurrentRisk(ArchitecturalRisk currentRisk) {
            this.currentRisk = currentRisk;
        }
    }
}
