package myavocat.legit.service;

import myavocat.legit.model.Dossier;
import myavocat.legit.model.Office;
import myavocat.legit.model.User;
import myavocat.legit.model.Client;
import myavocat.legit.model.Adversaire;
import myavocat.legit.repository.DossierRepository;
import myavocat.legit.repository.OfficeRepository;
import myavocat.legit.repository.UserRepository;
import myavocat.legit.repository.ClientRepository;
import myavocat.legit.repository.AdversaireRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class DossierService {

    @Autowired
    private DossierRepository dossierRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private OfficeRepository officeRepository;

    @Autowired
    private ClientRepository clientRepository;

    @Autowired
    private AdversaireRepository adversaireRepository;

    @Transactional
    public Dossier createDossier(Dossier dossier, UUID userId, UUID officeId, UUID clientId, UUID adversaireId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Utilisateur introuvable"));

        Office office = officeRepository.findById(officeId)
                .orElseThrow(() -> new RuntimeException("Cabinet introuvable"));

        // Vérification : l'utilisateur doit appartenir au cabinet
        if (!user.getOffice().getId().equals(officeId)) {
            throw new RuntimeException("Accès refusé : vous ne faites pas partie de ce cabinet.");
        }

        Client client = (clientId != null) ? clientRepository.findById(clientId)
                .orElseThrow(() -> new RuntimeException("Client introuvable")) : null;

        Adversaire adversaire = (adversaireId != null) ? adversaireRepository.findById(adversaireId)
                .orElseThrow(() -> new RuntimeException("Adversaire introuvable")) : null;

        dossier.setOffice(office);
        dossier.setAvocat(user); // L'avocat est l'utilisateur qui crée le dossier
        dossier.setClient(client);
        dossier.setAdversaire(adversaire);

        return dossierRepository.save(dossier);
    }

    @Transactional(readOnly = true)
    public List<Dossier> getAllDossiers(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Utilisateur introuvable"));

        UUID officeId = user.getOffice().getId(); // Un utilisateur appartient à **un seul** cabinet

        return dossierRepository.findAll().stream()
                .filter(dossier -> dossier.getOffice().getId().equals(officeId))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Dossier getDossierById(UUID dossierId, UUID userId) {
        Dossier dossier = dossierRepository.findById(dossierId)
                .orElseThrow(() -> new RuntimeException("Dossier non trouvé"));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Utilisateur introuvable"));

        if (!user.getOffice().getId().equals(dossier.getOffice().getId())) {
            throw new RuntimeException("Accès refusé : vous ne faites pas partie de ce cabinet.");
        }

        return dossier;
    }

    @Transactional
    public void deleteDossier(UUID dossierId, UUID userId) {
        Dossier dossier = dossierRepository.findById(dossierId)
                .orElseThrow(() -> new RuntimeException("Dossier non trouvé"));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Utilisateur introuvable"));

        if (!user.getOffice().getId().equals(dossier.getOffice().getId())) {
            throw new RuntimeException("Accès refusé : vous ne pouvez pas supprimer ce dossier.");
        }

        dossierRepository.deleteById(dossierId);
    }

    @Transactional
    public Dossier assignClientToDossier(UUID userId, UUID dossierId, UUID clientId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Utilisateur introuvable"));

        Dossier dossier = dossierRepository.findById(dossierId)
                .orElseThrow(() -> new RuntimeException("Dossier introuvable"));

        Client client = clientRepository.findById(clientId)
                .orElseThrow(() -> new RuntimeException("Client introuvable"));

        // Vérification : l'utilisateur doit faire partie du même cabinet que le dossier
        if (!user.getOffice().getId().equals(dossier.getOffice().getId())) {
            throw new RuntimeException("Accès refusé : vous ne pouvez pas modifier ce dossier.");
        }

        dossier.setClient(client); // ✅ Assigne le client au dossier
        return dossierRepository.save(dossier);
    }

    @Transactional
    public Dossier assignAdversaireToDossier(UUID userId, UUID dossierId, UUID adversaireId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Utilisateur introuvable"));

        Dossier dossier = dossierRepository.findById(dossierId)
                .orElseThrow(() -> new RuntimeException("Dossier introuvable"));

        Adversaire adversaire = adversaireRepository.findById(adversaireId)
                .orElseThrow(() -> new RuntimeException("Adversaire introuvable"));

        // Vérification : l'utilisateur doit faire partie du même cabinet que le dossier
        if (!user.getOffice().getId().equals(dossier.getOffice().getId())) {
            throw new RuntimeException("Accès refusé : vous ne pouvez pas modifier ce dossier.");
        }

        dossier.setAdversaire(adversaire); // ✅ Assigne l'adversaire au dossier
        return dossierRepository.save(dossier);
    }

    /**
     * Mettre à jour le statut d'un dossier
     */
    @Transactional
    public Dossier updateDossierStatut(UUID dossierId, String statut) {
        Dossier dossier = dossierRepository.findById(dossierId)
                .orElseThrow(() -> new RuntimeException("Dossier non trouvé avec l'ID: " + dossierId));

        dossier.setStatut(statut);
        return dossierRepository.save(dossier);
    }
}
