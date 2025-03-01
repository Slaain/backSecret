package myavocat.legit.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class OfficeAuthRequestDTO {
    @NotBlank(message = "Le nom du cabinet est requis")
    private String officeName;

    @NotBlank(message = "Le mot de passe du cabinet est requis")
    private String officePassword;
}