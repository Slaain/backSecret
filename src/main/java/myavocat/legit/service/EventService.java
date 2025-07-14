package myavocat.legit.service;

import myavocat.legit.model.Event;
import myavocat.legit.model.User;
import myavocat.legit.model.Dossier;
import myavocat.legit.repository.EventRepository;
import myavocat.legit.repository.UserRepository;
import myavocat.legit.repository.DossierRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
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

    // ✅ CRÉATION D'UN ÉVÉNEMENT
    public Event createEvent(Event event, UUID createdById, UUID dossierId, Set<UUID> participantIds) {
        User creator = userRepository.findById(createdById)
                .orElseThrow(() -> new RuntimeException("Créateur introuvable"));

        event.setCreatedBy(creator);

        // Gestion du dossier (optionnel)
        if (dossierId != null) {
            Dossier dossier = dossierRepository.findById(dossierId)
                    .orElseThrow(() -> new RuntimeException("Dossier introuvable"));
            event.setDossier(dossier);
        }

        // Gestion des participants
        Set<User> participants = new HashSet<>();
        participants.add(creator); // Le créateur est toujours participant

        if (participantIds != null) {
            for (UUID id : participantIds) {
                userRepository.findById(id).ifPresent(participants::add);
            }
        }
        event.setParticipants(participants);

        // Valeurs par défaut
        if (event.getType() == null) {
            event.setType(Event.EventType.AUTRE);
        }
        if (event.getStatus() == null) {
            event.setStatus(Event.EventStatus.PLANIFIE);
        }
        if (event.getPriority() == null) {
            event.setPriority(Event.EventPriority.NORMALE);
        }

        return eventRepository.save(event);
    }

    // ✅ VUE "MON AGENDA" - Combine événements créés + participation
    public List<Event> getMyEvents(UUID userId) {
        List<Event> createdEvents = eventRepository.findByCreatedById(userId);
        List<Event> participantEvents = eventRepository.findByParticipantsId(userId);

        // Combiner et supprimer les doublons
        Set<Event> allEvents = new HashSet<>();
        allEvents.addAll(createdEvents);
        allEvents.addAll(participantEvents);

        return allEvents.stream()
                .sorted(Comparator.comparing(Event::getStart))
                .collect(Collectors.toList());
    }

    // ✅ VUE "MON AGENDA" AVEC FILTRES
    public List<Event> getMyEventsByDate(UUID userId, LocalDateTime startDate, LocalDateTime endDate) {
        List<Event> myEvents = getMyEvents(userId);
        return myEvents.stream()
                .filter(event -> !event.getStart().isBefore(startDate) && !event.getStart().isAfter(endDate))
                .collect(Collectors.toList());
    }

    public List<Event> getMyEventsByType(UUID userId, Event.EventType type) {
        List<Event> createdEvents = eventRepository.findByCreatedByIdAndType(userId, type);
        List<Event> participantEvents = eventRepository.findByParticipantsId(userId).stream()
                .filter(event -> event.getType() == type)
                .collect(Collectors.toList());

        Set<Event> allEvents = new HashSet<>();
        allEvents.addAll(createdEvents);
        allEvents.addAll(participantEvents);

        return allEvents.stream()
                .sorted(Comparator.comparing(Event::getStart))
                .collect(Collectors.toList());
    }

    public List<Event> getMyEventsByDossier(UUID userId, UUID dossierId) {
        List<Event> createdEvents = eventRepository.findByCreatedByIdAndDossierId(userId, dossierId);
        List<Event> participantEvents = eventRepository.findByParticipantsId(userId).stream()
                .filter(event -> event.getDossier() != null && event.getDossier().getId().equals(dossierId))
                .collect(Collectors.toList());

        Set<Event> allEvents = new HashSet<>();
        allEvents.addAll(createdEvents);
        allEvents.addAll(participantEvents);

        return allEvents.stream()
                .sorted(Comparator.comparing(Event::getStart))
                .collect(Collectors.toList());
    }

    // ✅ VUE "AGENDA CABINET" - Événements publics du cabinet
    public List<Event> getCabinetEvents(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Utilisateur introuvable"));

        if (user.getOffice() == null) {
            throw new RuntimeException("Utilisateur non rattaché à un cabinet");
        }

        UUID officeId = user.getOffice().getId();

        // Récupérer tous les événements du cabinet
        List<Event> allOfficeEvents = eventRepository.findByCreatedByOfficeId(officeId);

        // Filtrer pour ne garder que les événements publics + télétravails
        return allOfficeEvents.stream()
                .filter(event -> !event.isPrivate() || event.getType() == Event.EventType.TELETRAVAIL)
                .sorted(Comparator.comparing(Event::getStart))
                .collect(Collectors.toList());
    }

    // ✅ VUE "AGENDA CABINET" AVEC FILTRES
    public List<Event> getCabinetEventsByDate(UUID userId, LocalDateTime startDate, LocalDateTime endDate) {
        List<Event> cabinetEvents = getCabinetEvents(userId);
        return cabinetEvents.stream()
                .filter(event -> !event.getStart().isBefore(startDate) && !event.getStart().isAfter(endDate))
                .collect(Collectors.toList());
    }

    public List<Event> getCabinetEventsByType(UUID userId, Event.EventType type) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Utilisateur introuvable"));

        UUID officeId = user.getOffice().getId();
        List<Event> typeEvents = eventRepository.findByCreatedByOfficeIdAndType(officeId, type);

        // Filtrer les événements publics + télétravails
        return typeEvents.stream()
                .filter(event -> !event.isPrivate() || event.getType() == Event.EventType.TELETRAVAIL)
                .sorted(Comparator.comparing(Event::getStart))
                .collect(Collectors.toList());
    }

    public List<Event> getCabinetEventsByDossier(UUID userId, UUID dossierId) {
        List<Event> cabinetEvents = getCabinetEvents(userId);
        return cabinetEvents.stream()
                .filter(event -> event.getDossier() != null && event.getDossier().getId().equals(dossierId))
                .collect(Collectors.toList());
    }

    // ✅ GESTION DES CONFLITS - Version simplifiée
    public List<Event> checkConflicts(UUID userId, LocalDateTime start, LocalDateTime end) {
        List<Event> myEvents = getMyEvents(userId);
        return myEvents.stream()
                .filter(event ->
                        (event.getStart().isBefore(end) && event.getEndTime().isAfter(start)))
                .collect(Collectors.toList());
    }

    public boolean hasConflicts(UUID userId, LocalDateTime start, LocalDateTime end) {
        return !checkConflicts(userId, start, end).isEmpty();
    }

    // ✅ MÉTHODES CRUD DE BASE
    public Optional<Event> getEventById(UUID id) {
        return eventRepository.findById(id);
    }

    public void deleteEvent(UUID id) {
        eventRepository.deleteById(id);
    }

    public List<Event> getAllEvents() {
        return eventRepository.findAll();
    }

    // ✅ MISE À JOUR SIMPLIFIÉE
    public Event updateEvent(UUID eventId, Event updatedData, Set<UUID> participantIds) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new RuntimeException("Événement non trouvé"));

        // Mise à jour des champs de base
        event.setTitle(updatedData.getTitle());
        event.setDescription(updatedData.getDescription());
        event.setStart(updatedData.getStart());
        event.setEndTime(updatedData.getEndTime());
        event.setReminderMinutesBefore(updatedData.getReminderMinutesBefore());
        event.setPrivate(updatedData.isPrivate());

        // Mise à jour des champs enum
        if (updatedData.getType() != null) {
            event.setType(updatedData.getType());
        }
        if (updatedData.getStatus() != null) {
            event.setStatus(updatedData.getStatus());
        }
        if (updatedData.getPriority() != null) {
            event.setPriority(updatedData.getPriority());
        }

        // Mise à jour des participants
        if (participantIds != null) {
            Set<User> participants = new HashSet<>();
            participants.add(event.getCreatedBy()); // Garder le créateur

            for (UUID id : participantIds) {
                userRepository.findById(id).ifPresent(participants::add);
            }
            event.setParticipants(participants);
        }

        return eventRepository.save(event);
    }

    // ✅ MÉTHODES UTILITAIRES
    public List<Event> getUpcomingEvents(UUID userId) {
        List<Event> myEvents = getMyEvents(userId);
        LocalDateTime now = LocalDateTime.now();

        return myEvents.stream()
                .filter(event -> event.getStart().isAfter(now) && event.getStatus() != Event.EventStatus.TERMINE)
                .sorted(Comparator.comparing(Event::getStart))
                .collect(Collectors.toList());
    }

    public List<Event> getEventsNeedingReminder(UUID userId) {
        List<Event> upcomingEvents = getUpcomingEvents(userId);
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime reminderTime = now.plusMinutes(60);

        return upcomingEvents.stream()
                .filter(event -> event.getReminderMinutesBefore() != null)
                .filter(event -> event.getStart().isAfter(now) && event.getStart().isBefore(reminderTime))
                .collect(Collectors.toList());
    }
}
