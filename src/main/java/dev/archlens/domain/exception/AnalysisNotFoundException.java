package dev.archlens.domain.exception;

import java.util.UUID;

public class AnalysisNotFoundException extends RuntimeException {

    public AnalysisNotFoundException(UUID id) {
        super("Analysis not found: " + id);
    }
}
