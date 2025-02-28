package myavocat.legit.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public class OfficeWithUsersDTO {
    // Information du cabinet
    private UUID officeId;
    private String officeName;
    private String officeAddress;
    private String officePhone;
    private String officeEmail;
    private String officeSiret;
    private boolean officeActive;
    private LocalDateTime officeCreatedAt;

    // Liste des utilisateurs simplifiés
    private List<UserInOfficeDTO> users;

    // Classe interne pour représenter un utilisateur dans un cabinet
    public static class UserInOfficeDTO {
        private UUID id;
        private String nom;
        private String prenom;
        private String email;
        private String roleName;
        private LocalDateTime createdAt;

        // Constructeurs
        public UserInOfficeDTO() {}

        public UserInOfficeDTO(UUID id, String nom, String prenom, String email,
                               String roleName, LocalDateTime createdAt) {
            this.id = id;
            this.nom = nom;
            this.prenom = prenom;
            this.email = email;
            this.roleName = roleName;
            this.createdAt = createdAt;
        }

        // Getters et Setters
        public UUID getId() { return id; }
        public void setId(UUID id) { this.id = id; }

        public String getNom() { return nom; }
        public void setNom(String nom) { this.nom = nom; }

        public String getPrenom() { return prenom; }
        public void setPrenom(String prenom) { this.prenom = prenom; }

        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }

        public String getRoleName() { return roleName; }
        public void setRoleName(String roleName) { this.roleName = roleName; }

        public LocalDateTime getCreatedAt() { return createdAt; }
        public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    }

    // Constructeurs
    public OfficeWithUsersDTO() {}

    public OfficeWithUsersDTO(UUID officeId, String officeName, String officeAddress,
                              String officePhone, String officeEmail, String officeSiret,
                              boolean officeActive, LocalDateTime officeCreatedAt,
                              List<UserInOfficeDTO> users) {
        this.officeId = officeId;
        this.officeName = officeName;
        this.officeAddress = officeAddress;
        this.officePhone = officePhone;
        this.officeEmail = officeEmail;
        this.officeSiret = officeSiret;
        this.officeActive = officeActive;
        this.officeCreatedAt = officeCreatedAt;
        this.users = users;
    }

    // Getters et Setters
    public UUID getOfficeId() { return officeId; }
    public void setOfficeId(UUID officeId) { this.officeId = officeId; }

    public String getOfficeName() { return officeName; }
    public void setOfficeName(String officeName) { this.officeName = officeName; }

    public String getOfficeAddress() { return officeAddress; }
    public void setOfficeAddress(String officeAddress) { this.officeAddress = officeAddress; }

    public String getOfficePhone() { return officePhone; }
    public void setOfficePhone(String officePhone) { this.officePhone = officePhone; }

    public String getOfficeEmail() { return officeEmail; }
    public void setOfficeEmail(String officeEmail) { this.officeEmail = officeEmail; }

    public String getOfficeSiret() { return officeSiret; }
    public void setOfficeSiret(String officeSiret) { this.officeSiret = officeSiret; }

    public boolean isOfficeActive() { return officeActive; }
    public void setOfficeActive(boolean officeActive) { this.officeActive = officeActive; }

    public LocalDateTime getOfficeCreatedAt() { return officeCreatedAt; }
    public void setOfficeCreatedAt(LocalDateTime officeCreatedAt) { this.officeCreatedAt = officeCreatedAt; }

    public List<UserInOfficeDTO> getUsers() { return users; }
    public void setUsers(List<UserInOfficeDTO> users) { this.users = users; }
}