package dev.archlens.application.service;

import dev.archlens.domain.model.ArchitecturalRisk;

final class RiskFingerprint {

    private RiskFingerprint() {
    }

    static String of(ArchitecturalRisk risk) {
        return normalize(risk.getCategory().name())
                + '|' + normalize(risk.getFilePath())
                + '|' + normalize(risk.getTitle());
    }

    private static String normalize(String value) {
        if (value == null) {
            return "";
        }
        return value.trim().toLowerCase();
    }
}
