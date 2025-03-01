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
    private UUID dossierId; // L'adversaire appartient à un seul dossier
}
