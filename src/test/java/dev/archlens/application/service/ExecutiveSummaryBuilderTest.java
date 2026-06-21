package dev.archlens.application.service;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import dev.archlens.domain.model.Adr;
import dev.archlens.domain.model.Analysis;
import dev.archlens.domain.model.AnalysisStatus;
import dev.archlens.domain.model.ArchitecturalRisk;
import dev.archlens.domain.model.Project;
import dev.archlens.domain.model.RiskCategory;
import dev.archlens.domain.model.RiskSeverity;

class ExecutiveSummaryBuilderTest {

    @Test
    void buildsExecutiveSummaryFromRisks() {
        Project project = new Project();
        project.setName("demo-loja");

        ArchitecturalRisk risk = new ArchitecturalRisk();
        risk.setSeverity(RiskSeverity.HIGH);
        risk.setCategory(RiskCategory.CONTRACT_VIOLATION);
        risk.setTitle("OpenAPI sem respostas de erro");
        risk.setFilePath("openapi.yaml");

        Analysis analysis = new Analysis();
        analysis.setStatus(AnalysisStatus.COMPLETED);
        analysis.setRisks(List.of(risk));

        String summary = ExecutiveSummaryBuilder.build(project, analysis, List.of());

        assertTrue(summary.contains("demo-loja"));
        assertTrue(summary.contains("OpenAPI sem respostas de erro"));
        assertTrue(summary.contains("HIGH=1"));
    }
}
