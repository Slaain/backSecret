package myavocat.legit.controller;

import myavocat.legit.model.TemplateDocument;
import myavocat.legit.service.TemplateDocumentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/templates")
public class TemplateDocumentController {

    private final TemplateDocumentService templateService;

    @Autowired
    public TemplateDocumentController(TemplateDocumentService templateService) {
        this.templateService = templateService;
    }

    @PostMapping
    public ResponseEntity<TemplateDocument> createTemplate(
            @RequestParam UUID userId,
            @RequestParam String title,
            @RequestParam String content
    ) {
        TemplateDocument created = templateService.createTemplate(userId, title, content);
        return ResponseEntity.ok(created);
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<TemplateDocument>> getTemplatesByUser(@PathVariable UUID userId) {
        return ResponseEntity.ok(templateService.getTemplatesByUser(userId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<TemplateDocument> getTemplateById(@PathVariable UUID id) {
        return ResponseEntity.ok(templateService.getTemplateById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<TemplateDocument> updateTemplate(
            @PathVariable UUID id,
            @RequestParam String title,
            @RequestParam String content
    ) {
        return ResponseEntity.ok(templateService.updateTemplate(id, title, content));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTemplate(@PathVariable UUID id) {
        templateService.deleteTemplate(id);
        return ResponseEntity.noContent().build();
    }
}
