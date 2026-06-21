package dev.archlens.infrastructure.gateway;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Respostas do chat em modo local (sem LLM externo): usa resumo da análise e trechos RAG.
 */
public final class ContextualRagAnswerBuilder {

    private static final String SUMMARY_MARKER = "=== Resumo da Análise ===";
    private static final String CHUNKS_MARKER = "=== Trechos Relevantes do Código-Fonte ===";
    private static final Pattern CHUNK_HEADER = Pattern.compile(
            "^--- (.+?) \\(chunk (\\d+), score ([0-9.]+)\\) ---$");
    private static final Set<String> STOPWORDS = Set.of(
            "a", "o", "os", "as", "de", "da", "do", "dos", "das", "e", "em", "na", "no", "nas", "nos",
            "um", "uma", "uns", "umas", "que", "por", "para", "com", "sem", "sobre", "como", "qual", "quais",
            "me", "eu", "voce", "você", "isso", "esta", "este", "essa", "esse", "ao", "aos", "à", "ou");

    private ContextualRagAnswerBuilder() {
    }

    public static String build(String question, String analysisContext) {
        String context = analysisContext != null ? analysisContext : "";
        String summary = extractSection(context, SUMMARY_MARKER, CHUNKS_MARKER).trim();
        if (summary.isEmpty()) {
            summary = "Sem resumo disponível";
        }
        List<ChunkRef> chunks = parseChunks(context);
        String normalized = normalize(question);

        if (normalized.isEmpty() || isGreeting(normalized)) {
            return greetingAnswer(summary, chunks);
        }
        if (isClarification(normalized)) {
            return clarificationAnswer(summary, chunks);
        }
        return targetedAnswer(question, summary, chunks, normalized);
    }

    private static String greetingAnswer(String summary, List<ChunkRef> chunks) {
        StringBuilder sb = new StringBuilder();
        sb.append("Olá! Sou o assistente arquitetural deste projeto.\n\n");
        sb.append("Resumo da análise:\n").append(trimTo(summary, 600)).append('\n');
        if (!chunks.isEmpty()) {
            sb.append("\nArtefatos indexados relevantes: ");
            sb.append(chunks.stream()
                    .map(c -> c.filePath())
                    .distinct()
                    .limit(5)
                    .collect(Collectors.joining(", ")));
            sb.append(".\n\nPergunte, por exemplo: \"Quais riscos no Docker?\" ou \"Problemas na API OpenAPI?\"");
        }
        return sb.toString().trim();
    }

    private static String clarificationAnswer(String summary, List<ChunkRef> chunks) {
        StringBuilder sb = new StringBuilder();
        sb.append("Em termos simples:\n\n");
        sb.append("• A análise estática encontrou pontos de atenção na arquitetura do projeto.\n");
        sb.append("• ").append(simplifySummary(summary)).append('\n');
        if (!chunks.isEmpty()) {
            sb.append("\nTrechos do código/documentação que sustentam isso:\n");
            for (int i = 0; i < Math.min(3, chunks.size()); i++) {
                ChunkRef chunk = chunks.get(i);
                sb.append("\n— ").append(chunk.filePath()).append(":\n");
                sb.append(trimTo(chunk.content(), 280)).append('\n');
            }
        }
        sb.append("\nFaça uma pergunta mais específica (ex.: migrations, CI, acoplamento) para detalhar.");
        return sb.toString().trim();
    }

    private static String targetedAnswer(
            String question,
            String summary,
            List<ChunkRef> chunks,
            String normalized) {
        List<String> terms = questionTerms(normalized);
        List<ChunkRef> ranked = rankChunks(chunks, terms);

        StringBuilder sb = new StringBuilder();
        sb.append("Sobre \"").append(question.trim()).append("\":\n\n");
        sb.append(trimTo(summary, 400));

        if (!ranked.isEmpty()) {
            sb.append("\n\nEvidências nos artefatos:\n");
            for (int i = 0; i < Math.min(3, ranked.size()); i++) {
                ChunkRef chunk = ranked.get(i);
                sb.append("\n— ").append(chunk.filePath()).append(":\n");
                sb.append(trimTo(chunk.content(), 320)).append('\n');
            }
        } else if (!chunks.isEmpty()) {
            sb.append("\n\nTrechos relacionados encontrados:\n");
            ChunkRef chunk = chunks.get(0);
            sb.append("\n— ").append(chunk.filePath()).append(":\n");
            sb.append(trimTo(chunk.content(), 320));
        } else {
            sb.append("\n\nNão encontrei trechos indexados para esta pergunta. "
                    + "Confirme se o upload foi concluído e a indexação terminou (status READY).");
        }

        return sb.toString().trim();
    }

    private static List<ChunkRef> rankChunks(List<ChunkRef> chunks, List<String> terms) {
        if (terms.isEmpty()) {
            return chunks;
        }
        return chunks.stream()
                .sorted((a, b) -> Integer.compare(scoreChunk(b, terms), scoreChunk(a, terms)))
                .filter(c -> scoreChunk(c, terms) > 0)
                .toList();
    }

    private static int scoreChunk(ChunkRef chunk, List<String> terms) {
        String haystack = (chunk.filePath() + " " + chunk.content()).toLowerCase(Locale.ROOT);
        int score = 0;
        for (String term : terms) {
            if (haystack.contains(term)) {
                score++;
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

    private record ChunkRef(String filePath, String content) {
    }
}
