package dev.archlens.application.port.out;

import java.util.List;

public record LlmAnalysisResult(String summary, List<LlmRiskFinding> findings) {
}
