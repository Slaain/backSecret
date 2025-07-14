package myavocat.legit.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "events")
@Getter
@Setter
@NoArgsConstructor
public class Event {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(nullable = false)
    private String title;

    @Column(length = 1000)
    private String description;

    @Column(nullable = false)
    private LocalDateTime start;

    @Column(nullable = false)
    private LocalDateTime endTime;

    @Column(name = "reminder_minutes_before")
    private Integer reminderMinutesBefore;

    @Column(name = "is_private")
    private boolean isPrivate = false;

    // ✅ NOUVEAUX CHAMPS AJOUTÉS
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EventType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EventStatus status = EventStatus.PLANIFIE;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EventPriority priority = EventPriority.NORMALE;

    // Champ libre pour type personnalisé quand type = AUTRE
    @Column(name = "type_personnalise")
    private String typePersonnalise;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // ✅ RELATIONS EXISTANTES
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", nullable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private User createdBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dossier_id")
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private Dossier dossier;

    @ManyToMany
    @JoinTable(
            name = "event_participants",
            joinColumns = @JoinColumn(name = "event_id"),
            inverseJoinColumns = @JoinColumn(name = "user_id")
    )
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private Set<User> participants = new HashSet<>();

    // ✅ MÉTHODES LIFECYCLE
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();

        // Ajouter automatiquement le créateur aux participants
        if (createdBy != null) {
            addParticipant(createdBy);
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // ✅ MÉTHODES UTILITAIRES

    /**
     * Ajoute un participant à l'événement
     */
    public void addParticipant(User user) {
        if (this.participants == null) {
            this.participants = new HashSet<>();
        }
        this.participants.add(user);
    }

    /**
     * Retire un participant de l'événement
     */
    public void removeParticipant(User user) {
        if (this.participants != null) {
            this.participants.remove(user);
        }
    }

    /**
     * Récupère le client lié via le dossier
     */
    public Client getClient() {
        return this.dossier != null ? this.dossier.getClientPrincipal() : null;
    }

    /**
     * Récupère l'office via le créateur ou le dossier
     */
    public Office getOffice() {
        if (this.dossier != null && this.dossier.getOffice() != null) {
            return this.dossier.getOffice();
        }
        return this.createdBy != null ? this.createdBy.getOffice() : null;
    }

    /**
     * Vérifie si l'événement est visible par un utilisateur donné
     */
    public boolean isVisibleBy(User user) {
        // Télétravail = toujours visible par le cabinet
        if (this.type == EventType.TELETRAVAIL) {
            return user.getOffice().equals(this.getOffice());
        }

        // Événement privé = seulement les participants
        if (this.isPrivate) {
            return this.participants.contains(user);
        }

        // Événement public = visible par le cabinet
        return user.getOffice().equals(this.getOffice());
    }

    /**
     * Retourne le type d'événement formaté pour l'affichage
     */
    public String getTypeFormatted() {
        if (this.type == EventType.AUTRE && this.typePersonnalise != null) {
            return this.typePersonnalise;
        }
        return this.type.getDisplayName();
    }

    // ✅ ENUMS POUR LES TYPES, STATUTS ET PRIORITÉS

    public enum EventType {
        RDV_CLIENT("RDV Client"),
        AUDIENCE("Audience"),
        ECHEANCE_LEGALE("Échéance légale"),
        REUNION_INTERNE("Réunion interne"),
        FORMATION("Formation"),
        CONGE("Congé"),
        TELETRAVAIL("Télétravail"),
        AUTRE("Autre");

        private final String displayName;

        EventType(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    public enum EventStatus {
        PLANIFIE("Planifié"),
        CONFIRME("Confirmé"),
        TERMINE("Terminé");

        private final String displayName;

        EventStatus(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    public enum EventPriority {
        NORMALE("Normale"),
        URGENTE("Urgente");

        private final String displayName;

        EventPriority(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }
}
