package myavocat.legit.repository;

import myavocat.legit.model.Office;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface OfficeRepository extends JpaRepository<Office, UUID> {
    Optional<Office> findByName(String name);
    boolean existsByName(String name);

    List<Office> findByUsers_Id(UUID userId);

}