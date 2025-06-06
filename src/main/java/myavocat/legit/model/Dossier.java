package myavocat.legit.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "dossiers")
@Getter
@Setter
@NoArgsConstructor
@JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class, property = "id")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
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
    private User avocat;

    // ✅ CHAMPS OPTIONNELS - Plus obligatoires
    @ManyToOne
    @JoinColumn(name = "client_id", nullable = true)  // nullable = true
    private Client client;

    @ManyToOne
    @JoinColumn(name = "adversaire_id", nullable = true)  // nullable = true
    private Adversaire adversaire;

    @ManyToOne
    @JoinColumn(name = "office_id", nullable = false)
    private Office office;

    @Column(nullable = false)
    private String qualiteProcedurale;

    @Column(length = 500)
    private String contentieux;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "contentieux_date")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate dateContentieux;

    // ✅ NOUVEAUX CHAMPS POUR RELATIONS MULTIPLES
    // Relations Many-to-Many pour gérer plusieurs clients et adversaires
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "dossier_clients",
            joinColumns = @JoinColumn(name = "dossier_id"),
            inverseJoinColumns = @JoinColumn(name = "client_id")
    )
    private List<Client> clients = new ArrayList<>();

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "dossier_adversaires",
            joinColumns = @JoinColumn(name = "dossier_id"),
            inverseJoinColumns = @JoinColumn(name = "adversaire_id")
    )
    private List<Adversaire> adversaires = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // ✅ CHAMPS TRANSIENTS POUR LA COMPATIBILITÉ
    @Transient
    private UUID clientId;

    @Transient
    private UUID adversaireId;

    // ✅ MÉTHODES UTILITAIRES POUR GÉRER LES LISTES

    /**
     * Ajoute un client à la liste
     */
    public void addClient(Client client) {
        if (this.clients == null) {
            this.clients = new ArrayList<>();
        }
        if (!this.clients.contains(client)) {
            this.clients.add(client);
        }
    }

    /**
     * Retire un client de la liste
     */
    public void removeClient(Client client) {
        if (this.clients != null) {
            this.clients.remove(client);
        }
    }

    /**
     * Ajoute un adversaire à la liste
     */
    public void addAdversaire(Adversaire adversaire) {
        if (this.adversaires == null) {
            this.adversaires = new ArrayList<>();
        }
        if (!this.adversaires.contains(adversaire)) {
            this.adversaires.add(adversaire);
        }
    }

    /**
     * Retire un adversaire de la liste
     */
    public void removeAdversaire(Adversaire adversaire) {
        if (this.adversaires != null) {
            this.adversaires.remove(adversaire);
        }
    }

    /**
     * Retourne le client principal (pour compatibilité)
     */
    public Client getClientPrincipal() {
        if (this.client != null) {
            return this.client;
        }
        if (this.clients != null && !this.clients.isEmpty()) {
            return this.clients.get(0);
        }
        return null;
    }

    /**
     * Retourne l'adversaire principal (pour compatibilité)
     */
    public Adversaire getAdversairePrincipal() {
        if (this.adversaire != null) {
            return this.adversaire;
        }
        if (this.adversaires != null && !this.adversaires.isEmpty()) {
            return this.adversaires.get(0);
        }
        return null;
    }

    public String getDateContentieuxFormatted() {
        return dateContentieux != null ?
                dateContentieux.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) : null;
    }
}
