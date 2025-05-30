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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

        dossier.setAdversaire(adversaire);  // ✅ Assigne l'objet Adversaire
        dossier.setAdversaireId(adversaire.getId()); // ✅ Met à jour adversaireId

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
    @Transactional
    public Dossier assignAvocatToDossier(UUID dossierId, UUID userId) {
        Dossier dossier = dossierRepository.findById(dossierId)
                .orElseThrow(() -> new RuntimeException("Dossier introuvable avec l'ID : " + dossierId));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Utilisateur introuvable avec l'ID : " + userId));

        // Vérification : l'utilisateur doit faire partie du même cabinet que le dossier
        if (!user.getOffice().getId().equals(dossier.getOffice().getId())) {
            throw new RuntimeException("Accès refusé : vous ne pouvez pas modifier ce dossier.");
        }

        // Assigner l'avocat au dossier
        dossier.setAvocat(user);

        // Sauvegarder et retourner le dossier mis à jour
        return dossierRepository.save(dossier);
    }

    @Transactional(readOnly = true)
    public List<Client> getClientsByOffice(UUID officeId) {
        return clientRepository.findByOfficeId(officeId);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getKpiDossiers(UUID userId) {
        UUID officeId = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Utilisateur introuvable"))
                .getOffice().getId();

        List<Dossier> allDossiers = dossierRepository.findAll().stream()
                .filter(dossier -> dossier.getOffice().getId().equals(officeId))
                .collect(Collectors.toList());

        long totalDossiers = allDossiers.size();
        long dossiersEnCours = allDossiers.stream().filter(d -> "En cours".equals(d.getStatut())).count();
        long dossiersEnAttente = allDossiers.stream().filter(d -> "En attente".equals(d.getStatut())).count();
        long dossiersTermines = allDossiers.stream().filter(d -> "Fini".equals(d.getStatut())).count();

        // **Filtres temporels**
        LocalDateTime debutJour = LocalDate.now().atStartOfDay();
        LocalDateTime debutSemaine = LocalDate.now().with(java.time.DayOfWeek.MONDAY).atStartOfDay();
        LocalDateTime debutMois = LocalDate.now().with(TemporalAdjusters.firstDayOfMonth()).atStartOfDay();
        LocalDateTime debutAnnee = LocalDate.now().with(TemporalAdjusters.firstDayOfYear()).atStartOfDay();

        long dossiersAujourdHui = allDossiers.stream().filter(d -> d.getCreatedAt().isAfter(debutJour)).count();
        long dossiersCetteSemaine = allDossiers.stream().filter(d -> d.getCreatedAt().isAfter(debutSemaine)).count();
        long dossiersCeMois = allDossiers.stream().filter(d -> d.getCreatedAt().isAfter(debutMois)).count();
        long dossiersCetteAnnee = allDossiers.stream().filter(d -> d.getCreatedAt().isAfter(debutAnnee)).count();

        // **5 derniers dossiers créés**
        List<Map<String, Object>> derniersDossiers = allDossiers.stream()
                .sorted((d1, d2) -> d2.getCreatedAt().compareTo(d1.getCreatedAt()))
                .limit(5)
                .map(dossier -> {
                    Map<String, Object> dossierMap = new HashMap<>();
                    dossierMap.put("id", dossier.getId());
                    dossierMap.put("reference", dossier.getReference());
                    dossierMap.put("nomDossier", dossier.getNomDossier());
                    dossierMap.put("typeAffaire", dossier.getTypeAffaire());
                    dossierMap.put("statut", dossier.getStatut());
                    dossierMap.put("createdAt", dossier.getCreatedAt());
                    return dossierMap;
                })
                .collect(Collectors.toList());

        // **Construction de la réponse**
        Map<String, Object> result = new HashMap<>();
        result.put("totalDossiers", totalDossiers);
        result.put("dossiersEnCours", dossiersEnCours);
        result.put("dossiersEnAttente", dossiersEnAttente);
        result.put("dossiersTermines", dossiersTermines);
        result.put("dossiersAujourdHui", dossiersAujourdHui);
        result.put("dossiersCetteSemaine", dossiersCetteSemaine);
        result.put("dossiersCeMois", dossiersCeMois);
        result.put("dossiersCetteAnnee", dossiersCetteAnnee);
        result.put("derniersDossiers", derniersDossiers);

        return result;
    }
}


