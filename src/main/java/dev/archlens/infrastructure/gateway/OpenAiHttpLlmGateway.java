package dev.archlens.infrastructure.gateway;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import org.jboss.logging.Logger;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import dev.archlens.application.port.out.LlmAnalysisResult;
import dev.archlens.application.port.out.LlmGateway;
import dev.archlens.application.port.out.LlmRiskFinding;
import dev.archlens.domain.model.RiskCategory;
import dev.archlens.domain.model.RiskSeverity;
import dev.archlens.infrastructure.config.ArchLensLlmConfig;

/**
 * Chamadas HTTP à API Chat Completions do fornecedor configurado em {@code baseUrl} (por defeito OpenAI).
 */
public final class OpenAiHttpLlmGateway implements LlmGateway {

    private static final Logger LOG = Logger.getLogger(OpenAiHttpLlmGateway.class);

    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(30))
            .build();
    private final ObjectMapper objectMapper;
    private final String apiKey;
    private final String baseUrl;
    private final String model;

    public OpenAiHttpLlmGateway(ArchLensLlmConfig.OpenAi cfg, ObjectMapper objectMapper) {
        this.apiKey = cfg.apiKey().orElse("");
        this.baseUrl = trimSlash(cfg.baseUrl());
        this.model = cfg.model();
        this.objectMapper = objectMapper;
    }

    @Override
    public LlmAnalysisResult analyzeProject(String projectContext) {
        String system = """
                És um arquitecto de software. Responde APENAS com um único objeto JSON válido (sem markdown),
                com o formato:
                {"summary":"texto em português","findings":[
                  {"category":"ENUM","severity":"ENUM","title":"","description":"","filePath":null,"evidence":"","suggestion":""}
                ]}
                Onde category é um destes valores: LACK_OF_OBSERVABILITY, CONTRACT_VIOLATION, DESTRUCTIVE_MIGRATION,
                ROLLBACK_RISK, EXCESSIVE_COUPLING, LACK_OF_IDEMPOTENCY, MISSING_DLQ_RETRY, DOMAIN_ENTITY_LEAK,
                OPENAPI_INCONSISTENCY, MISSING_CORRELATION_ID, LAYER_SEPARATION_ISSUE.
                severity: HIGH, MEDIUM, LOW ou CRITICAL. findings pode ser lista vazia.""";
        String user = "Contexto do projecto (pode ser reduzido):\n" + projectContext;

        String raw = chatCompletion(system, user);
        try {
            JsonNode root = objectMapper.readTree(stripJsonFence(raw));
            String summary = root.path("summary").asText("Análise concluída.");
            List<LlmRiskFinding> findings = new ArrayList<>();
            if (root.path("findings").isArray()) {
                for (JsonNode n : root.path("findings")) {
                    findings.add(parseFinding(n));
                }
            }
            return new LlmAnalysisResult(summary, findings);
        } catch (Exception e) {
            LOG.warnf(e, "Resposta LLM não parseável; devolvendo resumo textual");
            return new LlmAnalysisResult(raw, List.of());
        }
    }

    @Override
    public String answerQuestion(String question, String analysisContext) {
        String system = """
                Respondes em português de forma técnica e concisa.
                Baseia-te apenas no contexto fornecido; se faltar informação, indica-o explicitamente.""";
        String user = "Contexto:\n" + analysisContext + "\n\nPergunta:\n" + question;
        return chatCompletion(system, user).trim();
    }

    private LlmRiskFinding parseFinding(JsonNode n) {
        RiskCategory cat = parseCategory(n.path("category").asText(null));
        RiskSeverity sev = parseSeverity(n.path("severity").asText(null));
        return new LlmRiskFinding(
                cat,
                sev,
                n.path("title").asText("(sem título)"),
                n.path("description").asText(""),
                n.path("filePath").isNull() ? null : n.path("filePath").asText(null),
                n.path("evidence").asText(""),
                n.path("suggestion").asText(""));
    }

    private static RiskCategory parseCategory(String s) {
        if (s == null || s.isBlank()) {
            return RiskCategory.LAYER_SEPARATION_ISSUE;
        }
        try {
            return RiskCategory.valueOf(s.trim());
        } catch (IllegalArgumentException e) {
            return RiskCategory.LAYER_SEPARATION_ISSUE;
        }
    }

    private static RiskSeverity parseSeverity(String s) {
        if (s == null || s.isBlank()) {
            return RiskSeverity.MEDIUM;
        }
        try {
            return RiskSeverity.valueOf(s.trim());
        } catch (IllegalArgumentException e) {
            return RiskSeverity.MEDIUM;
        }
    }

    private String chatCompletion(String system, String user) {
        try {
            String body = objectMapper.createObjectNode()
                    .put("model", model)
                    .set("messages", objectMapper.createArrayNode()
                            .add(objectMapper.createObjectNode()
                                    .put("role", "system")
                                    .put("content", system))
                            .add(objectMapper.createObjectNode()
                                    .put("role", "user")
                                    .put("content", user)))
                    .toString();

            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/v1/chat/completions"))
                    .timeout(Duration.ofSeconds(120))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + apiKey)
                    .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                    .build();

            HttpResponse<String> res = http.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (res.statusCode() >= 400) {
                throw new IllegalStateException("HTTP " + res.statusCode() + ": " + res.body());
            }
            JsonNode root = objectMapper.readTree(res.body());
            return root.path("choices").path(0).path("message").path("content").asText("");
        } catch (Exception e) {
            throw new RuntimeException("Falha na chamada ao modelo remoto", e);
        }
    }

    private static String stripJsonFence(String raw) {
        String t = raw.trim();
        if (t.startsWith("```")) {
            int nl = t.indexOf('\n');
            if (nl > 0) {
                t = t.substring(nl + 1);
            }
            int end = t.lastIndexOf("```");
            if (end > 0) {
                t = t.substring(0, end);
            }
        }
        return t.trim();
    }

    private static String trimSlash(String url) {
        if (url == null || url.isEmpty()) {
            return "https://api.openai.com";
        }
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }
}
