package dev.archlens.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import org.junit.jupiter.api.Test;

import dev.archlens.infrastructure.config.ArchLensLlmConfig;

class CapabilitiesServiceTest {

    @Test
    void reportsLocalProviderWhenOpenAiKeyMissing() {
        ArchLensLlmConfig config = new ArchLensLlmConfig() {
            @Override
            public Provider provider() {
                return Provider.openai;
            }

            @Override
            public OpenAi openai() {
                return new OpenAi() {
                    @Override
                    public java.util.Optional<String> apiKey() {
                        return java.util.Optional.empty();
                    }

                    @Override
                    public String baseUrl() {
                        return "https://api.openai.com";
                    }

                    @Override
                    public String model() {
                        return "gpt-4o-mini";
                    }
                };
            }

            @Override
            public Ollama ollama() {
                return null;
            }
        };

        CapabilitiesService service = new CapabilitiesService(config);
        CapabilitiesService.CapabilitiesSnapshot snapshot = service.snapshot();

        assertEquals("openai", snapshot.llmProvider());
        assertFalse(snapshot.llmConfigured());
    }
}
