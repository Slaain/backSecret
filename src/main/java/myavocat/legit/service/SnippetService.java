package myavocat.legit.service;

import myavocat.legit.model.Snippet;
import myavocat.legit.model.User;
import myavocat.legit.repository.SnippetRepository;
import myavocat.legit.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class SnippetService {

    private final SnippetRepository snippetRepository;
    private final UserRepository userRepository;

    @Autowired
    public SnippetService(SnippetRepository snippetRepository, UserRepository userRepository) {
        this.snippetRepository = snippetRepository;
        this.userRepository = userRepository;
    }

    public Snippet createSnippet(UUID userId, String type, String label, String content) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

        Snippet snippet = new Snippet();
        snippet.setUser(user);
        snippet.setType(type.toLowerCase());
        snippet.setLabel(label);
        snippet.setContent(content);

        return snippetRepository.save(snippet);
    }

    public List<Snippet> getSnippetsByUserAndType(UUID userId, String type) {
        return snippetRepository.findByUserIdAndType(userId, type.toLowerCase());
    }

    public void deleteSnippet(UUID id) {
        snippetRepository.deleteById(id);
    }

    public Snippet getSnippetById(UUID id) {
        return snippetRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Snippet non trouvé"));
    }
}
