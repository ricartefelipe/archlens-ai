package dev.archlens.application.service;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import dev.archlens.infrastructure.config.ArchLensLlmConfig;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class CapabilitiesService {

    private final ArchLensLlmConfig llmConfig;

    @ConfigProperty(name = "quarkus.application.version", defaultValue = "dev")
    String applicationVersion;

    @ConfigProperty(name = "archlens.commercial.enforce-quotas", defaultValue = "true")
    boolean enforceQuotas;

    @ConfigProperty(name = "archlens.analysis.llm-fallback-enabled", defaultValue = "false")
    boolean analysisLlmFallbackEnabled;

    @ConfigProperty(name = "archlens.report.brand-name", defaultValue = "ArchLens")
    String reportBrandName;

    public CapabilitiesService(ArchLensLlmConfig llmConfig) {
        this.llmConfig = llmConfig;
    }

    public CapabilitiesSnapshot snapshot() {
        ArchLensLlmConfig.Provider provider = llmConfig.provider();
        boolean configured = switch (provider) {
            case openai -> llmConfig.openai().apiKey().map(k -> !k.isBlank()).orElse(false);
            case ollama -> true;
            case local -> false;
        };
        String version = applicationVersion == null || applicationVersion.isBlank()
                ? "dev"
                : applicationVersion;
        String brand = reportBrandName == null || reportBrandName.isBlank()
                ? "ArchLens"
                : reportBrandName;
        return new CapabilitiesSnapshot(
                "ArchLens AI",
                version,
                provider.name(),
                configured,
                enforceQuotas,
                brand,
                analysisLlmFallbackEnabled);
    }

    public record CapabilitiesSnapshot(
            String product,
            String version,
            String llmProvider,
            boolean llmConfigured,
            boolean enforceQuotas,
            String reportBrandName,
            boolean analysisLlmFallbackEnabled) {
    }
}
