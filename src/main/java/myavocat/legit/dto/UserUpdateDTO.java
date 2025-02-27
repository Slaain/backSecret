package myavocat.legit.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import myavocat.legit.model.UserRole;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserUpdateDTO {
    private String nom;
    private String prenom;
    private String password;
    private UserRole role;
}