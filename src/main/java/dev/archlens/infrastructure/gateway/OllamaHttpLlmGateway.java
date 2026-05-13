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
 * Chamadas HTTP ao endpoint /api/chat do Ollama.
 */
public final class OllamaHttpLlmGateway implements LlmGateway {

    private static final Logger LOG = Logger.getLogger(OllamaHttpLlmGateway.class);

    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(30))
            .build();
    private final ObjectMapper objectMapper;
    private final String baseUrl;
    private final String model;

    public OllamaHttpLlmGateway(ArchLensLlmConfig.Ollama cfg, ObjectMapper objectMapper) {
        this.baseUrl = trimSlash(cfg.baseUrl());
        this.model = cfg.model();
        this.objectMapper = objectMapper;
    }

    @Override
    public LlmAnalysisResult analyzeProject(String projectContext) {
        String prompt = """
                És um arquitecto de software. Responde APENAS com um único objeto JSON válido (sem markdown),
                com o formato:
                {"summary":"texto em português","findings":[
                  {"category":"ENUM","severity":"ENUM","title":"","description":"","filePath":null,"evidence":"","suggestion":""}
                ]}
                Onde category é um destes valores: LACK_OF_OBSERVABILITY, CONTRACT_VIOLATION, DESTRUCTIVE_MIGRATION,
                ROLLBACK_RISK, EXCESSIVE_COUPLING, LACK_OF_IDEMPOTENCY, MISSING_DLQ_RETRY, DOMAIN_ENTITY_LEAK,
                OPENAPI_INCONSISTENCY, MISSING_CORRELATION_ID, LAYER_SEPARATION_ISSUE.
                severity: HIGH, MEDIUM, LOW ou CRITICAL. findings pode ser lista vazia.

                Contexto:
                """ + projectContext;

        String raw = ollamaChat(prompt);
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
            LOG.warnf(e, "Resposta Ollama não parseável; devolvendo texto bruto");
            return new LlmAnalysisResult(raw, List.of());
        }
    }

    @Override
    public String answerQuestion(String question, String analysisContext) {
        String prompt = "Contexto:\n" + analysisContext + "\n\nPergunta (responde em português, técnico e conciso):\n" + question;
        return ollamaChat(prompt).trim();
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

    private String ollamaChat(String userContent) {
        try {
            String body = objectMapper.createObjectNode()
                    .put("model", model)
                    .put("stream", false)
                    .set("messages", objectMapper.createArrayNode()
                            .add(objectMapper.createObjectNode()
                                    .put("role", "user")
                                    .put("content", userContent)))
                    .toString();

            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/api/chat"))
                    .timeout(Duration.ofMinutes(3))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                    .build();

            HttpResponse<String> res = http.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (res.statusCode() >= 400) {
                throw new IllegalStateException("HTTP " + res.statusCode() + ": " + res.body());
            }
            JsonNode root = objectMapper.readTree(res.body());
            return root.path("message").path("content").asText("");
        } catch (Exception e) {
            throw new RuntimeException("Falha na chamada ao Ollama", e);
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
            return "http://localhost:11434";
        }
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }
}
