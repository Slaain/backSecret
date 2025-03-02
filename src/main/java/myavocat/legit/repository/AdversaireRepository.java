package myavocat.legit.repository;

import myavocat.legit.model.Adversaire;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface AdversaireRepository extends JpaRepository<Adversaire, UUID> {
    boolean existsByEmail(String email);
    List<Adversaire> findByDossierId(UUID dossierId);
}