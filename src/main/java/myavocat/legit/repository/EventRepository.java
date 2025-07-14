package myavocat.legit.repository;

import myavocat.legit.model.Event;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface EventRepository extends JpaRepository<Event, UUID> {

    // ✅ MÉTHODES DE BASE (comme tes autres repos)
    List<Event> findByCreatedById(UUID userId);
    List<Event> findByParticipantsId(UUID userId);
    List<Event> findByDossierId(UUID dossierId);
    List<Event> findByStartBetween(LocalDateTime start, LocalDateTime end);

    // ✅ MÉTHODES POUR FILTRES
    List<Event> findByType(Event.EventType type);
    List<Event> findByStatus(Event.EventStatus status);
    List<Event> findByPriority(Event.EventPriority priority);
    List<Event> findByIsPrivate(boolean isPrivate);

    // ✅ MÉTHODES COMBINÉES SIMPLES
    List<Event> findByCreatedByIdAndType(UUID userId, Event.EventType type);
    List<Event> findByCreatedByIdAndStartBetween(UUID userId, LocalDateTime start, LocalDateTime end);
    List<Event> findByCreatedByIdAndDossierId(UUID userId, UUID dossierId);

    // ✅ MÉTHODES POUR L'OFFICE (CABINET)
    List<Event> findByCreatedByOfficeId(UUID officeId);
    List<Event> findByCreatedByOfficeIdAndIsPrivate(UUID officeId, boolean isPrivate);
    List<Event> findByCreatedByOfficeIdAndType(UUID officeId, Event.EventType type);
    List<Event> findByCreatedByOfficeIdAndStartBetween(UUID officeId, LocalDateTime start, LocalDateTime end);
}
