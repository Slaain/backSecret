package myavocat.legit.service;

import myavocat.legit.model.*;
import myavocat.legit.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class EmailWebhookService {

    private static final Logger logger = LoggerFactory.getLogger(EmailWebhookService.class);

    @Autowired
    private EmailAccountRepository emailAccountRepository;

    @Autowired
    private EmailWebhookLogRepository webhookLogRepository;

    @Autowired
    private ClientRepository clientRepository;

    @Autowired
    private DossierRepository dossierRepository;

    @Autowired
    private DocumentService documentService;

    // Extensions de fichiers autorisées
    private static final List<String> ALLOWED_EXTENSIONS = Arrays.asList(
            "pdf", "jpg", "jpeg", "png", "doc", "docx", "txt"
    );

    // Statuts dossier considérés comme actifs
    private static final List<String> ACTIVE_STATUSES = Arrays.asList(
            "En cours", "En attente"
    );

    /**
     * Point d'entrée principal pour traiter un webhook email
     */
    public EmailWebhookLog processEmailWebhook(
            EmailWebhookLog.WebhookType webhookType,
            String emailAccountId,
            String senderEmail,
            String emailSubject,
            String emailMessageId,
            String emailThreadId,
            List<AttachmentData> attachments,
            String rawPayload) {

        logger.info("Processing webhook: type={}, sender={}, messageId={}",
                webhookType, senderEmail, emailMessageId);

        // Créer le log de webhook
        EmailWebhookLog log = createWebhookLog(webhookType, emailAccountId, senderEmail,
                emailSubject, emailMessageId, emailThreadId, attachments.size(), rawPayload);

        try {
            log.startProcessing();
            webhookLogRepository.save(log);

            // Vérifier si déjà traité (éviter doublons)
            if (isAlreadyProcessed(emailMessageId)) {
                log.markAsIgnored("Message déjà traité");
                webhookLogRepository.save(log);
                return log;
            }

            // Identifier le client
            Optional<Client> clientOpt = identifyClient(senderEmail);
            if (clientOpt.isEmpty()) {
                log.markAsIgnored("Client non trouvé pour l'email: " + senderEmail);
                webhookLogRepository.save(log);
                return log;
            }

            Client client = clientOpt.get();
            log.setClient(client); // ✅ associer au log (remplit client_id en DB)
            logger.info("Client identifié: {} {}", client.getNom(), client.getPrenom());


            // Trouver le dossier actif
            Optional<Dossier> dossierOpt = findActiveDossier(client, log.getEmailAccount().getUser());
            if (dossierOpt.isEmpty()) {
                log.markAsIgnored("Aucun dossier actif trouvé pour le client: " + client.getEmail());
                webhookLogRepository.save(log);
                return log;
            }

            Dossier dossier = dossierOpt.get();
            log.setDossier(dossier);
            logger.info("Dossier actif trouvé: {} ({})", dossier.getNomDossier(), dossier.getStatut());

            // Traiter les pièces jointes
            processAttachments(attachments, dossier, log.getEmailAccount().getUser(), log);

            // Déterminer le statut final
            if (log.getAttachmentsProcessed() == log.getAttachmentsCount()) {
                log.markAsSuccess();
            } else if (log.getAttachmentsProcessed() > 0) {
                log.markAsPartialSuccess("Certaines pièces jointes ont échoué");
            } else {
                log.markAsFailed("Aucune pièce jointe n'a pu être traitée", null);
            }

            webhookLogRepository.save(log);
            return log;

        } catch (Exception e) {
            logger.error("Erreur lors du traitement du webhook", e);
            log.markAsFailed(e.getMessage(), getStackTrace(e));
            webhookLogRepository.save(log);
            return log;
        }
    }

    /**
     * Créer le log initial du webhook
     */
    private EmailWebhookLog createWebhookLog(EmailWebhookLog.WebhookType webhookType,
                                             String emailAccountId, String senderEmail, String emailSubject,
                                             String emailMessageId, String emailThreadId, int attachmentCount, String rawPayload) {

        EmailAccount emailAccount = emailAccountRepository.findById(UUID.fromString(emailAccountId))
                .orElseThrow(() -> new RuntimeException("EmailAccount non trouvé: " + emailAccountId));

        EmailWebhookLog log = new EmailWebhookLog();
        log.setEmailAccount(emailAccount);
        log.setWebhookType(webhookType);
        log.setStatus(EmailWebhookLog.ProcessingStatus.RECEIVED);
        log.setSenderEmail(senderEmail);
        log.setEmailSubject(emailSubject);
        log.setEmailMessageId(emailMessageId);
        log.setEmailThreadId(emailThreadId);
        log.setAttachmentsCount(attachmentCount);
        log.setWebhookPayload(rawPayload);
        log.setWebhookReceivedAt(LocalDateTime.now());

        return webhookLogRepository.save(log);
    }

    /**
     * Vérifier si le message a déjà été traité
     */
    private boolean isAlreadyProcessed(String emailMessageId) {
        return webhookLogRepository.existsByEmailMessageId(emailMessageId);
    }

    /**
     * Identifier le client par son email
     */
    private Optional<Client> identifyClient(String senderEmail) {
        String cleanEmail = extractCleanEmail(senderEmail)
                .toLowerCase()
                .trim();

        logger.info("🔍 Recherche client par email: '{}'", cleanEmail);

        return clientRepository.findByEmailIgnoreCase(cleanEmail);
    }


    /**
     * Trouver le dossier actif pour un client
     */
    private Optional<Dossier> findActiveDossier(Client client, User lawyer) {
        // Chercher dans les relations directes
        List<Dossier> directDossiers = dossierRepository.findByClientAndAvocat(client, lawyer);

        // Chercher dans les relations Many-to-Many
        List<Dossier> multiDossiers = dossierRepository.findByClientsContainingAndAvocat(client, lawyer);

        // Combiner les résultats
        Set<Dossier> allDossiers = new HashSet<>();
        allDossiers.addAll(directDossiers);
        allDossiers.addAll(multiDossiers);

        // Filtrer par statut actif et prendre le plus récent
        return allDossiers.stream()
                .filter(d -> ACTIVE_STATUSES.contains(d.getStatut()))
                .max(Comparator.comparing(Dossier::getCreatedAt));
    }

    /**
     * Traiter toutes les pièces jointes
     */
    private void processAttachments(List<AttachmentData> attachments, Dossier dossier,
                                    User lawyer, EmailWebhookLog log) {

        List<String> processedFilenames = new ArrayList<>();

        for (AttachmentData attachment : attachments) {
            try {
                if (isAllowedFileType(attachment.getFilename())) {
                    processAttachment(attachment, dossier, lawyer);
                    log.incrementProcessedAttachments();
                    processedFilenames.add(attachment.getFilename());
                    logger.info("Pièce jointe traitée: {}", attachment.getFilename());
                } else {
                    log.incrementFailedAttachments();
                    logger.info("Pièce jointe ignorée (type non autorisé): {}", attachment.getFilename());
                }
            } catch (Exception e) {
                log.incrementFailedAttachments();
                logger.error("Erreur traitement pièce jointe {}: {}", attachment.getFilename(), e.getMessage());
            }
        }

        // Sauvegarder la liste des fichiers traités
        if (!processedFilenames.isEmpty()) {
            log.setAttachmentFilenames(String.join(", ", processedFilenames));
        }
    }

    /**
     * Traiter une pièce jointe individuelle
     */
    private void processAttachment(AttachmentData attachment, Dossier dossier, User lawyer) throws IOException {
        // Créer un MultipartFile à partir des données
        MultipartFile multipartFile = new AttachmentMultipartFile(
                attachment.getFilename(),
                attachment.getContentType(),
                attachment.getData()
        );

        // Utiliser le DocumentService existant
        documentService.uploadDocument(
                multipartFile,
                dossier.getId(),
                lawyer.getId(),
                "EMAIL_ATTACHMENT",
                "Pièce jointe reçue par email le " + LocalDateTime.now()
        );
    }

    /**
     * Vérifier si le type de fichier est autorisé
     */
    private boolean isAllowedFileType(String filename) {
        if (filename == null || !filename.contains(".")) {
            return false;
        }

        String extension = filename.substring(filename.lastIndexOf(".") + 1).toLowerCase();
        return ALLOWED_EXTENSIONS.contains(extension);
    }

    /**
     * Extraire l'email propre (sans nom)
     */
    private String extractCleanEmail(String fullEmail) {
        if (fullEmail == null) return "";
        if (fullEmail.contains("<") && fullEmail.contains(">")) {
            int start = fullEmail.indexOf("<") + 1;
            int end = fullEmail.indexOf(">");
            return fullEmail.substring(start, end).trim().toLowerCase();
        }
        return fullEmail.trim().toLowerCase();
    }


    /**
     * Obtenir la stack trace comme string
     */
    private String getStackTrace(Exception e) {
        java.io.StringWriter sw = new java.io.StringWriter();
        java.io.PrintWriter pw = new java.io.PrintWriter(sw);
        e.printStackTrace(pw);
        return sw.toString();
    }


    // Classes utilitaires

    /**
     * Classe pour représenter les données d'une pièce jointe
     */
    public static class AttachmentData {
        private String filename;
        private String contentType;
        private byte[] data;

        public AttachmentData(String filename, String contentType, byte[] data) {
            this.filename = filename;
            this.contentType = contentType;
            this.data = data;
        }

        // Getters
        public String getFilename() { return filename; }
        public String getContentType() { return contentType; }
        public byte[] getData() { return data; }
    }

    /**
     * Implémentation MultipartFile pour les pièces jointes
     */
    private static class AttachmentMultipartFile implements MultipartFile {
        private final String filename;
        private final String contentType;
        private final byte[] data;

        public AttachmentMultipartFile(String filename, String contentType, byte[] data) {
            this.filename = filename;
            this.contentType = contentType;
            this.data = data;
        }

        @Override
        public String getName() { return filename; }

        @Override
        public String getOriginalFilename() { return filename; }

        @Override
        public String getContentType() { return contentType; }

        @Override
        public boolean isEmpty() { return data.length == 0; }

        @Override
        public long getSize() { return data.length; }

        @Override
        public byte[] getBytes() { return data; }

        @Override
        public InputStream getInputStream() { return new ByteArrayInputStream(data); }

        @Override
        public void transferTo(java.io.File dest) throws IOException {
            try (java.io.FileOutputStream fos = new java.io.FileOutputStream(dest)) {
                fos.write(data);
            }
        }
    }
}
