package myavocat.legit.controller;

import myavocat.legit.model.Event;
import myavocat.legit.service.EventService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/api/events")
public class EventController {

    private final EventService eventService;

    @Autowired
    public EventController(EventService eventService) {
        this.eventService = eventService;
    }

    @PostMapping
    public ResponseEntity<Event> createEvent(
            @RequestBody Event event,
            @RequestParam UUID createdBy,
            @RequestParam(required = false) UUID dossierId,
            @RequestParam Set<UUID> participantIds
    ) {
        Event created = eventService.createEvent(event, createdBy, dossierId, participantIds);
        return ResponseEntity.ok(created);
    }

    @GetMapping
    public ResponseEntity<List<Event>> getAllEvents() {
        return ResponseEntity.ok(eventService.getAllEvents());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Event> getEventById(@PathVariable UUID id) {
        return eventService.getEventById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteEvent(@PathVariable UUID id) {
        eventService.deleteEvent(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<Event>> getEventsByUser(@PathVariable UUID userId) {
        return ResponseEntity.ok(eventService.getEventsByUser(userId));
    }

    @GetMapping("/creator/{creatorId}")
    public ResponseEntity<List<Event>> getEventsByCreator(@PathVariable UUID creatorId) {
        return ResponseEntity.ok(eventService.getEventsByCreator(creatorId));
    }

    @GetMapping("/dossier/{dossierId}")
    public ResponseEntity<List<Event>> getEventsByDossier(@PathVariable UUID dossierId) {
        return ResponseEntity.ok(eventService.getEventsByDossier(dossierId));
    }

    @GetMapping("/range")
    public ResponseEntity<List<Event>> getEventsInRange(
            @RequestParam LocalDateTime start,
            @RequestParam LocalDateTime end
    ) {
        return ResponseEntity.ok(eventService.getEventsInPeriod(start, end));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Event> updateEvent(
            @PathVariable UUID id,
            @RequestBody Event updatedEvent,
            @RequestParam Set<UUID> participantIds
    ) {
        return ResponseEntity.ok(eventService.updateEvent(id, updatedEvent, participantIds));
    }
}
