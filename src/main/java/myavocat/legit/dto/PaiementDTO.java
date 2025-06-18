package myavocat.legit.dto;

import lombok.Getter;
import lombok.Setter;
import myavocat.legit.model.ModePaiement;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
public class PaiementDTO {

    private UUID id;

    @NotNull(message = "L'ID de la facture est obligatoire")
    private UUID factureId;

    @NotNull(message = "Le montant est obligatoire")
    @Positive(message = "Le montant doit être positif")
    private BigDecimal montant;

    @NotNull(message = "La date de paiement est obligatoire")
    private LocalDate datePaiement;

    private ModePaiement modePaiement;

    @Size(max = 100, message = "La référence ne peut pas dépasser 100 caractères")
    private String reference;

    @Size(max = 500, message = "Les notes ne peuvent pas dépasser 500 caractères")
    private String notes;

    // 🔥 INFORMATIONS DE LA FACTURE ASSOCIÉE (lecture seule)
    private String factureNumero;
    private String factureIntitule;
    private String clientNom;
    private String clientPrenom;

    // 🔥 MÉTADONNÉES
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // 🔥 CONSTRUCTEUR COMPLET (pour les réponses API complètes)
    public PaiementDTO(UUID id, UUID factureId, BigDecimal montant, LocalDate datePaiement,
                       ModePaiement modePaiement, String reference, String notes,
                       String factureNumero, String factureIntitule, String clientNom, String clientPrenom,
                       LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.factureId = factureId;
        this.montant = montant;
        this.datePaiement = datePaiement;
        this.modePaiement = modePaiement;
        this.reference = reference;
        this.notes = notes;
        this.factureNumero = factureNumero;
        this.factureIntitule = factureIntitule;
        this.clientNom = clientNom;
        this.clientPrenom = clientPrenom;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    // 🔥 CONSTRUCTEUR SIMPLE (pour la création de paiements)
    public PaiementDTO(UUID factureId, BigDecimal montant, LocalDate datePaiement, ModePaiement modePaiement) {
        this.factureId = factureId;
        this.montant = montant;
        this.datePaiement = datePaiement;
        this.modePaiement = modePaiement;
    }

    // 🔥 CONSTRUCTEUR VIDE (pour la désérialisation JSON)
    public PaiementDTO() {}

    // 🔥 MÉTHODES UTILES POUR L'AFFICHAGE

    /**
     * Retourne une description lisible du paiement
     */
    public String getDescription() {
        StringBuilder desc = new StringBuilder();
        desc.append(montant).append("€");

        if (modePaiement != null) {
            desc.append(" (").append(getModePaiementAffichage()).append(")");
        }

        if (reference != null && !reference.trim().isEmpty()) {
            desc.append(" - Réf: ").append(reference);
        }

        return desc.toString();
    }

    /**
     * Mode de paiement avec affichage français
     */
    public String getModePaiementAffichage() {
        if (modePaiement == null) return "Non spécifié";

        return switch (modePaiement) {
            case VIREMENT -> "Virement";
            case CHEQUE -> "Chèque";
            case ESPECES -> "Espèces";
            case CB -> "Carte bancaire";
            case AUTRES -> "Autres";
            default -> modePaiement.name();
        };
    }

    /**
     * Client complet (nom + prénom)
     */
    public String getClientComplet() {
        if (clientNom == null && clientPrenom == null) {
            return "Client non spécifié";
        }
        return (clientPrenom != null ? clientPrenom + " " : "") +
                (clientNom != null ? clientNom : "");
    }

    /**
     * Date formatée pour l'affichage
     */
    public String getDatePaiementFormatee() {
        if (datePaiement == null) return "";
        return datePaiement.format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy"));
    }

    /**
     * Montant formaté avec devise
     */
    public String getMontantFormate() {
        if (montant == null) return "0,00 €";
        return String.format("%.2f €", montant);
    }

    /**
     * Vérifie si le paiement a une référence
     */
    public boolean hasReference() {
        return reference != null && !reference.trim().isEmpty();
    }

    /**
     * Vérifie si le paiement a des notes
     */
    public boolean hasNotes() {
        return notes != null && !notes.trim().isEmpty();
    }

    /**
     * Retourne un résumé court du paiement pour les listes
     */
    public String getResume() {
        return String.format("%s - %s (%s)",
                getMontantFormate(),
                getDatePaiementFormatee(),
                getModePaiementAffichage());
    }

    @Override
    public String toString() {
        return "PaiementDTO{" +
                "id=" + id +
                ", factureId=" + factureId +
                ", montant=" + montant +
                ", datePaiement=" + datePaiement +
                ", modePaiement=" + modePaiement +
                ", reference='" + reference + '\'' +
                '}';
    }
}
