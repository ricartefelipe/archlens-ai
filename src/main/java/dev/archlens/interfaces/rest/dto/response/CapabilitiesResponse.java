package dev.archlens.interfaces.rest.dto.response;

public record CapabilitiesResponse(
        String product,
        String version,
        String llmProvider,
        boolean llmConfigured,
        boolean enforceQuotas,
        String reportBrandName,
        boolean analysisLlmFallbackEnabled) {
}
