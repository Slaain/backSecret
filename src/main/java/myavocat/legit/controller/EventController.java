package myavocat.legit.controller;

import myavocat.legit.dto.EventDTO;
import myavocat.legit.model.Event;
import myavocat.legit.service.EventService;
import myavocat.legit.response.ApiResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/api/events")
@CrossOrigin(origins = "*")
public class EventController {

    private final EventService eventService;

    @Autowired
    public EventController(EventService eventService) {
        this.eventService = eventService;
    }

    // ✅ CRÉATION D'UN ÉVÉNEMENT AVEC GESTION DES CONFLITS
    @PostMapping
    public ResponseEntity<ApiResponse> createEvent(
            @RequestBody Event event,
            @RequestParam UUID createdBy,
            @RequestParam(required = false) UUID dossierId,
            @RequestParam(required = false) Set<UUID> participantIds
    ) {
        try {
            // Vérifier les conflits avant création
            List<EventDTO> conflicts = eventService.checkConflicts(createdBy, event.getStart(), event.getEndTime());

            EventDTO created = eventService.createEvent(event, createdBy, dossierId, participantIds);

            // Retourner avec avertissement si conflits détectés
            if (!conflicts.isEmpty()) {
                Map<String, Object> data = new HashMap<>();
                data.put("event", created);
                data.put("conflicts", conflicts);
                data.put("conflictWarning", "Attention : " + conflicts.size() + " conflit(s) détecté(s) sur cette plage horaire");

                return ResponseEntity.ok(new ApiResponse(true, "Événement créé avec avertissement de conflit", data));
            }

            return ResponseEntity.ok(new ApiResponse(true, "Événement créé avec succès", created));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(new ApiResponse(false, "Erreur lors de la création : " + e.getMessage(), null));
        }
    }

    // ✅ VUE "MON AGENDA" - ENDPOINT PRINCIPAL
    @GetMapping("/my-agenda")
    public ResponseEntity<ApiResponse> getMyAgenda(@RequestParam UUID userId) {
        try {
            List<EventDTO> events = eventService.getMyEvents(userId);
            return ResponseEntity.ok(new ApiResponse(true, "Mon agenda récupéré avec succès", events));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(new ApiResponse(false, "Erreur : " + e.getMessage(), null));
        }
    }

    // ✅ VUE "AGENDA CABINET" - ENDPOINT PRINCIPAL
    @GetMapping("/cabinet-agenda")
    public ResponseEntity<ApiResponse> getCabinetAgenda(@RequestParam UUID userId) {
        try {
            List<EventDTO> events = eventService.getCabinetEvents(userId);
            return ResponseEntity.ok(new ApiResponse(true, "Agenda cabinet récupéré avec succès", events));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(new ApiResponse(false, "Erreur : " + e.getMessage(), null));
        }
    }

    // ✅ FILTRES POUR "MON AGENDA"
    @GetMapping("/my-agenda/by-date")
    public ResponseEntity<ApiResponse> getMyAgendaByDate(
            @RequestParam UUID userId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate
    ) {
        try {
            List<EventDTO> events = eventService.getMyEventsByDate(userId, startDate, endDate);
            return ResponseEntity.ok(new ApiResponse(true, "Événements filtrés par date", events));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(new ApiResponse(false, "Erreur : " + e.getMessage(), null));
        }
    }

    @GetMapping("/my-agenda/by-type")
    public ResponseEntity<ApiResponse> getMyAgendaByType(
            @RequestParam UUID userId,
            @RequestParam Event.EventType type
    ) {
        try {
            List<EventDTO> events = eventService.getMyEventsByType(userId, type);
            return ResponseEntity.ok(new ApiResponse(true, "Événements filtrés par type", events));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(new ApiResponse(false, "Erreur : " + e.getMessage(), null));
        }
    }

    @GetMapping("/my-agenda/by-dossier")
    public ResponseEntity<ApiResponse> getMyAgendaByDossier(
            @RequestParam UUID userId,
            @RequestParam UUID dossierId
    ) {
        try {
            List<EventDTO> events = eventService.getMyEventsByDossier(userId, dossierId);
            return ResponseEntity.ok(new ApiResponse(true, "Événements filtrés par dossier", events));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(new ApiResponse(false, "Erreur : " + e.getMessage(), null));
        }
    }

    // ✅ FILTRES POUR "AGENDA CABINET"
    @GetMapping("/cabinet-agenda/by-date")
    public ResponseEntity<ApiResponse> getCabinetAgendaByDate(
            @RequestParam UUID userId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate
    ) {
        try {
            List<EventDTO> events = eventService.getCabinetEventsByDate(userId, startDate, endDate);
            return ResponseEntity.ok(new ApiResponse(true, "Agenda cabinet filtré par date", events));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(new ApiResponse(false, "Erreur : " + e.getMessage(), null));
        }
    }

    @GetMapping("/cabinet-agenda/by-type")
    public ResponseEntity<ApiResponse> getCabinetAgendaByType(
            @RequestParam UUID userId,
            @RequestParam Event.EventType type
    ) {
        try {
            List<EventDTO> events = eventService.getCabinetEventsByType(userId, type);
            return ResponseEntity.ok(new ApiResponse(true, "Agenda cabinet filtré par type", events));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(new ApiResponse(false, "Erreur : " + e.getMessage(), null));
        }
    }

    @GetMapping("/cabinet-agenda/by-dossier")
    public ResponseEntity<ApiResponse> getCabinetAgendaByDossier(
            @RequestParam UUID userId,
            @RequestParam UUID dossierId
    ) {
        try {
            List<EventDTO> events = eventService.getCabinetEventsByDossier(userId, dossierId);
            return ResponseEntity.ok(new ApiResponse(true, "Agenda cabinet filtré par dossier", events));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(new ApiResponse(false, "Erreur : " + e.getMessage(), null));
        }
    }

    // ✅ GESTION DES CONFLITS
    @GetMapping("/conflicts")
    public ResponseEntity<ApiResponse> checkConflicts(
            @RequestParam UUID userId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end
    ) {
        try {
            List<EventDTO> conflicts = eventService.checkConflicts(userId, start, end);
            Map<String, Object> data = new HashMap<>();
            data.put("conflicts", conflicts);
            data.put("hasConflicts", !conflicts.isEmpty());
            data.put("conflictCount", conflicts.size());

            return ResponseEntity.ok(new ApiResponse(true, "Vérification des conflits terminée", data));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(new ApiResponse(false, "Erreur : " + e.getMessage(), null));
        }
    }

    // ✅ MÉTHODES UTILITAIRES
    @GetMapping("/upcoming")
    public ResponseEntity<ApiResponse> getUpcomingEvents(@RequestParam UUID userId) {
        try {
            List<EventDTO> events = eventService.getUpcomingEvents(userId);
            return ResponseEntity.ok(new ApiResponse(true, "Prochains événements", events));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(new ApiResponse(false, "Erreur : " + e.getMessage(), null));
        }
    }

    @GetMapping("/reminders")
    public ResponseEntity<ApiResponse> getEventsNeedingReminder(@RequestParam UUID userId) {
        try {
            List<EventDTO> events = eventService.getEventsNeedingReminder(userId);
            return ResponseEntity.ok(new ApiResponse(true, "Événements nécessitant un rappel", events));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(new ApiResponse(false, "Erreur : " + e.getMessage(), null));
        }
    }

    // ✅ CRUD DE BASE
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse> getEventById(@PathVariable UUID id) {
        try {
            Optional<EventDTO> event = eventService.getEventById(id);
            if (event.isPresent()) {
                return ResponseEntity.ok(new ApiResponse(true, "Événement trouvé", event.get()));
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(new ApiResponse(false, "Erreur : " + e.getMessage(), null));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse> updateEvent(
            @PathVariable UUID id,
            @RequestBody Event updatedEvent,
            @RequestParam(required = false) Set<UUID> participantIds
    ) {
        try {
            EventDTO updated = eventService.updateEvent(id, updatedEvent, participantIds);
            return ResponseEntity.ok(new ApiResponse(true, "Événement mis à jour avec succès", updated));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(new ApiResponse(false, "Erreur lors de la mise à jour : " + e.getMessage(), null));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse> deleteEvent(@PathVariable UUID id) {
        try {
            eventService.deleteEvent(id);
            return ResponseEntity.ok(new ApiResponse(true, "Événement supprimé avec succès", null));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(new ApiResponse(false, "Erreur lors de la suppression : " + e.getMessage(), null));
        }
    }

    // ✅ ENDPOINT POUR RÉCUPÉRER LES TYPES D'ÉVÉNEMENTS DISPONIBLES
    @GetMapping("/types")
    public ResponseEntity<ApiResponse> getEventTypes() {
        try {
            return ResponseEntity.ok(new ApiResponse(true, "Types d'événements disponibles", Event.EventType.values()));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(new ApiResponse(false, "Erreur : " + e.getMessage(), null));
        }
    }

    // ✅ ENDPOINT POUR RÉCUPÉRER LES STATUTS DISPONIBLES
    @GetMapping("/statuses")
    public ResponseEntity<ApiResponse> getEventStatuses() {
        try {
            return ResponseEntity.ok(new ApiResponse(true, "Statuts d'événements disponibles", Event.EventStatus.values()));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(new ApiResponse(false, "Erreur : " + e.getMessage(), null));
        }
    }

    // ✅ ENDPOINT POUR RÉCUPÉRER LES PRIORITÉS DISPONIBLES
    @GetMapping("/priorities")
    public ResponseEntity<ApiResponse> getEventPriorities() {
        try {
            return ResponseEntity.ok(new ApiResponse(true, "Priorités d'événements disponibles", Event.EventPriority.values()));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(new ApiResponse(false, "Erreur : " + e.getMessage(), null));
        }
    }
}
