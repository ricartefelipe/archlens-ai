package dev.archlens.infrastructure.report;

import java.io.ByteArrayOutputStream;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;

import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class PdfReportRenderer {

    private static final DateTimeFormatter GENERATED_AT =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm 'UTC'");

    @ConfigProperty(name = "archlens.report.logo-url")
    Optional<String> logoUrl;

    @ConfigProperty(name = "archlens.report.brand-name", defaultValue = "ArchLens")
    String brandName;

    public byte[] render(String markdownBody, String projectName) {
        String html = buildHtml(markdownBody, projectName);
        try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            PdfRendererBuilder builder = new PdfRendererBuilder();
            builder.useFastMode();
            builder.withHtmlContent(html, null);
            builder.toStream(output);
            builder.run();
            return output.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate PDF report", e);
        }
    }

    private String buildHtml(String markdownBody, String projectName) {
        String bodyHtml = MarkdownHtmlConverter.convert(markdownBody);

        String logoBlock = logoUrl.filter(url -> !url.isBlank())
                .map(url -> "<img src=\"" + escapeAttr(url) + "\" alt=\"logo\" class=\"logo\"/>")
                .orElse("");

        String generatedAt = GENERATED_AT.format(ZonedDateTime.now(ZoneOffset.UTC));

        return """
                <!DOCTYPE html>
                <html><head><meta charset="UTF-8"/><style>
                @page { size: A4; margin: 22mm 18mm 24mm 18mm; }
                body { font-family: Helvetica, Arial, sans-serif; font-size: 10.5pt; color: #0f172a; line-height: 1.45; }
                .cover { border-bottom: 3px solid #2563eb; padding-bottom: 16px; margin-bottom: 28px; }
                .logo { max-height: 52px; margin-bottom: 12px; display: block; }
                .brand { color: #64748b; font-size: 9pt; letter-spacing: 0.04em; text-transform: uppercase; }
                .project { font-size: 22pt; font-weight: 700; color: #0f172a; margin: 8px 0 4px; }
                .meta { font-size: 9pt; color: #64748b; }
                h1 { font-size: 18pt; color: #0f172a; border-bottom: 2px solid #2563eb; padding-bottom: 6px; margin: 28px 0 12px; page-break-after: avoid; }
                h2 { font-size: 13pt; color: #1e40af; margin: 22px 0 10px; page-break-after: avoid; }
                h3 { font-size: 11pt; color: #334155; margin: 16px 0 8px; page-break-after: avoid; }
                p { margin: 0 0 10px; }
                ul { margin: 0 0 12px 18px; padding: 0; }
                li { margin-bottom: 4px; }
                table { width: 100%%; border-collapse: collapse; margin: 12px 0 18px; font-size: 9.5pt; }
                th, td { border: 1px solid #cbd5e1; padding: 7px 9px; text-align: left; vertical-align: top; }
                th { background: #eff6ff; color: #1e3a8a; font-weight: 600; }
                tr:nth-child(even) td { background: #f8fafc; }
                code { font-family: Consolas, monospace; font-size: 9pt; background: #f1f5f9; padding: 1px 4px; border-radius: 3px; }
                hr { border: none; border-top: 1px solid #e2e8f0; margin: 20px 0; }
                .footer { margin-top: 28px; padding-top: 10px; border-top: 1px solid #e2e8f0; font-size: 8.5pt; color: #64748b; }
                </style></head><body>
                <div class="cover">
                  %s
                  <p class="brand">%s</p>
                  <p class="project">%s</p>
                  <p class="meta">Relatório de diagnóstico arquitetural · Gerado em %s</p>
                </div>
                %s
                <div class="footer">Documento gerado automaticamente com evidências rastreáveis. Revisão humana recomendada antes de decisões executivas.</div>
                </body></html>
                """.formatted(
                logoBlock,
                escapeHtml(brandName),
                escapeHtml(projectName),
                generatedAt,
                bodyHtml);
    }

    private static String escapeHtml(String value) {
        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }

    private static String escapeAttr(String value) {
        return escapeHtml(value);
    }
}
