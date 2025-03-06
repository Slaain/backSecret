package myavocat.legit.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "dossiers")
@Getter
@Setter
@NoArgsConstructor
public class Dossier {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(nullable = false, unique = true)
    private String reference;

    @Column(nullable = false)
    private String nomDossier;

    @Column(nullable = false)
    private String typeAffaire;

    @Column(nullable = false)
    private String statut;

    @ManyToOne
    @JoinColumn(name = "avocat_id", nullable = false)
    @JsonIgnoreProperties("dossiers")
    private User avocat;

    @ManyToOne
    @JoinColumn(name = "client_id", nullable = true)
    @JsonIgnoreProperties("dossiers")
    private Client client;

    @ManyToOne
    @JoinColumn(name = "adversaire_id", nullable = true)
    @JsonIgnoreProperties("dossiers")
    private Adversaire adversaire;

    @ManyToOne
    @JoinColumn(name = "office_id", nullable = false)
    @JsonIgnoreProperties("dossiers")
    private Office office;

    @Column(nullable = false)
    private String qualiteProcedurale;

    @Column(length = 500)
    private String contentieux;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
    // Ces champs sont utilisés uniquement pour le transfert des données, pas pour le stockage
    @Transient // Cette annotation indique qu'ils ne sont pas persistés en base de données
    private UUID clientId;

    @Transient
    private UUID adversaireId;

    // Getters et setters
    public UUID getClientId() {
        return clientId;
    }

    public void setClientId(UUID clientId) {
        this.clientId = clientId;
    }

    public UUID getAdversaireId() {
        return adversaireId;
    }

    public void setAdversaireId(UUID adversaireId) {
        this.adversaireId = adversaireId;
    }


}
