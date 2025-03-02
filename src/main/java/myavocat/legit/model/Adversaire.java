package myavocat.legit.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

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

    @ManyToOne
    @JoinColumn(name = "dossier_id")
    private Dossier dossier; // Un adversaire appartient à un seul dossier

    public Adversaire() {}

    // Getters et Setters
}