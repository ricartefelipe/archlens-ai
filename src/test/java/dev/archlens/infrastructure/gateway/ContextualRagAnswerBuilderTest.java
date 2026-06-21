package dev.archlens.infrastructure.gateway;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ContextualRagAnswerBuilderTest {

    private static final String CONTEXT = """
            === Resumo da Análise ===
            Análise estática de 7 arquivos concluída. 5 riscos identificados.

            === Riscos Identificados ===
            [CRITICAL] SQL montado por concatenação | OrderController.java | SECURITY_RISK
              Queries JDBC com concatenação expõem SQL injection.
              Sugestão: Usar PreparedStatement com placeholders.
            [HIGH] OpenAPI sem security scheme | openapi.yaml | SECURITY_RISK
              Especificação sem esquemas de segurança.
            [MEDIUM] Dockerfile sem HEALTHCHECK | Dockerfile | MISSING_HEALTH_CHECK
              Sem instrução HEALTHCHECK no Dockerfile.

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

        assertTrue(answer.contains("docker") || answer.contains("HEALTHCHECK"));
        assertFalse(answer.contains("variáveis de ambiente"));
    }

    @Test
    void riskQuestionListsStructuredRisks() {
        String answer = ContextualRagAnswerBuilder.build("riscos", CONTEXT);

        assertTrue(answer.contains("Riscos identificados"));
        assertTrue(answer.contains("SQL montado"));
        assertTrue(answer.contains("OpenAPI sem security"));
    }

    @Test
    void vagueQuestionDiffersFromRiskOverview() {
        String risks = ContextualRagAnswerBuilder.build("riscos", CONTEXT);
        String vague = ContextualRagAnswerBuilder.build("teste", CONTEXT);

        assertNotEquals(risks, vague);
        assertTrue(vague.contains("Não encontrei") || vague.contains("Maior prioridade")
                || vague.contains("testes"));
    }
}
