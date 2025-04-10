package myavocat.legit.repository;

import myavocat.legit.model.Signature;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface SignatureRepository extends JpaRepository<Signature, UUID> {
    List<Signature> findByDocumentId(UUID documentId);
    List<Signature> findBySignedById(UUID userId);
}
