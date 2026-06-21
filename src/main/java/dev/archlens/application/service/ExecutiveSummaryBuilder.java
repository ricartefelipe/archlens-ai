package dev.archlens.application.service;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import dev.archlens.domain.model.Adr;
import dev.archlens.domain.model.Analysis;
import dev.archlens.domain.model.ArchitecturalRisk;
import dev.archlens.domain.model.Project;
import dev.archlens.domain.model.RiskSeverity;

/**
 * Sumário executivo derivado dos riscos reais (sem LLM) para entrega consultoria.
 */
public final class ExecutiveSummaryBuilder {

    private ExecutiveSummaryBuilder() {
    }

    public static String build(Project project, Analysis analysis, List<Adr> adrs) {
        List<ArchitecturalRisk> risks = analysis.getRisks();
        if (risks.isEmpty()) {
            if (analysis.getStatus() == dev.archlens.domain.model.AnalysisStatus.FAILED) {
                return "A análise não foi concluída com sucesso. "
                        + (analysis.getSummary() != null ? analysis.getSummary() : "Verifique upload e worker.");
            }
            return "Nenhum risco arquitetural foi identificado na análise estática de "
                    + project.getName() + ". Recomenda-se revisão manual ou novo upload com artefatos completos "
                    + "(código, OpenAPI, migrations, Docker, CI).";
        }

        Map<RiskSeverity, Long> counts = risks.stream()
                .collect(Collectors.groupingBy(ArchitecturalRisk::getSeverity, Collectors.counting()));

        long critical = counts.getOrDefault(RiskSeverity.CRITICAL, 0L);
        long high = counts.getOrDefault(RiskSeverity.HIGH, 0L);
        long medium = counts.getOrDefault(RiskSeverity.MEDIUM, 0L);
        long low = counts.getOrDefault(RiskSeverity.LOW, 0L);

        StringBuilder sb = new StringBuilder();
        sb.append("Diagnóstico arquitetural de **").append(project.getName()).append("**: ");
        sb.append(risks.size()).append(" risco(s) identificado(s) com evidência rastreável");

        if (critical + high > 0) {
            sb.append(" — ").append(critical + high).append(" de severidade alta ou crítica");
        }
        sb.append(".\n\n");

        sb.append("**Prioridade imediata:** ");
        if (critical > 0) {
            sb.append("tratar ").append(critical).append(" risco(s) CRITICAL antes de evoluções em produção. ");
        } else if (high > 0) {
            sb.append("endereçar ").append(high).append(" risco(s) HIGH no próximo ciclo de sprint. ");
        } else {
            sb.append("riscos predominantemente médios/baixos; foco em dívida técnica planejada. ");
        }

        sb.append("\n\n**Distribuição:** CRITICAL=").append(critical)
                .append(", HIGH=").append(high)
                .append(", MEDIUM=").append(medium)
                .append(", LOW=").append(low).append(".\n\n");

        sb.append("**Principais achados:**\n");
        risks.stream()
                .sorted(Comparator.comparingInt(r -> severityRank(r.getSeverity())))
                .limit(5)
                .forEach(r -> sb.append("- [").append(r.getSeverity()).append("] ")
                        .append(r.getTitle())
                        .append(r.getFilePath() != null ? " (`" + r.getFilePath() + "`)" : "")
                        .append("\n"));

        if (!adrs.isEmpty()) {
            sb.append("\n**Decisões recomendadas:** ").append(adrs.size())
                    .append(" ADR(s) proposto(s) para formalizar correções arquiteturais.\n");
        }

        sb.append("\n_Detalhamento técnico, evidências e recomendações na seção de riscos abaixo._");
        return sb.toString();
    }

    private static int severityRank(RiskSeverity severity) {
        return switch (severity) {
            case CRITICAL -> 0;
            case HIGH -> 1;
            case MEDIUM -> 2;
            case LOW -> 3;
        };
    }
}
