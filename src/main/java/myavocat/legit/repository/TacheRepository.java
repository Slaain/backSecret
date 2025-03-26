package myavocat.legit.repository;

import myavocat.legit.model.Tache;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

@Repository
public interface TacheRepository extends JpaRepository<Tache, Long> {

    List<Tache> findByDossierId(UUID dossierId);

    List<Tache> findByUserId(UUID userId);
}
