package dev.archlens.domain.exception;

import java.util.UUID;

import dev.archlens.domain.model.AnalysisStatus;

public class AnalysisNotComparableException extends RuntimeException {

    public AnalysisNotComparableException(UUID analysisId, AnalysisStatus status) {
        super("Análise " + analysisId + " não pode ser comparada no status " + status
                + ". Apenas análises COMPLETED são elegíveis.");
    }

    public AnalysisNotComparableException(String message) {
        super(message);
    }
}
