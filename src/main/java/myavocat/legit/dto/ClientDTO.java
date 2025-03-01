package myavocat.legit.dto;

import lombok.Data;
import java.util.UUID;

@Data
public class ClientDTO {
    private UUID id;
    private String nom;
    private String prenom;
    private String email;
    private String telephone;
    private String type;      // Ajout du type
    private String qualite;   // Ajout de la qualité
    private String commune;   // Ajout de la commune
    private UUID officeId;    // On stocke seulement l'ID du cabinet pour éviter d'exposer tout l'objet `Office`
}
