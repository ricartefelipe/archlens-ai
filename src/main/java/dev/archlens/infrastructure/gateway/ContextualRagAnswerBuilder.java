package dev.archlens.infrastructure.gateway;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Respostas do chat em modo local (sem LLM externo): riscos estruturados + trechos RAG por intenção.
 */
public final class ContextualRagAnswerBuilder {

    private static final String SUMMARY_MARKER = "=== Resumo da Análise ===";
    private static final String RISKS_MARKER = "=== Riscos Identificados ===";
    private static final String CHUNKS_MARKER = "=== Trechos Relevantes do Código-Fonte ===";
    private static final Pattern RISK_LINE = Pattern.compile(
            "^\\[(CRITICAL|HIGH|MEDIUM|LOW)\\] (.+?) \\| (.+?) \\| ([A-Z_]+)$");
    private static final Pattern CHUNK_HEADER = Pattern.compile(
            "^--- (.+?) \\(chunk (\\d+), score ([0-9.]+)\\) ---$");
    private static final Set<String> STOPWORDS = Set.of(
            "a", "o", "os", "as", "de", "da", "do", "dos", "das", "e", "em", "na", "no", "nas", "nos",
            "um", "uma", "uns", "umas", "que", "por", "para", "com", "sem", "sobre", "como", "qual", "quais",
            "me", "eu", "voce", "você", "isso", "esta", "este", "essa", "esse", "ao", "aos", "à", "ou");

    private static final Map<String, List<String>> TOPIC_KEYWORDS = Map.of(
            "testes", List.of("teste", "testes", "ci", "pipeline", "cobertura", "junit", "pytest", "github", "workflow"),
            "docker", List.of("docker", "compose", "container", "healthcheck", "dockerfile", "root", "imagem"),
            "openapi", List.of("openapi", "swagger", "api", "contrato", "endpoint", "rest", "4xx", "5xx"),
            "seguranca", List.of("seguranca", "security", "sql", "injection", "credencial", "secret", "critical"),
            "migration", List.of("migration", "flyway", "liquibase", "sql", "drop", "schema"));

    private ContextualRagAnswerBuilder() {
    }

    public static String build(String question, String analysisContext) {
        String context = analysisContext != null ? analysisContext : "";
        String summary = extractSection(context, SUMMARY_MARKER, RISKS_MARKER).trim();
        if (summary.isEmpty()) {
            summary = extractSection(context, SUMMARY_MARKER, CHUNKS_MARKER).trim();
        }
        if (summary.isEmpty()) {
            summary = "Sem resumo disponível";
        }
        List<ParsedRisk> risks = parseRisks(context);
        List<ChunkRef> chunks = parseChunks(context);
        String normalized = normalize(question);

        if (normalized.isEmpty() || isGreeting(normalized)) {
            return greetingAnswer(summary, risks, chunks);
        }
        if (isClarification(normalized)) {
            return clarificationAnswer(summary, risks, chunks);
        }
        return targetedAnswer(question, summary, risks, chunks, normalized);
    }

    private static String greetingAnswer(String summary, List<ParsedRisk> risks, List<ChunkRef> chunks) {
        StringBuilder sb = new StringBuilder();
        sb.append("Olá! Sou o assistente arquitetural deste projeto.\n\n");
        if (!risks.isEmpty()) {
            sb.append(formatRiskOverview(risks)).append("\n\n");
        } else {
            sb.append(trimTo(summary, 400)).append("\n\n");
        }
        if (!chunks.isEmpty()) {
            sb.append("Artefatos indexados: ");
            sb.append(chunks.stream().map(ChunkRef::filePath).distinct().limit(5)
                    .collect(Collectors.joining(", ")));
            sb.append(".\n\nExemplos: \"Quais riscos críticos?\", \"Problemas no Docker?\", \"OpenAPI?\"");
        }
        return sb.toString().trim();
    }

    private static String clarificationAnswer(String summary, List<ParsedRisk> risks, List<ChunkRef> chunks) {
        StringBuilder sb = new StringBuilder();
        sb.append("Em termos simples:\n\n");
        if (!risks.isEmpty()) {
            sb.append("• ").append(formatRiskOverview(risks)).append('\n');
            sb.append("• Principais achados:\n");
            for (int i = 0; i < Math.min(3, risks.size()); i++) {
                ParsedRisk risk = risks.get(i);
                sb.append("  — [").append(risk.severity()).append("] ")
                        .append(risk.title());
                if (risk.filePath() != null && !"-".equals(risk.filePath())) {
                    sb.append(" (`").append(risk.filePath()).append("`)");
                }
                sb.append('\n');
            }
        } else {
            sb.append("• ").append(simplifySummary(summary)).append('\n');
        }
        if (!chunks.isEmpty()) {
            sb.append("\nTrecho de evidência:\n— ").append(chunks.get(0).filePath())
                    .append(":\n").append(trimTo(chunks.get(0).content(), 240));
        }
        sb.append("\n\nPergunte de forma específica (Docker, OpenAPI, testes/CI, SQL).");
        return sb.toString().trim();
    }

    private static String targetedAnswer(
            String question,
            String summary,
            List<ParsedRisk> risks,
            List<ChunkRef> chunks,
            String normalized) {
        List<String> terms = questionTerms(normalized);
        String topic = detectTopic(normalized, terms);
        List<ParsedRisk> matchedRisks = filterRisks(risks, terms, topic);

        if (isRiskOverviewQuestion(normalized) && !risks.isEmpty()) {
            return formatRiskListAnswer(question, risks);
        }

        StringBuilder sb = new StringBuilder();

        if (!matchedRisks.isEmpty()) {
            sb.append("Achados relacionados à sua pergunta");
            if (topic != null) {
                sb.append(" (").append(topicLabel(topic)).append(')');
            }
            sb.append(":\n\n");
            for (int i = 0; i < Math.min(5, matchedRisks.size()); i++) {
                sb.append(formatRiskDetail(matchedRisks.get(i))).append("\n\n");
            }
        } else if (!risks.isEmpty()) {
            sb.append("Não encontrei riscos que combinem diretamente com \"")
                    .append(question.trim()).append("\".\n\n");
            boolean addedTopic = false;
            if (topic != null) {
                List<ParsedRisk> topicRisks = filterRisks(risks, TOPIC_KEYWORDS.get(topic), topic);
                if (!topicRisks.isEmpty()) {
                    addedTopic = true;
                    sb.append("Riscos do tema ").append(topicLabel(topic)).append(":\n\n");
                    for (int i = 0; i < Math.min(3, topicRisks.size()); i++) {
                        sb.append(formatRiskDetail(topicRisks.get(i))).append("\n\n");
                    }
                }
            }
            if (!addedTopic) {
                sb.append("Visão geral: ").append(formatRiskOverview(risks)).append("\n\n");
                sb.append("Maior prioridade:\n").append(formatRiskDetail(risks.get(0)));
            }
        } else {
            sb.append(trimTo(summary, 350));
        }

        List<ChunkRef> rankedChunks = rankChunks(chunks, terms, topic);
        if (!rankedChunks.isEmpty()) {
            sb.append("\n\nEvidência no código/documentação:\n");
            for (int i = 0; i < Math.min(2, rankedChunks.size()); i++) {
                ChunkRef chunk = rankedChunks.get(i);
                sb.append("\n— `").append(chunk.filePath()).append("`:\n");
                sb.append(trimTo(chunk.content(), 280)).append('\n');
            }
        }

        return sb.toString().trim();
    }

    private static String formatRiskListAnswer(String question, List<ParsedRisk> risks) {
        StringBuilder sb = new StringBuilder();
        sb.append("Riscos identificados na análise (").append(risks.size()).append("):\n\n");
        sb.append(formatRiskOverview(risks)).append("\n\n");
        for (ParsedRisk risk : risks) {
            sb.append(formatRiskDetail(risk)).append("\n\n");
        }
        sb.append("_Pergunte sobre um tema (Docker, OpenAPI, SQL) para detalhar um achado._");
        return sb.toString().trim();
    }

    private static String formatRiskOverview(List<ParsedRisk> risks) {
        long critical = risks.stream().filter(r -> "CRITICAL".equals(r.severity())).count();
        long high = risks.stream().filter(r -> "HIGH".equals(r.severity())).count();
        long medium = risks.stream().filter(r -> "MEDIUM".equals(r.severity())).count();
        long low = risks.stream().filter(r -> "LOW".equals(r.severity())).count();
        return new StringBuilder()
                .append(risks.size()).append(" risco(s): ")
                .append(critical).append(" crítico(s), ")
                .append(high).append(" alto(s), ")
                .append(medium).append(" médio(s), ")
                .append(low).append(" baixo(s).")
                .toString();
    }

    private static String formatRiskDetail(ParsedRisk risk) {
        StringBuilder sb = new StringBuilder();
        sb.append("**[").append(risk.severity()).append("] ").append(risk.title()).append("**");
        if (risk.filePath() != null && !"-".equals(risk.filePath())) {
            sb.append(" — `").append(risk.filePath()).append('`');
        }
        if (risk.description() != null && !risk.description().isBlank()) {
            sb.append("\n").append(trimTo(risk.description(), 300));
        }
        if (risk.suggestion() != null && !risk.suggestion().isBlank()) {
            sb.append("\n→ ").append(trimTo(risk.suggestion(), 200));
        }
        return sb.toString();
    }

    private static boolean isRiskOverviewQuestion(String normalized) {
        return normalized.matches(".*\\b(riscos?|achados?|problemas?|severidade|prioridade|criticos?|critical)\\b.*");
    }

    private static String detectTopic(String normalized, List<String> terms) {
        for (Map.Entry<String, List<String>> entry : TOPIC_KEYWORDS.entrySet()) {
            for (String keyword : entry.getValue()) {
                if (normalized.contains(keyword) || terms.contains(keyword)) {
                    return entry.getKey();
                }
            }
        }
        return null;
    }

    private static String topicLabel(String topic) {
        return switch (topic) {
            case "testes" -> "testes/CI";
            case "docker" -> "Docker/containers";
            case "openapi" -> "OpenAPI/contrato";
            case "seguranca" -> "segurança";
            case "migration" -> "migrations/SQL";
            default -> topic;
        };
    }

    private static List<ParsedRisk> filterRisks(List<ParsedRisk> risks, List<String> terms, String topic) {
        if (risks.isEmpty()) {
            return List.of();
        }
        List<ParsedRisk> filtered = risks.stream()
                .filter(r -> scoreRisk(r, terms, topic) > 0)
                .sorted(Comparator.comparingInt(r -> severityOrder(r.severity())))
                .toList();
        if (!filtered.isEmpty()) {
            return filtered;
        }
        if (topic != null) {
            List<String> topicKeywords = TOPIC_KEYWORDS.getOrDefault(topic, List.of());
            return risks.stream()
                    .filter(r -> scoreRisk(r, topicKeywords, null) > 0)
                    .sorted(Comparator.comparingInt(r -> severityOrder(r.severity())))
                    .toList();
        }
        return List.of();
    }

    private static int scoreRisk(ParsedRisk risk, List<String> terms, String topic) {
        String haystack = (risk.title() + " " + risk.description() + " " + risk.filePath()
                + " " + risk.category()).toLowerCase(Locale.ROOT);
        int score = 0;
        for (String term : terms) {
            if (haystack.contains(term)) {
                score += 2;
            }
        }
        if (topic != null) {
            for (String keyword : TOPIC_KEYWORDS.getOrDefault(topic, List.of())) {
                if (haystack.contains(keyword)) {
                    score += 3;
                }
            }
        }
        return score;
    }

    private static int severityOrder(String severity) {
        return switch (severity) {
            case "CRITICAL" -> 0;
            case "HIGH" -> 1;
            case "MEDIUM" -> 2;
            case "LOW" -> 3;
            default -> 9;
        };
    }

    private static List<ChunkRef> rankChunks(List<ChunkRef> chunks, List<String> terms, String topic) {
        if (chunks.isEmpty()) {
            return List.of();
        }
        return chunks.stream()
                .sorted((a, b) -> Integer.compare(scoreChunk(b, terms, topic), scoreChunk(a, terms, topic)))
                .filter(c -> scoreChunk(c, terms, topic) > 0)
                .toList();
    }

    private static int scoreChunk(ChunkRef chunk, List<String> terms, String topic) {
        String haystack = (chunk.filePath() + " " + chunk.content()).toLowerCase(Locale.ROOT);
        int score = 0;
        for (String term : terms) {
            if (haystack.contains(term)) {
                score += 2;
            }
        }
        if (topic != null) {
            for (String keyword : TOPIC_KEYWORDS.getOrDefault(topic, List.of())) {
                if (haystack.contains(keyword)) {
                    score += 2;
                }
            }
        }
        return score;
    }

    private static List<String> questionTerms(String normalized) {
        return Arrays.stream(normalized.split("\\s+"))
                .map(String::trim)
                .filter(t -> t.length() >= 3)
                .filter(t -> !STOPWORDS.contains(t))
                .distinct()
                .toList();
    }

    private static List<ParsedRisk> parseRisks(String context) {
        String body = extractSection(context, RISKS_MARKER, CHUNKS_MARKER).trim();
        if (body.isEmpty()) {
            return List.of();
        }
        List<ParsedRisk> risks = new ArrayList<>();
        String[] lines = body.split("\n");
        ParsedRisk current = null;
        for (String line : lines) {
            Matcher matcher = RISK_LINE.matcher(line.trim());
            if (matcher.matches()) {
                if (current != null) {
                    risks.add(current);
                }
                current = new ParsedRisk(
                        matcher.group(1),
                        matcher.group(2),
                        matcher.group(3),
                        matcher.group(4),
                        null,
                        null);
                continue;
            }
            if (current == null || line.isBlank()) {
                continue;
            }
            String trimmed = line.trim();
            if (trimmed.startsWith("Sugestão:")) {
                current = current.withSuggestion(trimmed.substring("Sugestão:".length()).trim());
            } else if (trimmed.startsWith("  ")) {
                current = current.withDescription(trimmed.trim());
            }
        }
        if (current != null) {
            risks.add(current);
        }
        risks.sort(Comparator.comparingInt(r -> severityOrder(r.severity())));
        return risks;
    }

    private static List<ChunkRef> parseChunks(String context) {
        int start = context.indexOf(CHUNKS_MARKER);
        if (start < 0) {
            return List.of();
        }
        String body = context.substring(start + CHUNKS_MARKER.length()).trim();
        if (body.isEmpty()) {
            return List.of();
        }

        List<ChunkRef> chunks = new ArrayList<>();
        String[] blocks = body.split("\n--- ");
        for (String block : blocks) {
            String trimmed = block.startsWith("--- ") ? block.substring(4) : block;
            int newline = trimmed.indexOf('\n');
            if (newline <= 0) {
                continue;
            }
            String headerLine = trimmed.substring(0, newline);
            String content = trimmed.substring(newline + 1).trim();
            Matcher matcher = CHUNK_HEADER.matcher("--- " + headerLine);
            if (!matcher.matches()) {
                matcher = CHUNK_HEADER.matcher(headerLine);
            }
            if (matcher.matches()) {
                chunks.add(new ChunkRef(matcher.group(1), content));
            }
        }
        return chunks;
    }

    private static String extractSection(String context, String startMarker, String endMarker) {
        int start = context.indexOf(startMarker);
        if (start < 0) {
            return "";
        }
        start += startMarker.length();
        int end = context.indexOf(endMarker, start);
        if (end < 0) {
            return context.substring(start);
        }
        return context.substring(start, end);
    }

    private static String simplifySummary(String summary) {
        String oneLine = summary.replace('\n', ' ').trim();
        if (oneLine.length() <= 200) {
            return oneLine;
        }
        return oneLine.substring(0, 197) + "...";
    }

    private static String trimTo(String text, int max) {
        if (text == null) {
            return "";
        }
        String t = text.trim();
        if (t.length() <= max) {
            return t;
        }
        return t.substring(0, max - 3) + "...";
    }

    private static String normalize(String question) {
        if (question == null) {
            return "";
        }
        return question.trim()
                .toLowerCase(Locale.ROOT)
                .replace('á', 'a').replace('ã', 'a').replace('â', 'a')
                .replace('é', 'e').replace('ê', 'e')
                .replace('í', 'i')
                .replace('ó', 'o').replace('õ', 'o').replace('ô', 'o')
                .replace('ú', 'u')
                .replace('ç', 'c');
    }

    private static boolean isGreeting(String normalized) {
        return normalized.matches("^(ola|oi|hey|hello|bom dia|boa tarde|boa noite)([!.? ]|$).*");
    }

    private static boolean isClarification(String normalized) {
        return normalized.contains("nao entendi")
                || normalized.contains("não entendi")
                || normalized.contains("explica")
                || normalized.contains("explique")
                || normalized.contains("o que significa")
                || normalized.contains("nao compreendi");
    }

    private record ParsedRisk(
            String severity,
            String title,
            String filePath,
            String category,
            String description,
            String suggestion) {
        ParsedRisk withDescription(String value) {
            return new ParsedRisk(severity, title, filePath, category, value, suggestion);
        }

        ParsedRisk withSuggestion(String value) {
            return new ParsedRisk(severity, title, filePath, category, description, value);
        }
    }

    private record ChunkRef(String filePath, String content) {
    }
}
