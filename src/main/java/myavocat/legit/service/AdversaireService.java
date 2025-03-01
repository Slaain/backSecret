package myavocat.legit.service;

import myavocat.legit.dto.AdversaireDTO;
import myavocat.legit.model.Adversaire;
import myavocat.legit.model.Dossier;
import myavocat.legit.repository.AdversaireRepository;
import myavocat.legit.repository.DossierRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class AdversaireService {

    @Autowired
    private AdversaireRepository adversaireRepository;

    @Autowired
    private DossierRepository dossierRepository;

    @Transactional
    public AdversaireDTO createAdversaire(AdversaireDTO adversaireDTO) {
        Dossier dossier = dossierRepository.findById(adversaireDTO.getDossierId())
                .orElseThrow(() -> new RuntimeException("Dossier introuvable"));

        Adversaire adversaire = new Adversaire();
        adversaire.setNom(adversaireDTO.getNom());
        adversaire.setPrenom(adversaireDTO.getPrenom());
        adversaire.setEmail(adversaireDTO.getEmail());
        adversaire.setTelephone(adversaireDTO.getTelephone());
        adversaire.setDossier(dossier);

        Adversaire savedAdversaire = adversaireRepository.save(adversaire);
        return convertToDTO(savedAdversaire);
    }

    @Transactional(readOnly = true)
    public List<AdversaireDTO> getAdversairesByDossier(UUID dossierId) {
        return adversaireRepository.findByDossier_Id(dossierId)
                .stream().map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public AdversaireDTO getAdversaireById(UUID id) {
        Adversaire adversaire = adversaireRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Adversaire non trouvé"));
        return convertToDTO(adversaire);
    }

    @Transactional
    public void deleteAdversaire(UUID id) {
        adversaireRepository.deleteById(id);
    }

    private AdversaireDTO convertToDTO(Adversaire adversaire) {
        AdversaireDTO dto = new AdversaireDTO();
        dto.setId(adversaire.getId());
        dto.setNom(adversaire.getNom());
        dto.setPrenom(adversaire.getPrenom());
        dto.setEmail(adversaire.getEmail());
        dto.setTelephone(adversaire.getTelephone());
        dto.setDossierId(adversaire.getDossier().getId());
        return dto;
    }
}
