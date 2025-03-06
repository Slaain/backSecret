package myavocat.legit.service;

import myavocat.legit.dto.AdversaireDTO;
import myavocat.legit.model.Adversaire;
import myavocat.legit.model.Dossier;
import myavocat.legit.model.Office;
import myavocat.legit.model.User;
import myavocat.legit.repository.AdversaireRepository;
import myavocat.legit.repository.DossierRepository;
import myavocat.legit.repository.OfficeRepository;
import myavocat.legit.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.Set;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class AdversaireService {

    @Autowired
    private AdversaireRepository adversaireRepository;

    @Autowired
    private DossierRepository dossierRepository;

    @Autowired
    private OfficeRepository officeRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Transactional
    public AdversaireDTO createAdversaire(AdversaireDTO adversaireDTO) {
        Adversaire adversaire = new Adversaire();
        adversaire.setNom(adversaireDTO.getNom());
        adversaire.setPrenom(adversaireDTO.getPrenom());
        adversaire.setEmail(adversaireDTO.getEmail());
        adversaire.setTelephone(adversaireDTO.getTelephone());

        // Ces champs sont optionnels, on vérifie s'ils existent
        if (adversaireDTO.getType() != null) {
            adversaire.setType(adversaireDTO.getType());
        }
        if (adversaireDTO.getQualite() != null) {
            adversaire.setQualite(adversaireDTO.getQualite());
        }
        if (adversaireDTO.getCommune() != null) {
            adversaire.setCommune(adversaireDTO.getCommune());
        }

        // Si un officeId est fourni, on associe l'office
        if (adversaireDTO.getOfficeId() != null) {
            Office office = officeRepository.findById(adversaireDTO.getOfficeId())
                    .orElseThrow(() -> new RuntimeException("Cabinet introuvable"));
            adversaire.setOffice(office);
        }

        // Si un dossierId est fourni, on associe le dossier
        if (adversaireDTO.getDossierId() != null) {
            Dossier dossier = dossierRepository.findById(adversaireDTO.getDossierId())
                    .orElseThrow(() -> new RuntimeException("Dossier introuvable"));
            HashSet<Dossier> dossiers = new HashSet<>();
            dossiers.add(dossier);
            adversaire.setDossiers(dossiers);
        }

        Adversaire savedAdversaire = adversaireRepository.save(adversaire);
        return convertToDTO(savedAdversaire);
    }

    @Transactional(readOnly = true)
    public List<AdversaireDTO> getAllAdversaires(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Utilisateur introuvable"));

        UUID officeId = user.getOffice().getId();

        // Récupérer les adversaires ayant au moins un dossier dans le cabinet de l'utilisateur
        List<Dossier> dossiers = dossierRepository.findAll().stream()
                .filter(dossier -> dossier.getOffice().getId().equals(officeId))
                .collect(Collectors.toList());

        System.out.println("Nombre de dossiers trouvés pour l'office " + officeId + ": " + dossiers.size());

        List<Adversaire> adversaires = dossiers.stream()
                .flatMap(dossier -> adversaireRepository.findByDossiersId(dossier.getId()).stream())
                .filter(adversaire -> adversaire != null)
                .distinct()
                .collect(Collectors.toList());

        System.out.println("Nombre d'adversaires uniques et non-null: " + adversaires.size());

        return adversaires.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public AdversaireDTO getAdversaireById(UUID adversaireId, UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Utilisateur introuvable"));

        Adversaire adversaire = adversaireRepository.findById(adversaireId)
                .orElseThrow(() -> new RuntimeException("Adversaire non trouvé"));

        // Vérifier que cet adversaire est lié à un dossier du même cabinet que l'utilisateur
        boolean adversaireAccessible = adversaire.getDossiers().stream()
                .anyMatch(dossier -> dossier.getOffice().getId().equals(user.getOffice().getId()));

        if (!adversaireAccessible) {
            throw new RuntimeException("Accès refusé : cet adversaire appartient à un autre cabinet.");
        }

        return convertToDTO(adversaire);
    }
    @Transactional(readOnly = true)
    public List<AdversaireDTO> findAll() {
        // Récupérer tous les adversaires sans filtrage
        List<Adversaire> adversaires = adversaireRepository.findAll();
        return adversaires.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }
    @Transactional
    public void deleteAdversaire(UUID adversaireId, UUID userId) {
        // Vérifier que l'utilisateur et l'adversaire existent
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Utilisateur introuvable"));

        Adversaire adversaire = adversaireRepository.findById(adversaireId)
                .orElseThrow(() -> new RuntimeException("Adversaire non trouvé"));

        // Mettre à NULL toutes les références à cet adversaire dans la table dossiers
        String sql = "UPDATE dossiers SET adversaire_id = NULL WHERE adversaire_id = ?";
        jdbcTemplate.update(sql, adversaireId.toString());

        // Maintenant on peut supprimer l'adversaire en toute sécurité
        adversaireRepository.deleteById(adversaireId);
    }

    /**
     * Dissocier tous les adversaires d'un dossier
     */
    @Transactional
    public void dissociateAdversairesFromDossier(UUID dossierId) {
        if (dossierId == null) {
            throw new RuntimeException("L'ID du dossier est requis");
        }

        String sql = "DELETE FROM adversaire_dossier WHERE dossier_id = ?";
        jdbcTemplate.update(sql, dossierId.toString());
    }

    @Transactional(readOnly = true)
    public List<AdversaireDTO> getAdversairesByDossier(UUID dossierId) {
        if (dossierId == null) {
            throw new RuntimeException("L'ID du dossier est requis pour récupérer les adversaires");
        }

        // Vérifier que le dossier existe
        if (!dossierRepository.existsById(dossierId)) {
            throw new RuntimeException("Dossier introuvable");
        }

        return adversaireRepository.findByDossiersId(dossierId)
                .stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    private AdversaireDTO convertToDTO(Adversaire adversaire) {
        AdversaireDTO dto = new AdversaireDTO();
        dto.setId(adversaire.getId());
        dto.setNom(adversaire.getNom());
        dto.setPrenom(adversaire.getPrenom());
        dto.setEmail(adversaire.getEmail());
        dto.setTelephone(adversaire.getTelephone());
        dto.setType(adversaire.getType());
        dto.setQualite(adversaire.getQualite());
        dto.setCommune(adversaire.getCommune());

        if (adversaire.getOffice() != null) {
            dto.setOfficeId(adversaire.getOffice().getId());
        }

        // Si l'adversaire a des dossiers, on récupère l'ID du premier pour la compatibilité
        if (adversaire.getDossiers() != null && !adversaire.getDossiers().isEmpty()) {
            dto.setDossierId(adversaire.getDossiers().iterator().next().getId());
        }

        return dto;
    }
}