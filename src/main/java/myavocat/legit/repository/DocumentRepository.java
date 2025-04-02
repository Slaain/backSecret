package myavocat.legit.repository;

import myavocat.legit.model.Document;
import myavocat.legit.model.Dossier;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface DocumentRepository extends JpaRepository<Document, UUID> {
    List<Document> findByDossierId(UUID dossierId);
    List<Document> findByUploadedById(UUID userId);

}
