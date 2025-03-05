package myavocat.legit.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AuthResponseDTO {
    private String token;
    private String email;
    private String role;
    private String userId;
    private String officeId;

    // ✅ Ajoute un constructeur pour les erreurs
    public AuthResponseDTO(String message) {
        this.token = null;
        this.email = null;
        this.role = message;
        this.userId = null;
        this.officeId = null;
    }
}
