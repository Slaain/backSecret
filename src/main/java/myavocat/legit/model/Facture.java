package myavocat.legit.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
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
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public void setMontantHt(BigDecimal montantHt) {
        this.montantHt = montantHt;
        this.montantTtc = tvaApplicable ? montantHt.multiply(BigDecimal.valueOf(1.2)) : montantHt;
    }
}
