package myavocat.legit.controller;

import myavocat.legit.model.Document;
import myavocat.legit.service.DocumentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/documents")
public class DocumentController {

    private final DocumentService documentService;

    @Autowired
    public DocumentController(DocumentService documentService) {
        this.documentService = documentService;
    }

    @PostMapping("/upload")
    public ResponseEntity<?> uploadDocument(
            @RequestParam("file") MultipartFile file,
            @RequestParam("dossierId") UUID dossierId,
            @RequestParam("userId") UUID userId,
            @RequestParam("typeFichier") String typeFichier,
            @RequestParam(value = "description", required = false) String description
    ) {
        try {
            Document savedDocument = documentService.uploadDocument(file, dossierId, userId, typeFichier, description);
            return ResponseEntity.ok(savedDocument);
        } catch (IOException e) {
            return ResponseEntity.status(500).body("Erreur lors du téléchargement du fichier.");
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/dossier/{dossierId}")
    public ResponseEntity<List<Document>> getDocumentsByDossier(@PathVariable UUID dossierId) {
        return ResponseEntity.ok(documentService.getDocumentsByDossier(dossierId));
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<Document>> getDocumentsByUser(@PathVariable UUID userId) {
        return ResponseEntity.ok(documentService.getDocumentsByUser(userId));
    }

    @GetMapping("/{documentId}/download")
    public ResponseEntity<?> downloadDocument(@PathVariable UUID documentId) {
        Document document = documentService.getDocumentById(documentId);
        Path path = Paths.get(document.getCheminFichier());

        if (!Files.exists(path)) {
            return ResponseEntity.notFound().build();
        }

        Resource fileResource = new FileSystemResource(path.toFile());

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + document.getNomFichier() + "\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(fileResource);
    }
}
