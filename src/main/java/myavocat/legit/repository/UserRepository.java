package myavocat.legit.repository;

import myavocat.legit.model.User;
import myavocat.legit.model.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);


    // Rechercher des utilisateurs par nom ou prénom
    List<User> findByNomContainingIgnoreCaseOrPrenomContainingIgnoreCase(String nom, String prenom);

    // Rechercher des utilisateurs par rôle
    List<User> findByRole(UserRole role);
}