package myavocat.legit.repository;

import myavocat.legit.model.Snippet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface SnippetRepository extends JpaRepository<Snippet, UUID> {
    List<Snippet> findByUserIdAndType(UUID userId, String type);
}
