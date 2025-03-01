package myavocat.legit.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class OfficeDTO {
    @NotBlank(message = "Le nom est obligatoire")
    @Size(max = 100, message = "Le nom ne peut pas dépasser 100 caractères")
    private String name;

    @Size(max = 255, message = "L'adresse ne peut pas dépasser 255 caractères")
    private String address;

    @Size(max = 20, message = "Le numéro de téléphone ne peut pas dépasser 20 caractères")
    private String phone;

    @Pattern(
            regexp = "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,6}$",
            message = "Format d'email invalide."
    )
    private String email;

    @Size(max = 20, message = "Le SIRET ne peut pas dépasser 20 caractères")
    private String siret;

    // Nouveau champ pour le mot de passe du cabinet
    @NotBlank(message = "Le mot de passe du cabinet est obligatoire")
    @Size(min = 6, message = "Le mot de passe doit contenir au moins 6 caractères")
    private String password;

    private boolean actif = true;

    public OfficeDTO() {}

    public OfficeDTO(String name, String address, String phone, String email, String siret, String password, boolean actif) {
        this.name = name;
        this.address = address;
        this.phone = phone;
        this.email = email;
        this.siret = siret;
        this.password = password;
        this.actif = actif;
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getSiret() { return siret; }
    public void setSiret(String siret) { this.siret = siret; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public boolean isActif() { return actif; }
    public void setActif(boolean actif) { this.actif = actif; }
}