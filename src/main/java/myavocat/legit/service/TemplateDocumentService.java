package myavocat.legit.service;

import myavocat.legit.model.TemplateDocument;
import myavocat.legit.model.User;
import myavocat.legit.repository.TemplateDocumentRepository;
import myavocat.legit.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class TemplateDocumentService {

    private final TemplateDocumentRepository templateRepository;
    private final UserRepository userRepository;

    @Autowired
    public TemplateDocumentService(TemplateDocumentRepository templateRepository, UserRepository userRepository) {
        this.templateRepository = templateRepository;
        this.userRepository = userRepository;
    }

    public TemplateDocument createTemplate(UUID userId, String title, String content) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

        TemplateDocument template = new TemplateDocument();
        template.setTitle(title);
        template.setContent(content);
        template.setCreatedBy(user);

        return templateRepository.save(template);
    }

    public List<TemplateDocument> getTemplatesByUser(UUID userId) {
        return templateRepository.findByCreatedById(userId);
    }

    public TemplateDocument getTemplateById(UUID id) {
        return templateRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Modèle non trouvé"));
    }

    public TemplateDocument updateTemplate(UUID id, String newTitle, String newContent) {
        TemplateDocument template = getTemplateById(id);
        template.setTitle(newTitle);
        template.setContent(newContent);
        return templateRepository.save(template);
    }

    public void deleteTemplate(UUID id) {
        TemplateDocument template = getTemplateById(id);
        templateRepository.delete(template);
    }
}
