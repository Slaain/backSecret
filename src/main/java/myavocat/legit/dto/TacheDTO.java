package myavocat.legit.dto;

import lombok.Data;

import java.time.LocalDate;

@Data
public class TacheDTO {
    private Long id;
    private String titre;
    private String description;
    private String statut;
    private LocalDate dateEcheance;

    private String dossierId; // ID du dossier à associer/modifier
    private String userId;    // ID de l'avocat à associer/modifier

    private DossierSimpleDTO dossier; // utilisé uniquement en lecture
    private UserSimpleDTO user;       // utilisé uniquement en lecture

    private LocalDate createdAt;
    private LocalDate updatedAt;

    @Data
    public static class DossierSimpleDTO {
        private String id;
        private String reference;
        private String nomDossier;
    }

    @Data
    public static class UserSimpleDTO {
        private String id;
        private String nom;
        private String prenom;
        private String email;
    }
}