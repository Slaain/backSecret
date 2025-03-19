package myavocat.legit.repository;

import myavocat.legit.model.Client;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ClientRepository extends JpaRepository<Client, UUID> {
    boolean existsByEmail(String email);
    List<Client> findByOfficeId(UUID officeId);


    Optional<Client> findByEmail(String email);
}

