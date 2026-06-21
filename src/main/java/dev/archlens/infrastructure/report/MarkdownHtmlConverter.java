package dev.archlens.infrastructure.report;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Conversor markdown mínimo para HTML de relatório executivo (PDF).
 */
final class MarkdownHtmlConverter {

    private static final Pattern TABLE_ROW = Pattern.compile("^\\|(.+)\\|$");
    private static final Pattern INLINE_BOLD = Pattern.compile("\\*\\*(.+?)\\*\\*");
    private static final Pattern INLINE_ITALIC = Pattern.compile("_(.+?)_");
    private static final Pattern INLINE_CODE = Pattern.compile("`([^`]+)`");

    private MarkdownHtmlConverter() {
    }

    static String convert(String markdown) {
        if (markdown == null || markdown.isBlank()) {
            return "";
        }

        StringBuilder html = new StringBuilder();
        List<String> lines = markdown.lines().toList();
        int i = 0;

        while (i < lines.size()) {
            String line = lines.get(i);
            String trimmed = line.trim();

            if (trimmed.isEmpty()) {
                i++;
                continue;
            }

            if (trimmed.startsWith("|") && trimmed.endsWith("|")) {
                i = renderTable(html, lines, i);
                continue;
            }

            if (trimmed.startsWith("#")) {
                html.append(renderHeading(trimmed)).append('\n');
                i++;
                continue;
            }

            if (trimmed.equals("---")) {
                html.append("<hr/>\n");
                i++;
                continue;
            }

            if (trimmed.startsWith("- ")) {
                i = renderList(html, lines, i, "ul");
                continue;
            }

            html.append("<p>").append(inlineFormat(escapeHtml(trimmed))).append("</p>\n");
            i++;
        }

        return html.toString();
    }

    private static int renderTable(StringBuilder html, List<String> lines, int start) {
        List<List<String>> rows = new ArrayList<>();
        int i = start;
        while (i < lines.size()) {
            String trimmed = lines.get(i).trim();
            if (!trimmed.startsWith("|") || !trimmed.endsWith("|")) {
                break;
            }
            Matcher matcher = TABLE_ROW.matcher(trimmed);
            if (!matcher.matches()) {
                break;
            }
            rows.add(parseCells(matcher.group(1)));
            i++;
        }

        if (rows.isEmpty()) {
            return start + 1;
        }

        html.append("<table>\n");
        boolean headerDone = false;
        for (List<String> row : rows) {
            if (!headerDone && row.stream().allMatch(cell -> cell.matches("-+"))) {
                headerDone = true;
                continue;
            }
            String tag = !headerDone ? "th" : "td";
            html.append("<tr>");
            for (String cell : row) {
                html.append('<').append(tag).append('>')
                        .append(inlineFormat(escapeHtml(cell.trim())))
                        .append("</").append(tag).append('>');
            }
            html.append("</tr>\n");
            headerDone = true;
        }
        html.append("</table>\n");
        return i;
    }

    private static List<String> parseCells(String rowContent) {
        List<String> cells = new ArrayList<>();
        for (String part : rowContent.split("\\|", -1)) {
            cells.add(part.trim());
        }
        if (!cells.isEmpty() && cells.get(0).isEmpty()) {
            cells.remove(0);
        }
        if (!cells.isEmpty() && cells.get(cells.size() - 1).isEmpty()) {
            cells.remove(cells.size() - 1);
        }
        return cells;
    }

    private static int renderList(StringBuilder html, List<String> lines, int start, String tag) {
        html.append('<').append(tag).append(">\n");
        int i = start;
        while (i < lines.size()) {
            String trimmed = lines.get(i).trim();
            if (!trimmed.startsWith("- ")) {
                break;
            }
            html.append("<li>").append(inlineFormat(escapeHtml(trimmed.substring(2).trim()))).append("</li>\n");
            i++;
        }
        html.append("</").append(tag).append(">\n");
        return i;
    }

    private static String renderHeading(String line) {
        int level = 0;
        while (level < line.length() && line.charAt(level) == '#') {
            level++;
        }
        if (level < 1 || level > 4) {
            return "<p>" + inlineFormat(escapeHtml(line)) + "</p>";
        }
        String text = line.substring(level).trim();
        return "<h" + level + ">" + inlineFormat(escapeHtml(text)) + "</h" + level + ">";
    }

    private static String inlineFormat(String text) {
        String result = INLINE_CODE.matcher(text).replaceAll("<code>$1</code>");
        result = INLINE_BOLD.matcher(result).replaceAll("<strong>$1</strong>");
        result = INLINE_ITALIC.matcher(result).replaceAll("<em>$1</em>");
        return result;
    }

    private static String escapeHtml(String value) {
        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }
}
