package myavocat.legit.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "paiements")
public class Paiement {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "facture_id", nullable = false)
    private Facture facture; // Relation Many-to-One vers Facture

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal montant; // Montant du paiement

    @Column(nullable = false)
    private LocalDate datePaiement; // Date du paiement réel par le client

    @Column(length = 50)
    @Enumerated(EnumType.STRING)
    private myavocat.legit.model.ModePaiement modePaiement; // VIREMENT, CHEQUE, ESPECES, CB, etc.

    @Column(length = 100)
    private String reference; // Numéro de chèque, référence virement, etc.

    @Column(columnDefinition = "TEXT")
    private String notes; // Commentaires optionnels sur le paiement

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();

    // 🔥 CONSTRUCTEURS
    public Paiement() {}

    public Paiement(Facture facture, BigDecimal montant, LocalDate datePaiement) {
        this.facture = facture;
        this.montant = montant;
        this.datePaiement = datePaiement;
    }

    public Paiement(Facture facture, BigDecimal montant, LocalDate datePaiement, myavocat.legit.model.ModePaiement modePaiement) {
        this.facture = facture;
        this.montant = montant;
        this.datePaiement = datePaiement;
        this.modePaiement = modePaiement;
    }

    // 🔥 LIFECYCLE CALLBACKS
    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    // 🔥 MÉTHODES UTILES

    /**
     * Vérifie si le paiement est valide (montant positif)
     */
    public boolean isValid() {
        return montant != null && montant.compareTo(BigDecimal.ZERO) > 0;
    }

    /**
     * Retourne une description lisible du paiement
     */
    public String getDescription() {
        StringBuilder desc = new StringBuilder();
        desc.append(montant).append("€");

        if (modePaiement != null) {
            desc.append(" (").append(modePaiement.name()).append(")");
        }

        if (reference != null && !reference.trim().isEmpty()) {
            desc.append(" - Réf: ").append(reference);
        }

        return desc.toString();
    }

    @Override
    public String toString() {
        return "Paiement{" +
                "id=" + id +
                ", montant=" + montant +
                ", datePaiement=" + datePaiement +
                ", modePaiement=" + modePaiement +
                ", reference='" + reference + '\'' +
                '}';
    }
}
