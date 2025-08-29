package myavocat.legit.repository;

import myavocat.legit.model.EmailWebhookLog;
import myavocat.legit.model.EmailAccount;
import myavocat.legit.model.Client;
import myavocat.legit.model.Dossier;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface EmailWebhookLogRepository extends JpaRepository<EmailWebhookLog, UUID> {

    // Recherche par compte email
    List<EmailWebhookLog> findByEmailAccount(EmailAccount emailAccount);
    List<EmailWebhookLog> findByEmailAccountId(UUID emailAccountId);
    Page<EmailWebhookLog> findByEmailAccountIdOrderByWebhookReceivedAtDesc(UUID emailAccountId, Pageable pageable);

    // Recherche par type de webhook
    List<EmailWebhookLog> findByWebhookType(EmailWebhookLog.WebhookType webhookType);
    Page<EmailWebhookLog> findByWebhookTypeOrderByWebhookReceivedAtDesc(EmailWebhookLog.WebhookType webhookType, Pageable pageable);

    // Recherche par statut
    List<EmailWebhookLog> findByStatus(EmailWebhookLog.ProcessingStatus status);
    List<EmailWebhookLog> findByStatusIn(List<EmailWebhookLog.ProcessingStatus> statuses);
    Page<EmailWebhookLog> findByStatusOrderByWebhookReceivedAtDesc(EmailWebhookLog.ProcessingStatus status, Pageable pageable);

    // Recherche par expéditeur
    List<EmailWebhookLog> findBySenderEmail(String senderEmail);
    Page<EmailWebhookLog> findBySenderEmailOrderByWebhookReceivedAtDesc(String senderEmail, Pageable pageable);

    // Recherche par client et dossier
    List<EmailWebhookLog> findByClient(Client client);
    List<EmailWebhookLog> findByDossier(Dossier dossier);
    List<EmailWebhookLog> findByClientAndDossier(Client client, Dossier dossier);

    // Recherche par ID de message email (éviter les doublons)
    Optional<EmailWebhookLog> findByEmailMessageId(String emailMessageId);
    boolean existsByEmailMessageId(String emailMessageId);
    List<EmailWebhookLog> findByEmailThreadId(String emailThreadId);

    // Recherche temporelle
    List<EmailWebhookLog> findByWebhookReceivedAtBetween(LocalDateTime start, LocalDateTime end);
    List<EmailWebhookLog> findByWebhookReceivedAtAfter(LocalDateTime after);

    @Query("SELECT ewl FROM EmailWebhookLog ewl WHERE ewl.webhookReceivedAt >= :since ORDER BY ewl.webhookReceivedAt DESC")
    List<EmailWebhookLog> findRecentLogs(@Param("since") LocalDateTime since);

    // Logs en échec
    List<EmailWebhookLog> findByStatusOrderByWebhookReceivedAtDesc(EmailWebhookLog.ProcessingStatus status);


    @Query("SELECT ewl FROM EmailWebhookLog ewl WHERE ewl.status = 'FAILED' AND ewl.webhookReceivedAt >= :since")
    List<EmailWebhookLog> findFailedLogsSince(@Param("since") LocalDateTime since);

    // Logs en cours de traitement (potentiellement bloqués)
    @Query("SELECT ewl FROM EmailWebhookLog ewl WHERE ewl.status = 'PROCESSING' AND ewl.processingStartedAt < :timeout")
    List<EmailWebhookLog> findStuckProcessingLogs(@Param("timeout") LocalDateTime timeout);

    // Statistiques de performance
    @Query("SELECT AVG(ewl.processingDurationMs) FROM EmailWebhookLog ewl WHERE ewl.processingDurationMs IS NOT NULL AND ewl.status = 'SUCCESS'")
    Double getAverageProcessingTime();

    @Query("SELECT MAX(ewl.processingDurationMs) FROM EmailWebhookLog ewl WHERE ewl.processingDurationMs IS NOT NULL")
    Long getMaxProcessingTime();

    @Query("SELECT COUNT(ewl), ewl.status FROM EmailWebhookLog ewl WHERE ewl.webhookReceivedAt >= :since GROUP BY ewl.status")
    List<Object[]> getStatusCountsSince(@Param("since") LocalDateTime since);

    @Query("SELECT COUNT(ewl), ewl.webhookType FROM EmailWebhookLog ewl WHERE ewl.webhookReceivedAt >= :since GROUP BY ewl.webhookType")
    List<Object[]> getWebhookTypeCountsSince(@Param("since") LocalDateTime since);

    // Statistiques des pièces jointes
    @Query("SELECT SUM(ewl.attachmentsProcessed), SUM(ewl.attachmentsFailed) FROM EmailWebhookLog ewl WHERE ewl.webhookReceivedAt >= :since")
    List<Object[]> getAttachmentStatsSince(@Param("since") LocalDateTime since);

    @Query("SELECT ewl FROM EmailWebhookLog ewl WHERE ewl.attachmentsCount > 0 AND ewl.attachmentsFailed > 0 ORDER BY ewl.webhookReceivedAt DESC")
    List<EmailWebhookLog> findLogsWithFailedAttachments();

    // Recherche par cabinet (office)
    @Query("SELECT ewl FROM EmailWebhookLog ewl WHERE ewl.emailAccount.user.office.id = :officeId ORDER BY ewl.webhookReceivedAt DESC")
    Page<EmailWebhookLog> findByOfficeId(@Param("officeId") UUID officeId, Pageable pageable);

    @Query("SELECT ewl FROM EmailWebhookLog ewl WHERE ewl.emailAccount.user.office.id = :officeId AND ewl.webhookReceivedAt >= :since")
    List<EmailWebhookLog> findByOfficeIdSince(@Param("officeId") UUID officeId, @Param("since") LocalDateTime since);

    // Recherche avancée et debugging
    @Query("SELECT ewl FROM EmailWebhookLog ewl WHERE " +
            "ewl.senderEmail LIKE %:searchTerm% OR " +
            "ewl.emailSubject LIKE %:searchTerm% OR " +
            "ewl.errorMessage LIKE %:searchTerm%")
    List<EmailWebhookLog> searchLogs(@Param("searchTerm") String searchTerm);

    // Logs avec erreurs spécifiques
    @Query("SELECT ewl FROM EmailWebhookLog ewl WHERE ewl.errorMessage IS NOT NULL AND ewl.errorMessage LIKE %:errorPattern%")
    List<EmailWebhookLog> findLogsByErrorPattern(@Param("errorPattern") String errorPattern);

    // Nettoyage et maintenance
    @Query("SELECT ewl FROM EmailWebhookLog ewl WHERE ewl.webhookReceivedAt < :cutoffDate AND ewl.status IN ('SUCCESS', 'IGNORED')")
    List<EmailWebhookLog> findOldSuccessfulLogs(@Param("cutoffDate") LocalDateTime cutoffDate);

    @Query("SELECT ewl FROM EmailWebhookLog ewl WHERE ewl.webhookReceivedAt < :cutoffDate AND ewl.status = 'FAILED'")
    List<EmailWebhookLog> findOldFailedLogs(@Param("cutoffDate") LocalDateTime cutoffDate);

    // Monitoring temps réel
    @Query("SELECT COUNT(ewl) FROM EmailWebhookLog ewl WHERE ewl.webhookReceivedAt >= :since")
    long countLogsSince(@Param("since") LocalDateTime since);

    @Query("SELECT COUNT(ewl) FROM EmailWebhookLog ewl WHERE ewl.webhookReceivedAt >= :since AND ewl.status = :status")
    long countLogsByStatusSince(@Param("since") LocalDateTime since, @Param("status") EmailWebhookLog.ProcessingStatus status);

    // Détection d'anomalies
    @Query("SELECT ewl.emailAccount, COUNT(ewl) FROM EmailWebhookLog ewl " +
            "WHERE ewl.webhookReceivedAt >= :since AND ewl.status = 'FAILED' " +
            "GROUP BY ewl.emailAccount " +
            "HAVING COUNT(ewl) >= :threshold")
    List<Object[]> findAccountsWithHighFailureRate(@Param("since") LocalDateTime since, @Param("threshold") long threshold);

    @Query("SELECT ewl.senderEmail, COUNT(ewl) FROM EmailWebhookLog ewl " +
            "WHERE ewl.webhookReceivedAt >= :since " +
            "GROUP BY ewl.senderEmail " +
            "ORDER BY COUNT(ewl) DESC")
    List<Object[]> findTopSendersByVolume(@Param("since") LocalDateTime since, Pageable pageable);

    // Analyse des tendances
    @Query("SELECT DATE(ewl.webhookReceivedAt) as date, COUNT(ewl) as count " +
            "FROM EmailWebhookLog ewl " +
            "WHERE ewl.webhookReceivedAt >= :since " +
            "GROUP BY DATE(ewl.webhookReceivedAt) " +
            "ORDER BY date DESC")
    List<Object[]> getDailyLogCounts(@Param("since") LocalDateTime since);

    @Query("SELECT HOUR(ewl.webhookReceivedAt) as hour, COUNT(ewl) as count " +
            "FROM EmailWebhookLog ewl " +
            "WHERE ewl.webhookReceivedAt >= :since " +
            "GROUP BY HOUR(ewl.webhookReceivedAt) " +
            "ORDER BY hour")
    List<Object[]> getHourlyLogCounts(@Param("since") LocalDateTime since);

    // Support pour les rapports
    @Query("SELECT ewl FROM EmailWebhookLog ewl WHERE " +
            "ewl.client IS NOT NULL AND ewl.dossier IS NOT NULL AND " +
            "ewl.attachmentsProcessed > 0 AND " +
            "ewl.webhookReceivedAt BETWEEN :startDate AND :endDate")
    List<EmailWebhookLog> findSuccessfulProcessingInPeriod(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

    // Validation et intégrité des données
    @Query("SELECT COUNT(ewl) FROM EmailWebhookLog ewl WHERE ewl.client IS NULL AND ewl.senderEmail IS NOT NULL")
    long countLogsWithUnidentifiedClients();

    @Query("SELECT COUNT(ewl) FROM EmailWebhookLog ewl WHERE ewl.dossier IS NULL AND ewl.client IS NOT NULL")
    long countLogsWithoutAssociatedDossier();
}
