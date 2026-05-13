package dev.archlens.infrastructure.gateway;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.jboss.logging.Logger;

import com.fasterxml.jackson.databind.ObjectMapper;

import dev.archlens.application.port.out.LlmAnalysisResult;
import dev.archlens.application.port.out.LlmGateway;
import dev.archlens.infrastructure.config.ArchLensLlmConfig;

/**
 * Selecciona o adapter de inferência conforme {@code archlens.llm.provider}.
 */
@ApplicationScoped
public class ConfigurableLlmGateway implements LlmGateway {

    private static final Logger LOG = Logger.getLogger(ConfigurableLlmGateway.class);

    private final LlmGateway delegate;

    @Inject
    public ConfigurableLlmGateway(ArchLensLlmConfig config, ObjectMapper objectMapper) {
        this.delegate = resolve(config, objectMapper);
    }

    private static LlmGateway resolve(ArchLensLlmConfig config, ObjectMapper objectMapper) {
        ArchLensLlmConfig.Provider p = config.provider();
        if (p == ArchLensLlmConfig.Provider.openai) {
            if (config.openai().apiKey().map(String::isBlank).orElse(true)) {
                LOG.warn("archlens.llm.provider=openai sem ARCHLENS_LLM_OPENAI_API_KEY / api-key definida; a usar implementação local");
                return new LocalLlmGateway();
            }
            return new OpenAiHttpLlmGateway(config.openai(), objectMapper);
        }
        if (p == ArchLensLlmConfig.Provider.ollama) {
            return new OllamaHttpLlmGateway(config.ollama(), objectMapper);
        }
        return new LocalLlmGateway();
    }

    @Override
    public LlmAnalysisResult analyzeProject(String projectContext) {
        return delegate.analyzeProject(projectContext);
    }

    @Override
    public String answerQuestion(String question, String analysisContext) {
        return delegate.answerQuestion(question, analysisContext);
    }
}
