package myavocat.legit.controller;

import myavocat.legit.model.Snippet;
import myavocat.legit.service.SnippetService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/snippets")
public class SnippetController {

    private final SnippetService snippetService;

    @Autowired
    public SnippetController(SnippetService snippetService) {
        this.snippetService = snippetService;
    }

    // Création d’une intro
    @PostMapping("/intros")
    public ResponseEntity<Snippet> createIntro(@RequestBody Map<String, String> payload) {
        UUID userId = UUID.fromString(payload.get("userId"));
        String label = payload.get("label");
        String content = payload.get("content");
        return ResponseEntity.ok(snippetService.createSnippet(userId, "intro", label, content));
    }

    // Création d’une conclusion
    @PostMapping("/conclusions")
    public ResponseEntity<Snippet> createConclusion(@RequestBody Map<String, String> payload) {
        UUID userId = UUID.fromString(payload.get("userId"));
        String label = payload.get("label");
        String content = payload.get("content");
        return ResponseEntity.ok(snippetService.createSnippet(userId, "conclusion", label, content));
    }

    // Liste des intros
    @GetMapping("/intros")
    public ResponseEntity<List<Snippet>> getIntros(@RequestParam UUID userId) {
        return ResponseEntity.ok(snippetService.getSnippetsByUserAndType(userId, "intro"));
    }

    // Liste des conclusions
    @GetMapping("/conclusions")
    public ResponseEntity<List<Snippet>> getConclusions(@RequestParam UUID userId) {
        return ResponseEntity.ok(snippetService.getSnippetsByUserAndType(userId, "conclusion"));
    }
}