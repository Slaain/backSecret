package myavocat.legit.repository;

import myavocat.legit.model.Event;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;
import java.time.LocalDateTime;

@Repository
public interface EventRepository extends JpaRepository<Event, UUID> {

    List<Event> findByCreatedById(UUID userId);

    List<Event> findByParticipantsId(UUID userId);

    List<Event> findByDossierId(UUID dossierId);

    List<Event> findByStartBetween(LocalDateTime start, LocalDateTime end);
}
