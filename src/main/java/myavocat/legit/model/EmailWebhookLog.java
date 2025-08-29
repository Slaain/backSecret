package myavocat.legit.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "email_webhook_logs")
@Getter
@Setter
@NoArgsConstructor
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class EmailWebhookLog {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "email_account_id", nullable = false)
    private EmailAccount emailAccount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private WebhookType webhookType; // GMAIL_PUBSUB, OUTLOOK_GRAPH, IMAP_POLL

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ProcessingStatus status; // RECEIVED, PROCESSING, SUCCESS, FAILED, IGNORED

    // Informations de l'email source
    @Column(name = "sender_email")
    private String senderEmail;

    @Column(name = "email_subject")
    private String emailSubject;

    @Column(name = "email_message_id")
    private String emailMessageId; // ID unique du message

    @Column(name = "email_thread_id")
    private String emailThreadId;

    // Client et dossier identifiés
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id")
    private Client client;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dossier_id")
    private Dossier dossier;

    // Pièces jointes traitées
    @Column(name = "attachments_count")
    private int attachmentsCount = 0;

    @Column(name = "attachments_processed")
    private int attachmentsProcessed = 0;

    @Column(name = "attachments_failed")
    private int attachmentsFailed = 0;

    @Column(name = "attachment_filenames", columnDefinition = "TEXT")
    private String attachmentFilenames; // JSON array des noms de fichiers

    // Payload webhook brut (pour debug)
    @Column(name = "webhook_payload", columnDefinition = "TEXT")
    private String webhookPayload;

    // Informations de traitement
    @Column(name = "processing_duration_ms")
    private Long processingDurationMs;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "error_stack_trace", columnDefinition = "TEXT")
    private String errorStackTrace;

    // Timestamps
    @Column(name = "webhook_received_at", nullable = false)
    private LocalDateTime webhookReceivedAt;

    @Column(name = "processing_started_at")
    private LocalDateTime processingStartedAt;

    @Column(name = "processing_completed_at")
    private LocalDateTime processingCompletedAt;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (webhookReceivedAt == null) {
            webhookReceivedAt = LocalDateTime.now();
        }
    }

    // Enums

    public enum WebhookType {
        GMAIL_PUBSUB("Gmail Pub/Sub"),
        OUTLOOK_GRAPH("Outlook Graph API"),
        IMAP_POLL("IMAP Polling");

        private final String displayName;

        WebhookType(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    public enum ProcessingStatus {
        RECEIVED("Reçu"),
        PROCESSING("En traitement"),
        SUCCESS("Succès"),
        FAILED("Échoué"),
        IGNORED("Ignoré"),
        PARTIAL_SUCCESS("Succès partiel");

        private final String displayName;

        ProcessingStatus(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    // Méthodes utilitaires

    /**
     * Démarrer le traitement
     */
    public void startProcessing() {
        this.status = ProcessingStatus.PROCESSING;
        this.processingStartedAt = LocalDateTime.now();
    }

    /**
     * Marquer comme succès
     */
    public void markAsSuccess() {
        this.status = ProcessingStatus.SUCCESS;
        this.processingCompletedAt = LocalDateTime.now();
        calculateProcessingDuration();
    }

    /**
     * Marquer comme succès partiel
     */
    public void markAsPartialSuccess(String reason) {
        this.status = ProcessingStatus.PARTIAL_SUCCESS;
        this.errorMessage = reason;
        this.processingCompletedAt = LocalDateTime.now();
        calculateProcessingDuration();
    }

    /**
     * Marquer comme échec
     */
    public void markAsFailed(String errorMessage, String stackTrace) {
        this.status = ProcessingStatus.FAILED;
        this.errorMessage = errorMessage;
        this.errorStackTrace = stackTrace;
        this.processingCompletedAt = LocalDateTime.now();
        calculateProcessingDuration();
    }

    /**
     * Marquer comme ignoré
     */
    public void markAsIgnored(String reason) {
        this.status = ProcessingStatus.IGNORED;
        this.errorMessage = reason;
        this.processingCompletedAt = LocalDateTime.now();
        calculateProcessingDuration();
    }

    /**
     * Calculer la durée de traitement
     */
    private void calculateProcessingDuration() {
        if (processingStartedAt != null && processingCompletedAt != null) {
            this.processingDurationMs = java.time.Duration.between(
                    processingStartedAt, processingCompletedAt).toMillis();
        }
    }

    /**
     * Incrémenter le nombre de pièces jointes traitées avec succès
     */
    public void incrementProcessedAttachments() {
        this.attachmentsProcessed++;
    }

    /**
     * Incrémenter le nombre de pièces jointes échouées
     */
    public void incrementFailedAttachments() {
        this.attachmentsFailed++;
    }

    /**
     * Vérifier si le traitement est terminé
     */
    public boolean isProcessingComplete() {
        return status == ProcessingStatus.SUCCESS ||
                status == ProcessingStatus.FAILED ||
                status == ProcessingStatus.IGNORED ||
                status == ProcessingStatus.PARTIAL_SUCCESS;
    }

    /**
     * Vérifier si le traitement a réussi (complètement ou partiellement)
     */
    public boolean isSuccessful() {
        return status == ProcessingStatus.SUCCESS ||
                status == ProcessingStatus.PARTIAL_SUCCESS;
    }

    /**
     * Obtenir un résumé du traitement des pièces jointes
     */
    public String getAttachmentsProcessingSummary() {
        if (attachmentsCount == 0) {
            return "Aucune pièce jointe";
        }

        return String.format("%d/%d traitées (%d échouées)",
                attachmentsProcessed,
                attachmentsCount,
                attachmentsFailed);
    }

    /**
     * Obtenir la durée de traitement formatée
     */
    public String getFormattedProcessingDuration() {
        if (processingDurationMs == null) {
            return "N/A";
        }

        if (processingDurationMs < 1000) {
            return processingDurationMs + "ms";
        } else {
            return String.format("%.2fs", processingDurationMs / 1000.0);
        }
    }

    /**
     * Représentation textuelle pour les logs
     */
    @Override
    public String toString() {
        return String.format("WebhookLog{type=%s, status=%s, sender=%s, client=%s, dossier=%s, attachments=%s}",
                webhookType,
                status,
                senderEmail,
                client != null ? client.getEmail() : "null",
                dossier != null ? dossier.getNomDossier() : "null",
                getAttachmentsProcessingSummary());
    }
}
