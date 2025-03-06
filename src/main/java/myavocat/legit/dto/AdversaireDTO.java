package myavocat.legit.dto;

import lombok.Data;
import java.util.UUID;

@Data
public class AdversaireDTO {
    private UUID id;
    private String nom;
    private String prenom;
    private String email;
    private String telephone;
    private String type;
    private String qualite;
    private String commune;
    private UUID officeId;
    private UUID dossierId; // Pour la compatibilité avec le code existant
}