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

    private final String uploadDir = "uploads/";

    @Autowired
    public DocumentService(DocumentRepository documentRepository, DossierRepository dossierRepository, UserRepository userRepository) {
        this.documentRepository = documentRepository;
        this.dossierRepository = dossierRepository;
        this.userRepository = userRepository;
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

        Path filePath = Paths.get(uploadDir + dossierId + "/" + uniqueFilename);
        Files.createDirectories(filePath.getParent());
        Files.copy(file.getInputStream(), filePath);

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
        return documentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Document non trouvé"));
    }

}
