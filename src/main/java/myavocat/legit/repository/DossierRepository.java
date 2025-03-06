package myavocat.legit.repository;

import myavocat.legit.model.Dossier;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface DossierRepository extends JpaRepository<Dossier, UUID> {
    List<Dossier> findByAvocatId(UUID avocatId);
    @Query("SELECT d FROM Dossier d WHERE d.client.id = :clientId")
    List<Dossier> findByClientId(@Param("clientId") UUID clientId);
    List<Dossier> findByOfficeId(UUID officeId);
    List<Dossier> findByStatut(String statut);
    List<Dossier> findByAvocatIdAndStatut(UUID avocatId, String statut);


}
