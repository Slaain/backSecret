package myavocat.legit.service;

import myavocat.legit.model.Event;
import myavocat.legit.model.User;
import myavocat.legit.model.Dossier;
import myavocat.legit.repository.EventRepository;
import myavocat.legit.repository.UserRepository;
import myavocat.legit.repository.DossierRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

@Service
public class EventService {

    private final EventRepository eventRepository;
    private final UserRepository userRepository;
    private final DossierRepository dossierRepository;

    @Autowired
    public EventService(EventRepository eventRepository, UserRepository userRepository, DossierRepository dossierRepository) {
        this.eventRepository = eventRepository;
        this.userRepository = userRepository;
        this.dossierRepository = dossierRepository;
    }

    public Event createEvent(Event event, UUID createdById, UUID dossierId, Set<UUID> participantIds) {
        User creator = userRepository.findById(createdById)
                .orElseThrow(() -> new RuntimeException("Créateur introuvable"));
        event.setCreatedBy(creator);

        if (dossierId != null) {
            Dossier dossier = dossierRepository.findById(dossierId)
                    .orElseThrow(() -> new RuntimeException("Dossier introuvable"));
            event.setDossier(dossier);
        }

        Set<User> participants = new HashSet<>();
        for (UUID id : participantIds) {
            userRepository.findById(id).ifPresent(participants::add);
        }
        event.setParticipants(participants);

        return eventRepository.save(event);
    }

    public List<Event> getAllEvents() {
        return eventRepository.findAll();
    }

    public Optional<Event> getEventById(UUID id) {
        return eventRepository.findById(id);
    }

    public void deleteEvent(UUID id) {
        eventRepository.deleteById(id);
    }

    public List<Event> getEventsByUser(UUID userId) {
        return eventRepository.findByParticipantsId(userId);
    }

    public List<Event> getEventsByCreator(UUID userId) {
        return eventRepository.findByCreatedById(userId);
    }

    public List<Event> getEventsByDossier(UUID dossierId) {
        return eventRepository.findByDossierId(dossierId);
    }

    public List<Event> getEventsInPeriod(LocalDateTime start, LocalDateTime end) {
        return eventRepository.findByStartBetween(start, end);
    }

    public Event updateEvent(UUID eventId, Event updatedData, Set<UUID> participantIds) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new RuntimeException("Événement non trouvé"));

        event.setTitle(updatedData.getTitle());
        event.setDescription(updatedData.getDescription());
        event.setStart(updatedData.getStart());
        event.setEnd(updatedData.getEnd());
        event.setReminderMinutesBefore(updatedData.getReminderMinutesBefore());

        // ✅ CORRECTION ICI - Ligne 87
        // Remplacer : event.setIsPrivate(updatedData.isPrivate());
        // Par :
        event.setPrivate(updatedData.isPrivate());

        if (updatedData.getDossier() != null) {
            Dossier dossier = dossierRepository.findById(updatedData.getDossier().getId())
                    .orElseThrow(() -> new RuntimeException("Dossier introuvable"));
            event.setDossier(dossier);
        }

        Set<User> participants = new HashSet<>();
        for (UUID id : participantIds) {
            userRepository.findById(id).ifPresent(participants::add);
        }
        event.setParticipants(participants);

        return eventRepository.save(event);
    }
}
