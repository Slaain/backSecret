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
    private StatutPaiement statutPaiement;
    private ModePaiement modePaiement;

    // Informations sur le dossier
    private String dossierReference;
    private String dossierNom;
    private String dossierStatut;

    // Informations sur le client
    private String clientNom;
    private String clientPrenom;

    public FactureDTO(UUID id, String numeroFacture, String intitule, LocalDateTime dateEmission,
                      BigDecimal montantHt, BigDecimal montantTtc, StatutPaiement statutPaiement,
                      ModePaiement modePaiement, String dossierReference, String dossierNom,
                      String dossierStatut, String clientNom, String clientPrenom) {
        this.id = id;
        this.numeroFacture = numeroFacture;
        this.intitule = intitule;
        this.dateEmission = dateEmission;
        this.montantHt = montantHt;
        this.montantTtc = montantTtc;
        this.statutPaiement = statutPaiement;
        this.modePaiement = modePaiement;
        this.dossierReference = dossierReference;
        this.dossierNom = dossierNom;
        this.dossierStatut = dossierStatut;
        this.clientNom = clientNom;
        this.clientPrenom = clientPrenom;
    }
}
