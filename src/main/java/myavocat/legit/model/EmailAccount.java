package myavocat.legit.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "email_accounts")
@Getter
@Setter
@NoArgsConstructor
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class EmailAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user; // L'avocat propriétaire

    @Column(nullable = false)
    private String emailAddress; // Email de l'avocat

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EmailProvider provider; // GMAIL, OUTLOOK, OTHER

    @Column(nullable = false)
    private boolean isActive = false; // Webhooks actifs ou non

    // Tokens OAuth chiffrés (null pour IMAP)
    @Column(name = "access_token", columnDefinition = "TEXT")
    private String encryptedAccessToken;

    @Column(name = "refresh_token", columnDefinition = "TEXT")
    private String encryptedRefreshToken;

    @Column(name = "token_expires_at")
    private LocalDateTime tokenExpiresAt;

    @Column(name = "last_history_id")
    private String lastHistoryId; //

    // Configuration IMAP (pour fallback)
    @Column(name = "imap_host")
    private String imapHost;

    @Column(name = "imap_port")
    private Integer imapPort = 993;

    @Column(name = "imap_password")
    private String encryptedImapPassword;

    // Métadonnées
    @Column(name = "last_sync_at")
    private LocalDateTime lastSyncAt;

    @Column(name = "webhook_subscription_id")
    private String webhookSubscriptionId; // ID subscription Google/Microsoft

    @Column(name = "webhook_expiry")
    private LocalDateTime webhookExpiry;

    @Column(name = "sync_errors_count")
    private int syncErrorsCount = 0;

    @Column(name = "last_error_message", columnDefinition = "TEXT")
    private String lastErrorMessage;

    // Timestamps
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();

        // Auto-détection du provider basé sur l'email
        if (emailAddress != null) {
            provider = detectProvider(emailAddress);
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // Enum pour les fournisseurs d'email
    public enum EmailProvider {
        GMAIL("Gmail"),
        OUTLOOK("Outlook"),
        OTHER("Autre");

        private final String displayName;

        EmailProvider(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    // Méthodes utilitaires

    /**
     * Auto-détection du fournisseur email
     */
    private EmailProvider detectProvider(String email) {
        String domain = email.substring(email.indexOf("@") + 1).toLowerCase();

        if (domain.contains("gmail.com")) {
            return EmailProvider.GMAIL;
        } else if (domain.contains("outlook.com") || domain.contains("hotmail.com") ||
                domain.contains("live.com") || domain.contains("office365.com")) {
            return EmailProvider.OUTLOOK;
        } else {
            return EmailProvider.OTHER;
        }
    }

    /**
     * Vérifie si les tokens OAuth sont valides
     */
    public boolean hasValidOAuthTokens() {
        return encryptedAccessToken != null &&
                tokenExpiresAt != null &&
                tokenExpiresAt.isAfter(LocalDateTime.now().plusMinutes(5));
    }

    /**
     * Vérifie si le webhook est actif et valide
     */
    public boolean hasActiveWebhook() {
        return isActive &&
                webhookSubscriptionId != null &&
                webhookExpiry != null &&
                webhookExpiry.isAfter(LocalDateTime.now().plusHours(1));
    }

    /**
     * Vérifie si l'account est prêt pour la synchronisation
     */
    public boolean isReadyForSync() {
        if (provider == EmailProvider.OTHER) {
            // Pour IMAP, vérifier la config
            return imapHost != null && encryptedImapPassword != null;
        } else {
            // Pour OAuth, vérifier les tokens
            return hasValidOAuthTokens();
        }
    }

    /**
     * Incrémenter le compteur d'erreurs
     */
    public void incrementErrorCount(String errorMessage) {
        this.syncErrorsCount++;
        this.lastErrorMessage = errorMessage;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Reset du compteur d'erreurs après succès
     */
    public void resetErrorCount() {
        this.syncErrorsCount = 0;
        this.lastErrorMessage = null;
        this.lastSyncAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Désactiver le compte après trop d'erreurs
     */
    public boolean shouldDisable() {
        return syncErrorsCount >= 5; // Seuil configurable
    }

    /**
     * Configuration automatique de l'host IMAP
     */
    public void configureImapHost() {
        if (provider == EmailProvider.OTHER && imapHost == null) {
            String domain = emailAddress.substring(emailAddress.indexOf("@") + 1).toLowerCase();

            if (domain.contains("yahoo.com")) {
                this.imapHost = "imap.mail.yahoo.com";
            } else if (domain.contains("free.fr")) {
                this.imapHost = "imap.free.fr";
            } else if (domain.contains("orange.fr")) {
                this.imapHost = "imap.orange.fr";
            } else {
                // Format standard
                this.imapHost = "imap." + domain;
            }
        }
    }

    /**
     * Représentation textuelle pour les logs
     */
    @Override
    public String toString() {
        return String.format("EmailAccount{user=%s, email=%s, provider=%s, active=%s}",
                user != null ? user.getEmail() : "null",
                emailAddress,
                provider,
                isActive);
    }
}
