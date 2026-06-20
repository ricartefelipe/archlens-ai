package dev.archlens.interfaces.rest;

import java.util.UUID;

import dev.archlens.application.port.in.ExportComparisonReportUseCase;
import dev.archlens.application.port.in.ExportComparisonReportUseCase.Report;
import dev.archlens.application.port.in.ExportComparisonReportUseCase.ReportFormat;
import jakarta.inject.Inject;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Response;

@Path("/v1/projects/{projectId}/analyses/compare/report")
public class ComparisonReportResource {

    private final ExportComparisonReportUseCase exportUseCase;

    @Inject
    public ComparisonReportResource(ExportComparisonReportUseCase exportUseCase) {
        this.exportUseCase = exportUseCase;
    }

    @GET
    @Produces({"text/markdown", "application/json", "application/pdf"})
    public Response exportReport(@PathParam("projectId") UUID projectId,
                                 @QueryParam("baseline") UUID baselineAnalysisId,
                                 @QueryParam("current") UUID currentAnalysisId,
                                 @QueryParam("format") @DefaultValue("markdown") String format) {
        ReportFormat reportFormat = switch (format.toLowerCase()) {
            case "json" -> ReportFormat.JSON;
            case "pdf" -> ReportFormat.PDF;
            default -> ReportFormat.MARKDOWN;
        };
        Report report = exportUseCase.export(projectId, baselineAnalysisId, currentAnalysisId, reportFormat);
        return Response.ok(report.content())
                .type(report.contentType())
                .header("Content-Disposition", "attachment; filename=\"" + report.fileName() + "\"")
                .build();
    }
}
