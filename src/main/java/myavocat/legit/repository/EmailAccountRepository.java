package myavocat.legit.repository;

import myavocat.legit.model.EmailAccount;
import myavocat.legit.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface EmailAccountRepository extends JpaRepository<EmailAccount, UUID> {

    // Recherche par utilisateur
    List<EmailAccount> findByUser(User user);
    Optional<EmailAccount> findByUserId(UUID userId);
    Optional<EmailAccount> findByUserIdAndIsActive(UUID userId, boolean isActive);

    // Recherche par email
    Optional<EmailAccount> findByEmailAddress(String emailAddress);
    Optional<EmailAccount> findByEmailAddressAndIsActive(String emailAddress, boolean isActive);

    // Recherche par fournisseur
    List<EmailAccount> findByProvider(EmailAccount.EmailProvider provider);
    List<EmailAccount> findByProviderAndIsActive(EmailAccount.EmailProvider provider, boolean isActive);

    // Comptes actifs
    List<EmailAccount> findByIsActive(boolean isActive);

    @Query("SELECT ea FROM EmailAccount ea WHERE ea.isActive = true AND ea.user.office.id = :officeId")
    List<EmailAccount> findActiveAccountsByOffice(@Param("officeId") UUID officeId);

    // Gestion des tokens OAuth
    @Query("SELECT ea FROM EmailAccount ea WHERE ea.tokenExpiresAt < :expiryTime AND ea.isActive = true")
    List<EmailAccount> findAccountsWithExpiringTokens(@Param("expiryTime") LocalDateTime expiryTime);

    @Query("SELECT ea FROM EmailAccount ea WHERE ea.encryptedAccessToken IS NOT NULL AND ea.tokenExpiresAt > :now")
    List<EmailAccount> findAccountsWithValidTokens(@Param("now") LocalDateTime now);

    // Gestion des webhooks
    @Query("SELECT ea FROM EmailAccount ea WHERE ea.webhookExpiry < :expiryTime AND ea.isActive = true")
    List<EmailAccount> findAccountsWithExpiringWebhooks(@Param("expiryTime") LocalDateTime expiryTime);

    @Query("SELECT ea FROM EmailAccount ea WHERE ea.webhookSubscriptionId IS NOT NULL AND ea.isActive = true")
    List<EmailAccount> findAccountsWithActiveWebhooks();

    // Comptes avec erreurs
    @Query("SELECT ea FROM EmailAccount ea WHERE ea.syncErrorsCount >= :threshold")
    List<EmailAccount> findAccountsWithErrors(@Param("threshold") int threshold);

    @Query("SELECT ea FROM EmailAccount ea WHERE ea.syncErrorsCount >= 5 AND ea.isActive = true")
    List<EmailAccount> findAccountsToDisable();

    // Synchronisation
    @Query("SELECT ea FROM EmailAccount ea WHERE ea.lastSyncAt < :syncTime OR ea.lastSyncAt IS NULL")
    List<EmailAccount> findAccountsNeedingSync(@Param("syncTime") LocalDateTime syncTime);

    @Query("SELECT ea FROM EmailAccount ea WHERE ea.isActive = true AND " +
            "((ea.provider = 'OTHER' AND ea.imapHost IS NOT NULL) OR " +
            "(ea.provider != 'OTHER' AND ea.encryptedAccessToken IS NOT NULL))")
    List<EmailAccount> findReadyForSync();

    // Statistiques et monitoring
    @Query("SELECT COUNT(ea) FROM EmailAccount ea WHERE ea.isActive = true")
    long countActiveAccounts();

    @Query("SELECT COUNT(ea) FROM EmailAccount ea WHERE ea.isActive = true AND ea.provider = :provider")
    long countActiveAccountsByProvider(@Param("provider") EmailAccount.EmailProvider provider);

    @Query("SELECT ea.provider, COUNT(ea) FROM EmailAccount ea WHERE ea.isActive = true GROUP BY ea.provider")
    List<Object[]> countAccountsByProvider();

    @Query("SELECT AVG(ea.syncErrorsCount) FROM EmailAccount ea WHERE ea.isActive = true")
    Double getAverageErrorCount();

    // Nettoyage et maintenance
    @Query("SELECT ea FROM EmailAccount ea WHERE ea.createdAt < :cutoffDate AND ea.isActive = false")
    List<EmailAccount> findInactiveAccountsOlderThan(@Param("cutoffDate") LocalDateTime cutoffDate);

    @Query("SELECT ea FROM EmailAccount ea WHERE ea.lastSyncAt < :cutoffDate AND ea.isActive = false")
    List<EmailAccount> findStaleAccounts(@Param("cutoffDate") LocalDateTime cutoffDate);

    // Requêtes complexes pour l'administration
    @Query("SELECT ea FROM EmailAccount ea WHERE " +
            "ea.user.office.id = :officeId AND " +
            "ea.isActive = true AND " +
            "ea.syncErrorsCount < 3 AND " +
            "(ea.lastSyncAt IS NULL OR ea.lastSyncAt > :recentTime)")
    List<EmailAccount> findHealthyAccountsByOffice(
            @Param("officeId") UUID officeId,
            @Param("recentTime") LocalDateTime recentTime);

    @Query("SELECT ea FROM EmailAccount ea WHERE " +
            "ea.isActive = true AND " +
            "ea.provider IN ('GMAIL', 'OUTLOOK') AND " +
            "ea.encryptedAccessToken IS NOT NULL AND " +
            "ea.tokenExpiresAt > :minExpiryTime")
    List<EmailAccount> findOAuthAccountsReadyForWebhooks(@Param("minExpiryTime") LocalDateTime minExpiryTime);

    // Recherche avancée pour debugging
    @Query("SELECT ea FROM EmailAccount ea WHERE " +
            "ea.user.email LIKE %:searchTerm% OR " +
            "ea.emailAddress LIKE %:searchTerm% OR " +
            "ea.lastErrorMessage LIKE %:searchTerm%")
    List<EmailAccount> searchAccounts(@Param("searchTerm") String searchTerm);

    // Vérification d'unicité et validation
    boolean existsByUserAndEmailAddress(User user, String emailAddress);
    boolean existsByEmailAddressAndIsActive(String emailAddress, boolean isActive);

    @Query("SELECT COUNT(ea) FROM EmailAccount ea WHERE ea.user = :user AND ea.isActive = true")
    long countActiveAccountsByUser(@Param("user") User user);

    // Méthodes pour la configuration automatique
    @Query("SELECT DISTINCT ea.imapHost FROM EmailAccount ea WHERE " +
            "ea.provider = 'OTHER' AND ea.imapHost IS NOT NULL AND ea.syncErrorsCount < 3")
    List<String> findWorkingImapHosts();

    // Support pour les tâches de maintenance planifiées
    @Query("SELECT ea FROM EmailAccount ea WHERE " +
            "ea.isActive = true AND " +
            "MOD(EXTRACT(HOUR FROM ea.createdAt), :intervalHours) = MOD(EXTRACT(HOUR FROM :currentTime), :intervalHours)")
    List<EmailAccount> findAccountsForMaintenanceSlot(
            @Param("intervalHours") int intervalHours,
            @Param("currentTime") LocalDateTime currentTime);
}
