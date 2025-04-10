package myavocat.legit.service;

import myavocat.legit.model.Document;
import myavocat.legit.model.Dossier;
import myavocat.legit.model.User;
import myavocat.legit.repository.DocumentRepository;
import myavocat.legit.repository.DossierRepository;
import myavocat.legit.repository.UserRepository;
import org.apache.commons.io.FilenameUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;

@Service
public class DocumentService {

    private final DocumentRepository documentRepository;
    private final DossierRepository dossierRepository;
    private final UserRepository userRepository;

    // Utiliser un chemin absolu qui correspond au volume Docker
    private final String uploadDir = "/app/uploads/";

    @Autowired
    public DocumentService(DocumentRepository documentRepository, DossierRepository dossierRepository, UserRepository userRepository) {
        this.documentRepository = documentRepository;
        this.dossierRepository = dossierRepository;
        this.userRepository = userRepository;

        // Créer le répertoire de base s'il n'existe pas
        try {
            Files.createDirectories(Paths.get(uploadDir));
            System.out.println("✅ Répertoire d'upload initialisé: " + uploadDir);
        } catch (IOException e) {
            System.err.println("⚠️ Impossible de créer le répertoire d'upload: " + e.getMessage());
        }
    }

    public Document uploadDocument(MultipartFile file, UUID dossierId, UUID userId, String typeFichier, String description) throws IOException {
        // Vérification MIME et extension
        System.out.println("📥 Upload demandé - dossierId: " + dossierId + ", userId: " + userId);
        String mimeType = file.getContentType();
        String extension = FilenameUtils.getExtension(file.getOriginalFilename()).toLowerCase();

        if (!"application/pdf".equals(mimeType) || !"pdf".equals(extension)) {
            throw new RuntimeException("Seuls les fichiers PDF sont autorisés.");
        }

        Dossier dossier = dossierRepository.findById(dossierId)
                .orElseThrow(() -> new RuntimeException("Dossier non trouvé"));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

        String originalFilename = file.getOriginalFilename();
        String uniqueFilename = UUID.randomUUID() + "_" + originalFilename;

        // Chemin absolu pour stocker les fichiers
        Path filePath = Paths.get(uploadDir, dossierId.toString(), uniqueFilename);
        System.out.println("📂 Chemin de fichier: " + filePath.toAbsolutePath());

        // Création des répertoires nécessaires
        Files.createDirectories(filePath.getParent());

        // Copie du fichier
        Files.copy(file.getInputStream(), filePath);
        System.out.println("✅ Fichier enregistré: " + filePath);

        Document document = new Document();
        document.setNomFichier(originalFilename);
        document.setCheminFichier(filePath.toString());
        document.setTypeFichier(typeFichier);
        document.setDescription(description);
        document.setUploadedBy(user);
        document.setDossier(dossier);

        return documentRepository.save(document);
    }

    public List<Document> getDocumentsByDossier(UUID dossierId) {
        return documentRepository.findByDossierId(dossierId);
    }

    public List<Document> getDocumentsByUser(UUID userId) {
        return documentRepository.findByUploadedById(userId);
    }

    public Document getDocumentById(UUID id) {
        Document document = documentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Document non trouvé"));

        // Vérifier si le fichier existe
        Path filePath = Paths.get(document.getCheminFichier());
        if (!Files.exists(filePath)) {
            System.out.println("⚠️ Le fichier n'existe pas: " + filePath);
            // Vous pourriez décider de gérer cette situation différemment
        }

        return document;
    }
}