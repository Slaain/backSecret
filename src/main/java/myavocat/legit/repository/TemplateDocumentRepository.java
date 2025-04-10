package myavocat.legit.repository;

import myavocat.legit.model.TemplateDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface TemplateDocumentRepository extends JpaRepository<TemplateDocument, UUID> {
    List<TemplateDocument> findByCreatedById(UUID userId);
}
