package dev.archlens.application.service;

import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import dev.archlens.application.port.in.ExportAnalysisReportUseCase;
import dev.archlens.application.port.out.AdrRepositoryPort;
import dev.archlens.application.port.out.AnalysisRepositoryPort;
import dev.archlens.application.port.out.ProjectRepositoryPort;
import dev.archlens.application.port.out.TenantProvider;
import dev.archlens.domain.exception.AnalysisNotFoundException;
import dev.archlens.domain.exception.ProjectNotFoundException;
import dev.archlens.domain.model.Adr;
import dev.archlens.domain.model.Analysis;
import dev.archlens.domain.model.ArchitecturalRisk;
import dev.archlens.domain.model.Project;
import dev.archlens.domain.model.RiskSeverity;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class ReportExportService implements ExportAnalysisReportUseCase {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final AnalysisRepositoryPort analysisRepository;
    private final ProjectRepositoryPort projectRepository;
    private final AdrRepositoryPort adrRepository;
    private final TenantProvider tenantProvider;
    private final ObjectMapper objectMapper;

    public ReportExportService(AnalysisRepositoryPort analysisRepository,
                               ProjectRepositoryPort projectRepository,
                               AdrRepositoryPort adrRepository,
                               TenantProvider tenantProvider) {
        this.analysisRepository = analysisRepository;
        this.projectRepository = projectRepository;
        this.adrRepository = adrRepository;
        this.tenantProvider = tenantProvider;
        this.objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    @Override
    public Report export(UUID projectId, UUID analysisId, ReportFormat format) {
        String tenantId = tenantProvider.getCurrentTenantId();
        Project project = projectRepository.findByIdAndTenantId(projectId, tenantId)
                .orElseThrow(() -> new ProjectNotFoundException(projectId));

        Analysis analysis = analysisRepository.findByProjectIdAndId(projectId, analysisId)
                .filter(a -> tenantId.equals(a.getTenantId()))
                .orElseThrow(() -> new AnalysisNotFoundException(analysisId));

        List<Adr> adrs = adrRepository.findByAnalysisId(analysisId);

        return switch (format) {
            case MARKDOWN -> new Report(
                    "text/markdown; charset=utf-8",
                    "archlens-report-" + project.getName() + ".md",
                    buildMarkdown(project, analysis, adrs));
            case JSON -> new Report(
                    "application/json; charset=utf-8",
                    "archlens-report-" + project.getName() + ".json",
                    buildJson(project, analysis, adrs));
        };
    }

    private String buildMarkdown(Project project, Analysis analysis, List<Adr> adrs) {
        StringBuilder md = new StringBuilder();
        md.append("# Relatório de Diagnóstico Arquitetural — ArchLens\n\n");
        md.append("| Campo | Valor |\n|---|---|\n");
        md.append("| Projeto | ").append(escape(project.getName())).append(" |\n");
        md.append("| Tenant | ").append(escape(project.getTenantId())).append(" |\n");
        md.append("| Status da análise | ").append(analysis.getStatus()).append(" |\n");
        md.append("| Gerado em | ").append(DATE_FMT.format(analysis.getUpdatedAt())).append(" |\n\n");

        md.append("## Sumário executivo\n\n");
        md.append(analysis.getSummary() != null ? analysis.getSummary() : "_Sem resumo disponível._").append("\n\n");

        Map<RiskSeverity, Long> counts = analysis.getRisks().stream()
                .collect(Collectors.groupingBy(ArchitecturalRisk::getSeverity, Collectors.counting()));

        md.append("## Matriz de riscos\n\n");
        md.append("| Severidade | Quantidade |\n|---|---|\n");
        for (RiskSeverity severity : List.of(RiskSeverity.CRITICAL, RiskSeverity.HIGH, RiskSeverity.MEDIUM, RiskSeverity.LOW)) {
            md.append("| ").append(severity).append(" | ")
                    .append(counts.getOrDefault(severity, 0L)).append(" |\n");
        }
        md.append("\n");

        md.append("## Riscos identificados\n\n");
        analysis.getRisks().stream()
                .sorted(Comparator.comparingInt(r -> severityRank(r.getSeverity())))
                .forEach(risk -> appendRisk(md, risk));

        if (!adrs.isEmpty()) {
            md.append("## ADRs recomendados\n\n");
            adrs.forEach(adr -> appendAdr(md, adr));
        }

        md.append("---\n\n");
        md.append("_Gerado por ArchLens — diagnóstico arquitetural com evidências rastreáveis._\n");
        return md.toString();
    }

    private void appendRisk(StringBuilder md, ArchitecturalRisk risk) {
        md.append("### ").append(escape(risk.getTitle())).append("\n\n");
        md.append("- **Severidade:** ").append(risk.getSeverity()).append("\n");
        md.append("- **Categoria:** ").append(risk.getCategory()).append("\n");
        if (risk.getFilePath() != null) {
            md.append("- **Arquivo:** `").append(escape(risk.getFilePath())).append("`\n");
        }
        md.append("- **Descrição:** ").append(escape(risk.getDescription())).append("\n");
        if (risk.getEvidence() != null) {
            md.append("- **Evidência:** ").append(escape(risk.getEvidence())).append("\n");
        }
        if (risk.getSuggestion() != null) {
            md.append("- **Recomendação:** ").append(escape(risk.getSuggestion())).append("\n");
        }
        md.append("\n");
    }

    private void appendAdr(StringBuilder md, Adr adr) {
        md.append("### ").append(escape(adr.getTitle())).append("\n\n");
        md.append("**Contexto:** ").append(escape(adr.getContext())).append("\n\n");
        md.append("**Decisão:** ").append(escape(adr.getDecision())).append("\n\n");
        md.append("**Consequências:** ").append(escape(adr.getConsequences())).append("\n\n");
    }

    private String buildJson(Project project, Analysis analysis, List<Adr> adrs) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("project", Map.of(
                "id", project.getId(),
                "name", project.getName(),
                "tenantId", project.getTenantId()));
        payload.put("analysis", analysis);
        payload.put("adrs", adrs);
        try {
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize report", e);
        }
    }

    private static int severityRank(RiskSeverity severity) {
        return switch (severity) {
            case CRITICAL -> 0;
            case HIGH -> 1;
            case MEDIUM -> 2;
            case LOW -> 3;
        };
    }

    private static String escape(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("|", "\\|").replace("\n", " ");
    }
}
