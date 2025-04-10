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
        try {
            Document document = documentService.getDocumentById(documentId);
            System.out.println("Document trouvé: " + document.getNomFichier());

            // Le problème est ici - le chemin stocké en BDD est juste "uploads/..."
            // alors que le chemin réel dans le conteneur est "/app/uploads/..."
            String cheminOriginal = document.getCheminFichier();
            System.out.println("Chemin stocké en BDD: " + cheminOriginal);

            // Si le chemin ne commence pas par "/app/" et qu'il commence par "uploads/"
            Path path;
            if (cheminOriginal.startsWith("uploads/") && !cheminOriginal.startsWith("/app/")) {
                // Nous devons remplacer "uploads/" par "/app/uploads/"
                path = Paths.get("/app/" + cheminOriginal);
            } else {
                // Sinon, utiliser le chemin tel quel
                path = Paths.get(cheminOriginal);
            }

            System.out.println("Chemin final utilisé: " + path.toAbsolutePath());

            if (!Files.exists(path)) {
                System.out.println("Fichier non trouvé: " + path.toAbsolutePath());
                return ResponseEntity.notFound().build();
            }

            Resource fileResource = new FileSystemResource(path.toFile());
            System.out.println("Fichier trouvé et prêt pour téléchargement");

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + document.getNomFichier() + "\"")
                    .contentType(MediaType.APPLICATION_PDF)
                    .body(fileResource);
        } catch (Exception e) {
            System.err.println("Erreur lors du téléchargement: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(500).body("Erreur lors du téléchargement: " + e.getMessage());
        }
    }
}
