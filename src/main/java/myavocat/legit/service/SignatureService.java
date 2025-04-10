package myavocat.legit.service;

import jakarta.servlet.http.HttpServletRequest;
import myavocat.legit.model.Document;
import myavocat.legit.model.Signature;
import myavocat.legit.model.User;
import myavocat.legit.repository.DocumentRepository;
import myavocat.legit.repository.SignatureRepository;
import myavocat.legit.repository.UserRepository;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Base64;
import java.util.UUID;

@Service
public class SignatureService {
    private static final Logger logger = LoggerFactory.getLogger(SignatureService.class);

    private final SignatureRepository signatureRepository;
    private final DocumentRepository documentRepository;
    private final UserRepository userRepository;

    @Value("${app.upload-dir:/app/uploads/}")
    private String uploadDir;

    @Autowired
    public SignatureService(SignatureRepository signatureRepository,
                            DocumentRepository documentRepository,
                            UserRepository userRepository) {
        this.signatureRepository = signatureRepository;
        this.documentRepository = documentRepository;
        this.userRepository = userRepository;
    }

    public Signature signerDocument(UUID documentId, UUID userId, String base64Image, HttpServletRequest request) {
        logger.info("Début de la signature - documentId: {}, userId: {}", documentId, userId);

        File tempImageFile = null;
        try {
            // Vérifier et récupérer le document
            Document document = documentRepository.findById(documentId)
                    .orElseThrow(() -> {
                        logger.error("Document introuvable avec ID: {}", documentId);
                        return new IllegalArgumentException("Document introuvable");
                    });
            logger.info("Document trouvé: {}", document.getCheminFichier());

            // Vérifier et récupérer l'utilisateur
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> {
                        logger.error("Utilisateur introuvable avec ID: {}", userId);
                        return new IllegalArgumentException("Utilisateur introuvable");
                    });
            logger.info("Utilisateur trouvé: {}", user.getId());

            // Vérifier que le document existe physiquement
            File inputPdf = new File(document.getCheminFichier());
            if (!inputPdf.exists()) {
                logger.error("Le fichier PDF n'existe pas: {}", document.getCheminFichier());
                throw new RuntimeException("Le fichier PDF n'existe pas: " + document.getCheminFichier());
            }

            // Vérifier l'image base64
            if (base64Image == null || base64Image.isEmpty()) {
                logger.error("L'image de signature est vide");
                throw new IllegalArgumentException("L'image de signature est vide");
            }

            // Nettoyer la chaîne base64
            String base64Clean;
            if (base64Image.contains(",")) {
                base64Clean = base64Image.split(",")[1];
            } else if (base64Image.startsWith("data:image")) {
                base64Clean = base64Image.replaceFirst("^data:image/[^;]+;base64,", "");
            } else {
                base64Clean = base64Image;
            }

            // Décoder l'image
            byte[] imageBytes;
            try {
                imageBytes = Base64.getDecoder().decode(base64Clean);
                logger.info("Image décodée avec succès, taille: {} bytes", imageBytes.length);
            } catch (IllegalArgumentException e) {
                logger.error("Format base64 invalide: {}", e.getMessage());
                throw new IllegalArgumentException("Format d'image invalide", e);
            }

            // Créer le répertoire temporaire si nécessaire
            File tempDir = new File(uploadDir);
            if (!tempDir.exists()) {
                boolean created = tempDir.mkdirs();
                if (!created) {
                    logger.error("Impossible de créer le répertoire: {}", tempDir.getAbsolutePath());
                    throw new RuntimeException("Impossible de créer le répertoire temporaire");
                }
            }

            // Écrire l'image temporaire
            tempImageFile = new File(uploadDir + "signature-" + userId + ".png");
            logger.info("Écriture de l'image temporaire: {}", tempImageFile.getAbsolutePath());

            try (OutputStream os = new FileOutputStream(tempImageFile)) {
                os.write(imageBytes);
                logger.info("Image temporaire créée avec succès");
            } catch (IOException e) {
                logger.error("Erreur lors de l'écriture de l'image: {}", e.getMessage(), e);
                throw new RuntimeException("Erreur lors de la conversion de l'image", e);
            }

            // Manipuler le PDF
            logger.info("Ouverture du PDF: {}", inputPdf.getAbsolutePath());
            try (PDDocument pdf = PDDocument.load(inputPdf)) {
                PDPage page = pdf.getPage(0); // on signe la première page
                logger.info("PDF chargé, nombre de pages: {}", pdf.getNumberOfPages());

                PDImageXObject pdImage = PDImageXObject.createFromFile(tempImageFile.getAbsolutePath(), pdf);
                logger.info("Image chargée pour insertion dans le PDF");

                try (PDPageContentStream contentStream = new PDPageContentStream(pdf, page, PDPageContentStream.AppendMode.APPEND, true)) {
                    // Position bas droite
                    PDRectangle mediaBox = page.getMediaBox();
                    float x = mediaBox.getWidth() - 160;
                    float y = 60;
                    float width = 120;
                    float height = 50;

                    contentStream.drawImage(pdImage, x, y, width, height);
                    logger.info("Image insérée dans le PDF");
                }

                // Préparer le chemin du fichier signé
                String signedPath = document.getCheminFichier().replace(".pdf", "-signe.pdf");
                File signedFile = new File(signedPath);
                File signedDir = signedFile.getParentFile();

                // Créer le répertoire de destination si nécessaire
                if (!signedDir.exists()) {
                    boolean created = signedDir.mkdirs();
                    if (!created) {
                        logger.error("Impossible de créer le répertoire de destination: {}", signedDir.getAbsolutePath());
                    }
                }

                // Sauvegarder le PDF signé
                logger.info("Sauvegarde du PDF signé: {}", signedPath);
                pdf.save(signedPath);
                logger.info("PDF signé sauvegardé avec succès");
            } catch (IOException e) {
                logger.error("Erreur I/O lors de la manipulation du PDF: {}", e.getMessage(), e);
                throw new RuntimeException("Erreur lors de la manipulation du PDF", e);
            }

            // Mettre à jour le document avec le nouveau chemin
            logger.info("Mise à jour du document dans la base de données");
            String signedPath = document.getCheminFichier().replace(".pdf", "-signe.pdf");
            document.setCheminFichier(signedPath);
            documentRepository.save(document);

            // Enregistrer la signature
            logger.info("Création de l'entrée de signature dans la base de données");
            Signature signature = new Signature();
            signature.setDocument(document);
            signature.setSignedBy(user);
            signature.setIpAddress(request.getRemoteAddr());

            Signature savedSignature = signatureRepository.save(signature);
            logger.info("Signature enregistrée avec succès, ID: {}", savedSignature.getId());

            return savedSignature;

        } catch (Exception e) {
            logger.error("Erreur lors de la signature: {}", e.getMessage(), e);
            throw e;
        } finally {
            // Supprimer le fichier temporaire
            if (tempImageFile != null && tempImageFile.exists()) {
                boolean deleted = tempImageFile.delete();
                if (!deleted) {
                    logger.warn("Impossible de supprimer le fichier temporaire: {}", tempImageFile.getAbsolutePath());
                }
            }
        }
    }
}
