package dev.archlens.application.port.in;

import java.util.UUID;

public interface ExportComparisonReportUseCase {

    Report export(UUID projectId, UUID baselineAnalysisId, UUID currentAnalysisId, ReportFormat format);

    enum ReportFormat {
        MARKDOWN,
        JSON,
        PDF
    }

    record Report(String contentType, String fileName, byte[] content) {
    }
}
