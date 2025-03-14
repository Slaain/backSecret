package myavocat.legit.repository;

import myavocat.legit.model.Facture;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public interface FactureRepository extends JpaRepository<Facture, UUID> {

    /**
     * Récupérer la dernière facture d'un client en fonction du modèle "AI001"
     */
    @Query("SELECT f.numeroFacture FROM Facture f WHERE f.numeroFacture LIKE :pattern ORDER BY f.numeroFacture DESC LIMIT 1")
    String findLastFactureByClient(@Param("pattern") String pattern);


    List<Facture> findByDossierId(UUID dossierId);

    /**
     * Calculer le montant total des factures émises pour un office spécifique
     */
    @Query("SELECT COALESCE(SUM(f.montantTtc), 0) FROM Facture f " +
            "WHERE f.dossier.office.id = :officeId")
    BigDecimal getTotalMontantEmis(@Param("officeId") UUID officeId);

    /**
     * Calculer le montant total des paiements reçus (factures réglées) pour un office spécifique
     */
    @Query("SELECT COALESCE(SUM(f.montantTtc), 0) FROM Facture f " +
            "WHERE f.dossier.office.id = :officeId AND f.statutPaiement = 'REGLEE'")
    BigDecimal getTotalMontantRegle(@Param("officeId") UUID officeId);

    /**
     * Trouver les factures impayées en retard pour un office spécifique
     */
    @Query("SELECT f FROM Facture f " +
            "WHERE f.dossier.office.id = :officeId " +
            "AND f.statutPaiement = 'ATTENTE_REGLEMENT' " +
            "AND f.dateEcheance < CURRENT_TIMESTAMP")
    List<Facture> findFacturesEnRetard(@Param("officeId") UUID officeId);

    /**
     * Récupérer toutes les factures pour un office spécifique
     */
    @Query("SELECT f FROM Facture f WHERE f.dossier.office.id = :officeId")
    List<Facture> findAllByOffice(@Param("officeId") UUID officeId);


    /**
     * Récupérer une facture par son ID uniquement si elle appartient au même office
     */
    @Query("SELECT f FROM Facture f WHERE f.id = :id AND f.dossier.office.id = :officeId")
    Facture findByIdAndOffice(@Param("id") UUID id, @Param("officeId") UUID officeId);


}
