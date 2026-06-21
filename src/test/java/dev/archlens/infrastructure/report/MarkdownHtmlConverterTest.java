package dev.archlens.infrastructure.report;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class MarkdownHtmlConverterTest {

    @Test
    void convertsHeadingsTablesAndBold() {
        String md = """
                # Título

                ## Sumário executivo

                **Prioridade imediata:** tratar riscos.

                | Severidade | Quantidade |
                |---|---|
                | HIGH | 2 |

                - item um
                - item dois
                """;

        String html = MarkdownHtmlConverter.convert(md);

        assertTrue(html.contains("<h1>Título</h1>"));
        assertTrue(html.contains("<h2>Sumário executivo</h2>"));
        assertTrue(html.contains("<strong>Prioridade imediata:</strong>"));
        assertTrue(html.contains("<table>"));
        assertTrue(html.contains("<th>Severidade</th>"));
        assertTrue(html.contains("<td>HIGH</td>"));
        assertTrue(html.contains("<ul>"));
    }
}
