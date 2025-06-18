package myavocat.legit.dto;

import lombok.Getter;
import lombok.Setter;
import myavocat.legit.model.ModePaiement;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Getter
@Setter
public class CreatePaiementDTO {

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

    // 🔥 CONSTRUCTEURS
    public CreatePaiementDTO() {}

    public CreatePaiementDTO(UUID factureId, BigDecimal montant, LocalDate datePaiement) {
        this.factureId = factureId;
        this.montant = montant;
        this.datePaiement = datePaiement;
    }

    public CreatePaiementDTO(UUID factureId, BigDecimal montant, LocalDate datePaiement, ModePaiement modePaiement) {
        this.factureId = factureId;
        this.montant = montant;
        this.datePaiement = datePaiement;
        this.modePaiement = modePaiement;
    }

    // 🔥 MÉTHODES UTILES

    /**
     * Vérifie si toutes les données obligatoires sont présentes
     */
    public boolean isValid() {
        return factureId != null &&
                montant != null && montant.compareTo(BigDecimal.ZERO) > 0 &&
                datePaiement != null;
    }

    /**
     * Retourne une description du paiement à créer
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
        return "CreatePaiementDTO{" +
                "factureId=" + factureId +
                ", montant=" + montant +
                ", datePaiement=" + datePaiement +
                ", modePaiement=" + modePaiement +
                ", reference='" + reference + '\'' +
                '}';
    }
}
