package myavocat.legit.controller;

import myavocat.legit.model.Snippet;
import myavocat.legit.model.TemplateDocument;
import myavocat.legit.service.DocumentGeneratorService;
import myavocat.legit.service.SnippetService;
import myavocat.legit.service.TemplateDocumentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/templates")
public class DocumentGenerationController {

    private final TemplateDocumentService templateService;
    private final DocumentGeneratorService generatorService;
    private final SnippetService snippetService;

    @Autowired
    public DocumentGenerationController(
            TemplateDocumentService templateService,
            DocumentGeneratorService generatorService,
            SnippetService snippetService
    ) {
        this.templateService = templateService;
        this.generatorService = generatorService;
        this.snippetService = snippetService;
    }

    @PostMapping("/{id}/generate")
    public ResponseEntity<byte[]> generatePdfFromTemplate(
            @PathVariable UUID id,
            @RequestBody Map<String, Object> payload
    ) {
        // Extraction
        Map<String, String> variables = (Map<String, String>) payload.get("variables");

        String introContent = "";
        String conclusionContent = "";

        if (payload.containsKey("introId")) {
            UUID introId = UUID.fromString(payload.get("introId").toString());
            Snippet intro = snippetService.getSnippetById(introId);
            introContent = intro.getContent();
        }

        if (payload.containsKey("conclusionId")) {
            UUID conclusionId = UUID.fromString(payload.get("conclusionId").toString());
            Snippet conclusion = snippetService.getSnippetById(conclusionId);
            conclusionContent = conclusion.getContent();
        }

        TemplateDocument template = templateService.getTemplateById(id);

        // Construction du contenu global
        String fullContent = introContent + template.getContent() + conclusionContent;
        template.setContent(fullContent);

        // Génération PDF
        byte[] pdf = generatorService.generatePdfFromTemplate(template, variables);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + template.getTitle() + ".pdf\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }
}
