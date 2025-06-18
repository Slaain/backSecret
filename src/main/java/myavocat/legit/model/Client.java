package myavocat.legit.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "clients")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Client {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(nullable = false, length = 100)
    private String nom;

    @Column(nullable = false, length = 100)
    private String prenom;

    @Column(nullable = false, unique = true, length = 255)
    private String email;

    @Column(length = 15)
    private String telephone;

    @Column(nullable = false)
    private String type; // Particulier, Entreprise...

    @Column(nullable = false)
    private String qualite; // Demandeur, Défendeur...

    @Column(length = 255)
    private String commune; // Ville

    @ManyToOne
    @JoinColumn(name = "office_id", nullable = false)
    private Office office; // Un client appartient à un seul cabinet

    // ✅ RELATION BIDIRECTIONNELLE CORRECTE
    // mappedBy indique que c'est Dossier qui gère la relation
    @ManyToMany(mappedBy = "clients", fetch = FetchType.LAZY)
    @JsonIgnoreProperties({"clients", "adversaires", "avocat", "office"})
    private Set<Dossier> dossiers = new HashSet<>();

    public Client() {}

    // ✅ MÉTHODES UTILITAIRES POUR GÉRER LA RELATION BIDIRECTIONNELLE

    /**
     * Ajoute un dossier et synchronise la relation bidirectionnelle
     */
    public void addDossier(Dossier dossier) {
        if (this.dossiers == null) {
            this.dossiers = new HashSet<>();
        }
        this.dossiers.add(dossier);
        dossier.addClient(this);
    }

    /**
     * Retire un dossier et synchronise la relation bidirectionnelle
     */
    public void removeDossier(Dossier dossier) {
        if (this.dossiers != null) {
            this.dossiers.remove(dossier);
            dossier.removeClient(this);
        }
    }

    // ✅ MÉTHODE POUR OBTENIR LE NOMBRE DE DOSSIERS
    public int getNombreDossiers() {
        return this.dossiers != null ? this.dossiers.size() : 0;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Client)) return false;
        Client client = (Client) o;
        return id != null && id.equals(client.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
