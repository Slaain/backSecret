package myavocat.legit.dto;

import lombok.Getter;
import lombok.Setter;
import myavocat.legit.model.ModePaiement;
import myavocat.legit.model.StatutPaiement;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
public class FactureDTO {
    private UUID id;
    private String numeroFacture;
    private String intitule;
    private LocalDateTime dateEmission;
    private BigDecimal montantHt;
    private BigDecimal montantTtc;

    // 🔥 NOUVEAUX CHAMPS POUR LES PAIEMENTS
    private BigDecimal montantReclame;      // Montant réclamé
    private BigDecimal montantRegleTtc;     // Montant réglé TTC (calculé)
    private BigDecimal montantRestantDu;    // Montant restant dû ou solde à percevoir (calculé)

    private StatutPaiement statutPaiement;
    private ModePaiement modePaiement;

    // Informations sur le dossier
    private String dossierReference;
    private String dossierNom;
    private String dossierStatut;

    // Informations sur le client
    private String clientNom;
    private String clientPrenom;

    // 🔥 CONSTRUCTEUR MIS À JOUR avec les nouveaux champs
    public FactureDTO(UUID id, String numeroFacture, String intitule, LocalDateTime dateEmission,
                      BigDecimal montantHt, BigDecimal montantTtc, BigDecimal montantReclame,
                      BigDecimal montantRegleTtc, BigDecimal montantRestantDu,
                      StatutPaiement statutPaiement, ModePaiement modePaiement,
                      String dossierReference, String dossierNom, String dossierStatut,
                      String clientNom, String clientPrenom) {
        this.id = id;
        this.numeroFacture = numeroFacture;
        this.intitule = intitule;
        this.dateEmission = dateEmission;
        this.montantHt = montantHt;
        this.montantTtc = montantTtc;
        this.montantReclame = montantReclame;
        this.montantRegleTtc = montantRegleTtc;
        this.montantRestantDu = montantRestantDu;
        this.statutPaiement = statutPaiement;
        this.modePaiement = modePaiement;
        this.dossierReference = dossierReference;
        this.dossierNom = dossierNom;
        this.dossierStatut = dossierStatut;
        this.clientNom = clientNom;
        this.clientPrenom = clientPrenom;
    }

    // 🔥 CONSTRUCTEUR DE COMPATIBILITÉ (pour éviter les erreurs dans l'ancien code)
    public FactureDTO(UUID id, String numeroFacture, String intitule, LocalDateTime dateEmission,
                      BigDecimal montantHt, BigDecimal montantTtc, StatutPaiement statutPaiement,
                      ModePaiement modePaiement, String dossierReference, String dossierNom,
                      String dossierStatut, String clientNom, String clientPrenom) {
        this(id, numeroFacture, intitule, dateEmission, montantHt, montantTtc,
                montantTtc, // montantReclame = montantTtc par défaut
                BigDecimal.ZERO, // montantRegleTtc = 0 par défaut
                montantTtc, // montantRestantDu = montantTtc par défaut
                statutPaiement, modePaiement, dossierReference, dossierNom,
                dossierStatut, clientNom, clientPrenom);
    }

    // 🔥 MÉTHODES UTILES POUR L'AFFICHAGE

    /**
     * Vérifie si la facture est entièrement payée
     */
    public boolean isPayee() {
        return montantRestantDu != null && montantRestantDu.compareTo(BigDecimal.ZERO) <= 0;
    }

    /**
     * Vérifie si la facture est partiellement payée
     */
    public boolean isPartiellementPayee() {
        return montantRegleTtc != null && montantRegleTtc.compareTo(BigDecimal.ZERO) > 0
                && montantRestantDu != null && montantRestantDu.compareTo(BigDecimal.ZERO) > 0;
    }

    /**
     * Calcule le pourcentage de paiement
     */
    public double getPourcentagePaiement() {
        if (montantReclame == null || montantReclame.compareTo(BigDecimal.ZERO) == 0) {
            return 0.0;
        }
        if (montantRegleTtc == null) {
            return 0.0;
        }
        return montantRegleTtc.divide(montantReclame, 4, java.math.RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .doubleValue();
    }

    /**
     * Retourne un statut lisible pour l'affichage
     */
    public String getStatutAffichage() {
        if (isPayee()) {
            return "Payée ✅";
        } else if (isPartiellementPayee()) {
            return "Paiement partiel ⚠️";
        } else {
            return "En attente 🕐";
        }
    }
}
