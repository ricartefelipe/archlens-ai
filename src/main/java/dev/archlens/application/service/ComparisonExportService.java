package dev.archlens.application.service;

import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import dev.archlens.application.port.in.CompareAnalysesUseCase;
import dev.archlens.application.port.in.ExportComparisonReportUseCase;
import dev.archlens.application.port.out.ProjectRepositoryPort;
import dev.archlens.application.port.out.TenantProvider;
import dev.archlens.domain.exception.ProjectNotFoundException;
import dev.archlens.domain.model.AnalysisComparisonResult;
import dev.archlens.domain.model.AnalysisComparisonResult.SeverityChangedRisk;
import dev.archlens.domain.model.ArchitecturalRisk;
import dev.archlens.domain.model.Project;
import dev.archlens.domain.model.RiskSeverity;
import dev.archlens.infrastructure.persistence.rls.TenantScopedRls;
import dev.archlens.infrastructure.report.PdfReportRenderer;
import jakarta.enterprise.context.ApplicationScoped;

@TenantScopedRls
@ApplicationScoped
public class ComparisonExportService implements ExportComparisonReportUseCase {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final CompareAnalysesUseCase compareAnalysesUseCase;
    private final ProjectRepositoryPort projectRepository;
    private final TenantProvider tenantProvider;
    private final PdfReportRenderer pdfReportRenderer;
    private final ObjectMapper objectMapper;

    public ComparisonExportService(CompareAnalysesUseCase compareAnalysesUseCase,
                                   ProjectRepositoryPort projectRepository,
                                   TenantProvider tenantProvider,
                                   PdfReportRenderer pdfReportRenderer) {
        this.compareAnalysesUseCase = compareAnalysesUseCase;
        this.projectRepository = projectRepository;
        this.tenantProvider = tenantProvider;
        this.pdfReportRenderer = pdfReportRenderer;
        this.objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    @Override
    public Report export(UUID projectId, UUID baselineAnalysisId, UUID currentAnalysisId, ReportFormat format) {
        String tenantId = tenantProvider.getCurrentTenantId();
        Project project = projectRepository.findByIdAndTenantId(projectId, tenantId)
                .orElseThrow(() -> new ProjectNotFoundException(projectId));

        AnalysisComparisonResult comparison = compareAnalysesUseCase.compare(
                projectId, baselineAnalysisId, currentAnalysisId);

        return switch (format) {
            case MARKDOWN -> textReport(
                    "text/markdown; charset=utf-8",
                    "archlens-comparison-" + project.getName() + ".md",
                    buildMarkdown(project, comparison));
            case JSON -> textReport(
                    "application/json; charset=utf-8",
                    "archlens-comparison-" + project.getName() + ".json",
                    buildJson(project, comparison));
            case PDF -> {
                String markdown = buildMarkdown(project, comparison);
                byte[] pdf = pdfReportRenderer.render(markdown, project.getName());
                yield new Report(
                        "application/pdf",
                        "archlens-comparison-" + project.getName() + ".pdf",
                        pdf);
            }
        };
    }

    private static Report textReport(String contentType, String fileName, String body) {
        return new Report(contentType, fileName, body.getBytes(StandardCharsets.UTF_8));
    }

    private String buildMarkdown(Project project, AnalysisComparisonResult comparison) {
        StringBuilder md = new StringBuilder();
        md.append("# Relatório Comparativo Before/After — ArchLens\n\n");
        md.append("| Campo | Valor |\n|---|---|\n");
        md.append("| Projeto | ").append(escape(project.getName())).append(" |\n");
        md.append("| Tenant | ").append(escape(project.getTenantId())).append(" |\n");
        md.append("| Baseline | ").append(DATE_FMT.format(comparison.getBaseline().createdAt()))
                .append(" (").append(comparison.getBaseline().riskCount()).append(" riscos) |\n");
        md.append("| Atual | ").append(DATE_FMT.format(comparison.getCurrent().createdAt()))
                .append(" (").append(comparison.getCurrent().riskCount()).append(" riscos) |\n");

        int delta = comparison.getCurrent().riskCount() - comparison.getBaseline().riskCount();
        md.append("| Delta total | ").append(delta >= 0 ? "+" : "").append(delta).append(" |\n\n");

        md.append("## Evolução por severidade\n\n");
        md.append("| Severidade | Antes | Depois | Delta |\n|---|---|---|---|\n");
        for (RiskSeverity severity : List.of(
                RiskSeverity.CRITICAL, RiskSeverity.HIGH, RiskSeverity.MEDIUM, RiskSeverity.LOW)) {
            int before = comparison.getBaselineSeverityCounts().getOrDefault(severity, 0);
            int after = comparison.getCurrentSeverityCounts().getOrDefault(severity, 0);
            int severityDelta = after - before;
            md.append("| ").append(severity).append(" | ").append(before).append(" | ")
                    .append(after).append(" | ")
                    .append(severityDelta >= 0 ? "+" : "").append(severityDelta).append(" |\n");
        }
        md.append("\n");

        appendRiskSection(md, "Novos riscos", comparison.getAdded());
        appendRiskSection(md, "Riscos resolvidos", comparison.getRemoved());
        appendSeverityChangedSection(md, comparison.getSeverityChanged());

        md.append("## Resumo\n\n");
        md.append("- **Novos:** ").append(comparison.getAdded().size()).append("\n");
        md.append("- **Resolvidos:** ").append(comparison.getRemoved().size()).append("\n");
        md.append("- **Severidade alterada:** ").append(comparison.getSeverityChanged().size()).append("\n");
        md.append("- **Inalterados:** ").append(comparison.getUnchanged().size()).append("\n\n");

        md.append("---\n\n");
        md.append("_Gerado por ArchLens — follow-up consultivo com evidências rastreáveis._\n");
        return md.toString();
    }

    private void appendRiskSection(StringBuilder md, String title, List<ArchitecturalRisk> risks) {
        md.append("## ").append(title).append(" (").append(risks.size()).append(")\n\n");
        if (risks.isEmpty()) {
            md.append("_Nenhum._\n\n");
            return;
        }
        for (ArchitecturalRisk risk : risks) {
            appendRisk(md, risk);
        }
    }

    private void appendSeverityChangedSection(StringBuilder md, List<SeverityChangedRisk> changes) {
        md.append("## Severidade alterada (").append(changes.size()).append(")\n\n");
        if (changes.isEmpty()) {
            md.append("_Nenhum._\n\n");
            return;
        }
        for (SeverityChangedRisk change : changes) {
            ArchitecturalRisk baseline = change.getBaselineRisk();
            ArchitecturalRisk current = change.getCurrentRisk();
            md.append("### ").append(escape(current.getTitle())).append("\n\n");
            md.append("- **Antes:** ").append(baseline.getSeverity()).append("\n");
            md.append("- **Depois:** ").append(current.getSeverity()).append("\n");
            if (current.getFilePath() != null) {
                md.append("- **Arquivo:** `").append(escape(current.getFilePath())).append("`\n");
            }
            md.append("\n");
        }
    }

    private void appendRisk(StringBuilder md, ArchitecturalRisk risk) {
        md.append("### ").append(escape(risk.getTitle())).append("\n\n");
        md.append("- **Severidade:** ").append(risk.getSeverity()).append("\n");
        md.append("- **Categoria:** ").append(risk.getCategory()).append("\n");
        if (risk.getFilePath() != null) {
            md.append("- **Arquivo:** `").append(escape(risk.getFilePath())).append("`\n");
        }
        md.append("- **Descrição:** ").append(escape(risk.getDescription())).append("\n");
        if (risk.getSuggestion() != null) {
            md.append("- **Recomendação:** ").append(escape(risk.getSuggestion())).append("\n");
        }
        md.append("\n");
    }

    private String buildJson(Project project, AnalysisComparisonResult comparison) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("project", Map.of(
                "id", project.getId(),
                "name", project.getName(),
                "tenantId", project.getTenantId()));
        payload.put("comparison", comparison);
        try {
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize comparison report", e);
        }
    }

    private static String escape(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("|", "\\|").replace("\n", " ");
    }
}
