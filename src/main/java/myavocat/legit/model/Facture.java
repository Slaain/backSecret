package myavocat.legit.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "factures")
public class Facture {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(nullable = false, unique = true, length = 50)
    private String numeroFacture; // Ex: AI001, AI002

    @ManyToOne
    @JoinColumn(name = "dossier_id", nullable = false)
    private Dossier dossier; // Une facture appartient à un dossier

    @ManyToOne
    @JoinColumn(name = "client_id", nullable = false)
    private Client client; // Le client lié à cette facture

    @Column(nullable = false)
    private LocalDateTime dateEmission = LocalDateTime.now();

    @Column(nullable = false, length = 255)
    private String intitule; // Intitulé de la facture

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal montantHt; // Montant Hors Taxe

    @Column(nullable = false)
    private Boolean tvaApplicable = true;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal montantTtc; // Calculé automatiquement en backend

    // 🔥 NOUVEAUX CHAMPS POUR LES PAIEMENTS
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal montantReclame; // Montant réclamé (= montantTtc par défaut)

    // 🔥 RELATION AVEC LES PAIEMENTS (One-to-Many)
    @OneToMany(mappedBy = "facture", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Paiement> paiements = new ArrayList<>();

    @Column(nullable = false, length = 50)
    @Enumerated(EnumType.STRING)
    private myavocat.legit.model.StatutPaiement statutPaiement = myavocat.legit.model.StatutPaiement.ATTENTE_REGLEMENT;

    @Column(length = 50)
    @Enumerated(EnumType.STRING)
    private myavocat.legit.model.ModePaiement modePaiement; // CB, Virement, Chèque, Espèces...

    @Column
    private LocalDateTime dateEcheance; // Date limite de paiement

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();

        // 🔥 Par défaut, montantReclame = montantTtc
        if (this.montantReclame == null) {
            this.montantReclame = this.montantTtc;
        }
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public void setMontantHt(BigDecimal montantHt) {
        this.montantHt = montantHt;
        this.montantTtc = tvaApplicable ? montantHt.multiply(BigDecimal.valueOf(1.2)) : montantHt;

        // 🔥 Mise à jour automatique du montant réclamé
        this.montantReclame = this.montantTtc;
    }

    // 🔥 MÉTHODES CALCULÉES POUR LES PAIEMENTS

    /**
     * Calcule le montant total réglé TTC à partir de tous les paiements
     */
    public BigDecimal getMontantRegleTtc() {
        return paiements.stream()
                .map(Paiement::getMontant)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * Calcule le montant restant dû
     */
    public BigDecimal getMontantRestantDu() {
        return montantReclame.subtract(getMontantRegleTtc());
    }

    /**
     * Vérifie si la facture est entièrement payée
     */
    public boolean isPayee() {
        return getMontantRestantDu().compareTo(BigDecimal.ZERO) <= 0;
    }

    /**
     * Vérifie si la facture est partiellement payée
     */
    public boolean isPartiellementPayee() {
        BigDecimal regle = getMontantRegleTtc();
        return regle.compareTo(BigDecimal.ZERO) > 0 &&
                regle.compareTo(montantReclame) < 0;
    }

    /**
     * Met à jour automatiquement le statut de paiement selon les montants
     */
    public void updateStatutPaiement() {
        if (isPayee()) {
            this.statutPaiement = myavocat.legit.model.StatutPaiement.REGLEE;
        } else if (isPartiellementPayee()) {
            this.statutPaiement = myavocat.legit.model.StatutPaiement.PARTIELLEMENT_REGLEE;
        } else {
            this.statutPaiement = myavocat.legit.model.StatutPaiement.ATTENTE_REGLEMENT;
        }
    }
}
