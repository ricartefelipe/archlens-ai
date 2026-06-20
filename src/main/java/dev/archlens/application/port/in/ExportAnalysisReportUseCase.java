package dev.archlens.application.port.in;

import java.util.UUID;

public interface ExportAnalysisReportUseCase {

    Report export(UUID projectId, UUID analysisId, ReportFormat format);

    enum ReportFormat {
        MARKDOWN,
        JSON,
        PDF
    }

    record Report(String contentType, String fileName, byte[] content) {
    }
}
