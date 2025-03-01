package myavocat.legit.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class OfficeAuthResponseDTO {
    private boolean success;
    private String message;
    private UUID officeId;
    private String officeName;
    // On peut ajouter d'autres informations du cabinet si nécessaire
    private String tempToken;  // Token temporaire pour la seconde étape d'authentification
}