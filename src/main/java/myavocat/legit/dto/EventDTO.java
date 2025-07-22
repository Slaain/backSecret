package myavocat.legit.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import myavocat.legit.model.Event;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EventDTO {
    private UUID id;
    private String title;
    private String description;
    private LocalDateTime start;
    private LocalDateTime end;
    private Integer reminderMinutesBefore;
    private boolean isPrivate;

    // Enums
    private String type;
    private String status;
    private String priority;
    private String typePersonnalise;

    // Timestamps
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Relations (IDs seulement)
    private UUID createdById;
    private String createdByName;
    private UUID dossierId;
    private String dossierNom;
    private Set<UUID> participantIds;
    private Set<String> participantNames;

    // Office info
    private UUID officeId;
    private String officeName;

    // Client info (si dossier)
    private UUID clientId;
    private String clientName;

    // Méthode de conversion depuis l'entité
    public static EventDTO fromEntity(Event event) {
        EventDTO dto = new EventDTO();

        // Champs simples
        dto.setId(event.getId());
        dto.setTitle(event.getTitle());
        dto.setDescription(event.getDescription());
        dto.setStart(event.getStart());
        dto.setEnd(event.getEndTime());
        dto.setReminderMinutesBefore(event.getReminderMinutesBefore());
        dto.setPrivate(event.isPrivate());
        dto.setCreatedAt(event.getCreatedAt());
        dto.setUpdatedAt(event.getUpdatedAt());

        // Enums
        if (event.getType() != null) {
            dto.setType(event.getType().name());
        }
        if (event.getStatus() != null) {
            dto.setStatus(event.getStatus().name());
        }
        if (event.getPriority() != null) {
            dto.setPriority(event.getPriority().name());
        }
        dto.setTypePersonnalise(event.getTypePersonnalise());

        // Relations - CreatedBy
        if (event.getCreatedBy() != null) {
            dto.setCreatedById(event.getCreatedBy().getId());
            dto.setCreatedByName(event.getCreatedBy().getNom() + " " + event.getCreatedBy().getPrenom());
        }

        // Relations - Dossier
        if (event.getDossier() != null) {
            dto.setDossierId(event.getDossier().getId());
            dto.setDossierNom(event.getDossier().getNomDossier());

            // Client info via dossier
            if (event.getDossier().getClientPrincipal() != null) {
                dto.setClientId(event.getDossier().getClientPrincipal().getId());
                dto.setClientName(event.getDossier().getClientPrincipal().getNom() + " " +
                        event.getDossier().getClientPrincipal().getPrenom());
            }
        }

        // Relations - Participants
        if (event.getParticipants() != null && !event.getParticipants().isEmpty()) {
            dto.setParticipantIds(
                    event.getParticipants().stream()
                            .map(user -> user.getId())
                            .collect(Collectors.toSet())
            );
            dto.setParticipantNames(
                    event.getParticipants().stream()
                            .map(user -> user.getNom() + " " + user.getPrenom())
                            .collect(Collectors.toSet())
            );
        }

        // Office info
        if (event.getOffice() != null) {
            dto.setOfficeId(event.getOffice().getId());
            dto.setOfficeName(event.getOffice().getName());

        }

        return dto;
    }
}
