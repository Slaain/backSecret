package myavocat.legit.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.Set;
import java.util.UUID;


@Getter
@Setter
@Entity
@Table(name = "clients")
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

    @ManyToMany
    @JoinTable(
            name = "client_dossier",
            joinColumns = @JoinColumn(name = "client_id"),
            inverseJoinColumns = @JoinColumn(name = "dossier_id")
    )
    private Set<Dossier> dossiers; // Un client peut être lié à plusieurs dossiers

    public Client() {}

    // Getters et Setters
}
