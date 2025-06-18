package myavocat.legit.repository;

import myavocat.legit.model.Paiement;
import myavocat.legit.model.ModePaiement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public interface PaiementRepository extends JpaRepository<Paiement, UUID> {

    // 🔥 MÉTHODES DE BASE POUR UNE FACTURE

    /**
     * Récupérer tous les paiements d'une facture spécifique
     */
    List<Paiement> findByFactureIdOrderByDatePaiementDesc(UUID factureId);

    /**
     * Récupérer tous les paiements d'une facture triés par date croissante
     */
    List<Paiement> findByFactureIdOrderByDatePaiementAsc(UUID factureId);

    /**
     * Calculer le montant total payé pour une facture
     */
    @Query("SELECT COALESCE(SUM(p.montant), 0) FROM Paiement p WHERE p.facture.id = :factureId")
    BigDecimal sumMontantByFactureId(@Param("factureId") UUID factureId);

    /**
     * Compter le nombre de paiements pour une facture
     */
    long countByFactureId(UUID factureId);

    // 🔥 RECHERCHES PAR DATE

    /**
     * Paiements entre deux dates
     */
    List<Paiement> findByDatePaiementBetween(LocalDate dateDebut, LocalDate dateFin);

    /**
     * Paiements d'une facture entre deux dates
     */
    List<Paiement> findByFactureIdAndDatePaiementBetween(UUID factureId, LocalDate dateDebut, LocalDate dateFin);

    /**
     * Paiements effectués aujourd'hui
     */
    @Query("SELECT p FROM Paiement p WHERE p.datePaiement = CURRENT_DATE")
    List<Paiement> findPaiementsAujourdhui();

    /**
     * Paiements de la semaine en cours
     */
    @Query("SELECT p FROM Paiement p WHERE p.datePaiement >= :debutSemaine AND p.datePaiement <= :finSemaine")
    List<Paiement> findPaiementsSemaine(@Param("debutSemaine") LocalDate debutSemaine, @Param("finSemaine") LocalDate finSemaine);

    // 🔥 RECHERCHES PAR MODE DE PAIEMENT

    /**
     * Paiements par mode de paiement
     */
    List<Paiement> findByModePaiement(ModePaiement modePaiement);

    /**
     * Paiements d'une facture par mode de paiement
     */
    List<Paiement> findByFactureIdAndModePaiement(UUID factureId, ModePaiement modePaiement);

    // 🔥 RECHERCHES PAR OFFICE (SÉCURITÉ)

    /**
     * Tous les paiements d'un office (sécurité)
     */
    @Query("SELECT p FROM Paiement p WHERE p.facture.dossier.office.id = :officeId ORDER BY p.datePaiement DESC")
    List<Paiement> findAllByOfficeId(@Param("officeId") UUID officeId);

    /**
     * Paiements d'un office entre deux dates
     */
    @Query("SELECT p FROM Paiement p WHERE p.facture.dossier.office.id = :officeId " +
            "AND p.datePaiement BETWEEN :dateDebut AND :dateFin ORDER BY p.datePaiement DESC")
    List<Paiement> findByOfficeIdAndDateBetween(@Param("officeId") UUID officeId,
                                                @Param("dateDebut") LocalDate dateDebut,
                                                @Param("dateFin") LocalDate dateFin);

    // 🔥 STATISTIQUES ET KPI

    /**
     * Montant total des paiements pour un office
     */
    @Query("SELECT COALESCE(SUM(p.montant), 0) FROM Paiement p WHERE p.facture.dossier.office.id = :officeId")
    BigDecimal sumMontantByOfficeId(@Param("officeId") UUID officeId);

    /**
     * Montant total des paiements d'un office entre deux dates
     */
    @Query("SELECT COALESCE(SUM(p.montant), 0) FROM Paiement p " +
            "WHERE p.facture.dossier.office.id = :officeId " +
            "AND p.datePaiement BETWEEN :dateDebut AND :dateFin")
    BigDecimal sumMontantByOfficeIdAndDateBetween(@Param("officeId") UUID officeId,
                                                  @Param("dateDebut") LocalDate dateDebut,
                                                  @Param("dateFin") LocalDate dateFin);

    /**
     * Nombre de paiements par mode pour un office
     */
    @Query("SELECT p.modePaiement, COUNT(p) FROM Paiement p " +
            "WHERE p.facture.dossier.office.id = :officeId " +
            "GROUP BY p.modePaiement")
    List<Object[]> countPaiementsByModePaiementAndOfficeId(@Param("officeId") UUID officeId);

    // 🔥 RECHERCHES AVANCÉES

    /**
     * Dernier paiement d'une facture
     */
    @Query("SELECT p FROM Paiement p WHERE p.facture.id = :factureId ORDER BY p.datePaiement DESC LIMIT 1")
    Paiement findLastPaiementByFactureId(@Param("factureId") UUID factureId);

    /**
     * Paiements avec référence (chèque, virement, etc.)
     */
    List<Paiement> findByReferenceContainingIgnoreCase(String reference);

    /**
     * Paiements supérieurs à un montant
     */
    List<Paiement> findByMontantGreaterThan(BigDecimal montant);

    /**
     * Paiements d'un client spécifique (via facture)
     */
    @Query("SELECT p FROM Paiement p WHERE p.facture.client.id = :clientId ORDER BY p.datePaiement DESC")
    List<Paiement> findByClientId(@Param("clientId") UUID clientId);

    // 🔥 VÉRIFICATIONS D'EXISTENCE

    /**
     * Vérifier si une facture a des paiements
     */
    boolean existsByFactureId(UUID factureId);

    /**
     * Vérifier si un paiement avec cette référence existe
     */
    boolean existsByReference(String reference);
}
