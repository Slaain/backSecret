package myavocat.legit.service;

import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import myavocat.legit.model.TemplateDocument;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.nio.file.Path;
import java.util.Map;

@Service
public class DocumentGeneratorService {

    public byte[] generatePdfFromTemplate(TemplateDocument template, Map<String, String> variables) {
        try {
            String rawHtml = wrapWithLayout(template.getContent());
            String filledHtml = replaceVariables(rawHtml, variables);

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            PdfRendererBuilder builder = new PdfRendererBuilder();
            builder.useFastMode();
            builder.withHtmlContent(filledHtml, null);
            builder.toStream(outputStream);
            builder.run();

            return outputStream.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Erreur lors de la génération du PDF: " + e.getMessage(), e);
        }
    }

    public void savePdfToFile(byte[] pdfBytes, Path outputPath) {
        try (OutputStream out = new FileOutputStream(outputPath.toFile())) {
            out.write(pdfBytes);
        } catch (Exception e) {
            throw new RuntimeException("Erreur lors de la sauvegarde du PDF: " + e.getMessage(), e);
        }
    }

    private String replaceVariables(String html, Map<String, String> variables) {
        String result = html;
        for (Map.Entry<String, String> entry : variables.entrySet()) {
            result = result.replace("{{" + entry.getKey() + "}}", entry.getValue());
        }
        return result;
    }

    private String wrapWithLayout(String content) {
        return """
    <html>
    <head>
        <style>
            body { font-family: Arial, sans-serif; margin: 50px; }
            header, footer { text-align: center; font-size: 12px; margin-bottom: 30px; }
            footer { margin-top: 50px; }
            main { font-size: 14px; line-height: 1.6; }
        </style>
    </head>
    <body>
        <main>
            %s
        </main>
    </body>
    </html>
    """.formatted(content);
    }

}
