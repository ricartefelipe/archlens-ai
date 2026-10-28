package dev.archlens.infrastructure.config;

import java.util.Optional;

import io.smallrye.config.WithDefault;
import io.smallrye.config.ConfigMapping;

@ConfigMapping(prefix = "archlens.llm")
public interface ArchLensLlmConfig {

    @WithDefault("local")
    Provider provider();

    /**
     * Permite, apenas em modo demo explícito, iniciar produção com gateway
     * simulado alimentando o fallback. Padrão falso: produção recusa saída
     * simulada apresentada como análise real.
     */
    @WithDefault("false")
    boolean allowSimulatedDemo();

    OpenAi openai();

    Ollama ollama();

    enum Provider {
        local,
        openai,
        ollama
    }

    interface OpenAi {

        Optional<String> apiKey();

        @WithDefault("https://api.openai.com")
        String baseUrl();

        @WithDefault("gpt-4o-mini")
        String model();
    }

    interface Ollama {

        @WithDefault("http://localhost:11434")
        String baseUrl();

        @WithDefault("llama3.2")
        String model();
    }
}
