package dev.archlens.infrastructure.report;

import java.io.ByteArrayOutputStream;
import java.util.Optional;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;

import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class PdfReportRenderer {

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
        String escaped = escapeHtml(markdownBody)
                .replace("\n\n", "</p><p>")
                .replace("\n", "<br/>");

        String logoBlock = logoUrl.filter(url -> !url.isBlank())
                .map(url -> "<img src=\"" + escapeHtml(url) + "\" alt=\"logo\" style=\"max-height:48px;margin-bottom:16px;\"/>")
                .orElse("");

        return """
                <!DOCTYPE html>
                <html><head><meta charset="UTF-8"/><style>
                body { font-family: Helvetica, Arial, sans-serif; font-size: 11pt; color: #1a1a1a; margin: 40px; }
                h1 { font-size: 20pt; color: #0f172a; border-bottom: 2px solid #2563eb; padding-bottom: 8px; }
                h2 { font-size: 14pt; color: #1e40af; margin-top: 24px; }
                h3 { font-size: 12pt; color: #334155; }
                p { line-height: 1.5; }
                .header { margin-bottom: 24px; }
                .brand { color: #64748b; font-size: 9pt; }
                </style></head><body>
                <div class="header">%s<p class="brand">%s — %s</p></div>
                <p>%s</p>
                </body></html>
                """.formatted(logoBlock, escapeHtml(brandName), escapeHtml(projectName), escaped);
    }

    private static String escapeHtml(String value) {
        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }
}
