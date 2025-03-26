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
    private DossierSimpleDTO dossier;
    private UserSimpleDTO user;
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
