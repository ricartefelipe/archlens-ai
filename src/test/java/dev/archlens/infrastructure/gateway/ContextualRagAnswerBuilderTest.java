package dev.archlens.infrastructure.gateway;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ContextualRagAnswerBuilderTest {

    private static final String CONTEXT = """
            === Resumo da Análise ===
            Análise estática de 7 arquivos concluída. 5 riscos identificados.

            === Trechos Relevantes do Código-Fonte ===
            --- docker-compose.yml (chunk 0, score 0.812) ---
            services:
              api:
                image: latest
            --- openapi.yaml (chunk 1, score 0.701) ---
            paths:
              /orders:
                get:
                  responses: {}
            """;

    @Test
    void greetingAnswerIsDistinctFromClarification() {
        String greeting = ContextualRagAnswerBuilder.build("ola", CONTEXT);
        String clarification = ContextualRagAnswerBuilder.build("não entendi", CONTEXT);

        assertTrue(greeting.contains("Olá"));
        assertTrue(clarification.contains("Em termos simples"));
        assertNotEquals(greeting, clarification);
    }

    @Test
    void targetedAnswerReferencesMatchingArtifact() {
        String answer = ContextualRagAnswerBuilder.build("problemas no docker-compose?", CONTEXT);

        assertTrue(answer.contains("docker-compose.yml"));
        assertFalse(answer.contains("variáveis de ambiente"));
    }
}
