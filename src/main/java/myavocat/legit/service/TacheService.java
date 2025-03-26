package myavocat.legit.service;

import myavocat.legit.model.Tache;
import myavocat.legit.model.User;
import myavocat.legit.repository.TacheRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class TacheService {

    private final TacheRepository tacheRepository;
    private final UserService userService;

    @Autowired
    public TacheService(TacheRepository tacheRepository, UserService userService) {
        this.tacheRepository = tacheRepository;
        this.userService = userService;
    }

    // Créer une tâche après vérification que les deux utilisateurs sont dans le même office
    public Tache createTacheWithOfficeCheck(UUID creatorId, UUID officeId, Tache tache) {
        User creator = userService.getUserById(creatorId);

        if (!creator.getOffice().getId().equals(officeId)) {
            throw new RuntimeException("Créateur n'appartient pas à l'office spécifié.");
        }

        if (tache.getUser() != null) {
            User assignedUser = userService.getUserById(tache.getUser().getId());
            if (!assignedUser.getOffice().getId().equals(officeId)) {
                throw new RuntimeException("L'utilisateur assigné n'appartient pas à l'office spécifié.");
            }
        }

        return tacheRepository.save(tache);
    }

    // Vérifier si une tâche appartient bien à un office précis
    public boolean isTacheInOffice(Long tacheId, UUID officeId) {
        Optional<Tache> optionalTache = tacheRepository.findById(tacheId);

        if (optionalTache.isEmpty()) {
            throw new RuntimeException("Tâche non trouvée.");
        }

        Tache tache = optionalTache.get();

        if (tache.getUser() == null) {
            throw new RuntimeException("La tâche n'est pas assignée à un utilisateur.");
        }

        return tache.getUser().getOffice().getId().equals(officeId);
    }

    // Autres méthodes existantes (aucun changement nécessaire)
    public Optional<Tache> findById(Long id) {
        return tacheRepository.findById(id);
    }

    public List<Tache> findAll() {
        return tacheRepository.findAll();
    }

    public void deleteById(Long id) {
        tacheRepository.deleteById(id);
    }

    public List<Tache> findByDossierId(UUID dossierId) {
        return tacheRepository.findByDossierId(dossierId);
    }

    public List<Tache> findByUserId(UUID userId) {
        return tacheRepository.findByUserId(userId);
    }
}
