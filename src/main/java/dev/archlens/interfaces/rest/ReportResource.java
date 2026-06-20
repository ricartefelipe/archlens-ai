package dev.archlens.interfaces.rest;

import java.util.UUID;

import dev.archlens.application.port.in.ExportAnalysisReportUseCase;
import dev.archlens.application.port.in.ExportAnalysisReportUseCase.Report;
import dev.archlens.application.port.in.ExportAnalysisReportUseCase.ReportFormat;
import jakarta.inject.Inject;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Response;

@Path("/v1/projects/{projectId}/analyses/{analysisId}/report")
public class ReportResource {

    private final ExportAnalysisReportUseCase exportUseCase;

    @Inject
    public ReportResource(ExportAnalysisReportUseCase exportUseCase) {
        this.exportUseCase = exportUseCase;
    }

    @GET
    @Produces({"text/markdown", "application/json", "application/pdf"})
    public Response exportReport(@PathParam("projectId") UUID projectId,
                                 @PathParam("analysisId") UUID analysisId,
                                 @QueryParam("format") @DefaultValue("markdown") String format) {
        ReportFormat reportFormat = switch (format.toLowerCase()) {
            case "json" -> ReportFormat.JSON;
            case "pdf" -> ReportFormat.PDF;
            default -> ReportFormat.MARKDOWN;
        };
        Report report = exportUseCase.export(projectId, analysisId, reportFormat);
        return Response.ok(report.content())
                .type(report.contentType())
                .header("Content-Disposition", "attachment; filename=\"" + report.fileName() + "\"")
                .build();
    }
}
