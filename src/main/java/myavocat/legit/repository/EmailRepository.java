package myavocat.legit.repository;

import myavocat.legit.model.Email;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface EmailRepository extends JpaRepository<Email, UUID> {

    // Récupérer les 10 derniers emails reçus
    List<Email> findTop10ByOrderByReceivedAtDesc();

    // Récupérer les emails par compte (Gmail, Outlook…)
    List<Email> findByEmailAccountIdOrderByReceivedAtDesc(UUID emailAccountId);
}
