package myavocat.legit.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.Set;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "adversaires")
public class Adversaire {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(length = 100)
    private String nom;

    @Column(length = 100)
    private String prenom;

    @Column(unique = true, length = 255)
    private String email;

    @Column(length = 15)
    private String telephone;

    @Column
    private String type; // Particulier, Entreprise...

    @Column
    private String qualite; // Demandeur, Défendeur...

    @Column(length = 255)
    private String commune; // Ville

    @ManyToOne
    @JoinColumn(name = "office_id", nullable = true)
    private Office office;

    @ManyToMany
    @JoinTable(
            name = "adversaire_dossier",
            joinColumns = @JoinColumn(name = "adversaire_id"),
            inverseJoinColumns = @JoinColumn(name = "dossier_id")
    )
    private Set<Dossier> dossiers; // Un adversaire peut être lié à plusieurs dossiers

    public Adversaire() {}

    // Getters et Setters générés par Lombok
}