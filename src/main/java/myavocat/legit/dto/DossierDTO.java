package myavocat.legit.dto;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class DossierDTO {
    // Informations de base du dossier
    private UUID id;
    private String reference;
    private String nomDossier;
    private String typeAffaire;
    private String statut;
    private String qualiteProcedurale;
    private String contentieux;
    private LocalDateTime createdAt;

    // Personnes associées (représentées avec des versions simplifiées)
    private PersonneSimpleDTO client;
    private PersonneSimpleDTO adversaire;
    private PersonneSimpleDTO avocat;

    // IDs pour les assignations
    private UUID clientId;
    private UUID adversaireId;
    private UUID avocatId;
    private UUID officeId;

    // DTO interne pour représenter uniformément les personnes associées
    @Data
    public static class PersonneSimpleDTO {
        private UUID id;
        private String nom;
        private String prenom;
        private String email;
    }
}