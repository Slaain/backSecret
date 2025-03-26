package myavocat.legit.service.mapper;

import myavocat.legit.dto.TacheDTO;
import myavocat.legit.model.Dossier;
import myavocat.legit.model.StatutTache;
import myavocat.legit.model.Tache;
import myavocat.legit.model.User;
import myavocat.legit.repository.DossierRepository;
import myavocat.legit.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Service dédié à la conversion entre les entités Tache et les DTOs TacheDTO
 */
@Service
public class TacheMapperService {

    private final UserService userService;
    private final DossierRepository dossierRepository;

    @Autowired
    public TacheMapperService(UserService userService, DossierRepository dossierRepository) {
        this.userService = userService;
        this.dossierRepository = dossierRepository;
    }

    /**
     * Convertit un DTO en entité Tache
     * @param dto Le DTO à convertir
     * @return Une nouvelle instance de Tache avec les données du DTO
     */
    public Tache convertFromDTO(TacheDTO dto) {
        Tache tache = new Tache();

        // Définir les propriétés simples
        tache.setTitre(dto.getTitre());
        tache.setDescription(dto.getDescription());
        tache.setDateEcheance(dto.getDateEcheance());

        // Conversion du statut
        if (dto.getStatut() != null) {
            try {
                tache.setStatut(StatutTache.valueOf(dto.getStatut()));
            } catch (IllegalArgumentException e) {
                throw new RuntimeException("Statut de tâche invalide: " + dto.getStatut());
            }
        }

        // Récupération et assignation de l'utilisateur
        if (dto.getUserId() != null && !dto.getUserId().isEmpty()) {
            try {
                User user = userService.getUserById(UUID.fromString(dto.getUserId()));
                if (user == null) {
                    throw new RuntimeException("Utilisateur non trouvé avec l'ID: " + dto.getUserId());
                }
                tache.setUser(user);
            } catch (IllegalArgumentException e) {
                throw new RuntimeException("ID d'utilisateur invalide: " + dto.getUserId());
            }
        }

        // Récupération et assignation du dossier
        if (dto.getDossierId() != null && !dto.getDossierId().isEmpty()) {
            try {
                Dossier dossier = dossierRepository.findById(UUID.fromString(dto.getDossierId()))
                        .orElseThrow(() -> new RuntimeException("Dossier non trouvé avec l'ID: " + dto.getDossierId()));
                tache.setDossier(dossier);
            } catch (IllegalArgumentException e) {
                throw new RuntimeException("ID de dossier invalide: " + dto.getDossierId());
            }
        }

        return tache;
    }

    /**
     * Convertit une entité Tache en DTO
     * @param tache L'entité à convertir
     * @return Un nouveau TacheDTO avec les données de l'entité
     */
    public TacheDTO convertToDTO(Tache tache) {
        TacheDTO dto = new TacheDTO();

        // Définir les propriétés simples
        dto.setId(tache.getId());
        dto.setTitre(tache.getTitre());
        dto.setDescription(tache.getDescription());
        if (tache.getStatut() != null) {
            dto.setStatut(tache.getStatut().name());
        }
        dto.setDateEcheance(tache.getDateEcheance());
        dto.setCreatedAt(tache.getCreatedAt());
        dto.setUpdatedAt(tache.getUpdatedAt());

        // Gestion du dossier associé
        if (tache.getDossier() != null) {
            TacheDTO.DossierSimpleDTO dossierDTO = new TacheDTO.DossierSimpleDTO();
            dossierDTO.setId(tache.getDossier().getId().toString());
            dossierDTO.setReference(tache.getDossier().getReference());
            dossierDTO.setNomDossier(tache.getDossier().getNomDossier());
            dto.setDossier(dossierDTO);

            // Sauvegarder l'ID pour la mise à jour
            dto.setDossierId(tache.getDossier().getId().toString());
        }

        // Gestion de l'utilisateur associé
        if (tache.getUser() != null) {
            TacheDTO.UserSimpleDTO userDTO = new TacheDTO.UserSimpleDTO();
            userDTO.setId(tache.getUser().getId().toString());
            userDTO.setNom(tache.getUser().getNom());
            userDTO.setPrenom(tache.getUser().getPrenom());
            userDTO.setEmail(tache.getUser().getEmail());
            dto.setUser(userDTO);

            // Sauvegarder l'ID pour la mise à jour
            dto.setUserId(tache.getUser().getId().toString());
        }

        return dto;
    }

    /**
     * Met à jour une entité Tache existante avec les données d'un DTO
     * @param existingTache L'entité Tache à mettre à jour
     * @param dto Le DTO contenant les nouvelles données
     * @return L'entité Tache mise à jour
     */
    public Tache updateEntityFromDTO(Tache existingTache, TacheDTO dto) {
        // Mettre à jour les propriétés simples si elles ne sont pas null dans le DTO
        if (dto.getTitre() != null) {
            existingTache.setTitre(dto.getTitre());
        }

        if (dto.getDescription() != null) {
            existingTache.setDescription(dto.getDescription());
        }

        if (dto.getDateEcheance() != null) {
            existingTache.setDateEcheance(dto.getDateEcheance());
        }

        // Mise à jour du statut
        if (dto.getStatut() != null) {
            try {
                existingTache.setStatut(StatutTache.valueOf(dto.getStatut()));
            } catch (IllegalArgumentException e) {
                throw new RuntimeException("Statut de tâche invalide: " + dto.getStatut());
            }
        }

        // Mise à jour de l'utilisateur assigné
        if (dto.getUserId() != null) {
            if (dto.getUserId().isEmpty()) {
                // Si l'ID est vide, on retire l'association
                existingTache.setUser(null);
            } else {
                try {
                    User user = userService.getUserById(UUID.fromString(dto.getUserId()));
                    if (user == null) {
                        throw new RuntimeException("Utilisateur non trouvé avec l'ID: " + dto.getUserId());
                    }
                    existingTache.setUser(user);
                } catch (IllegalArgumentException e) {
                    throw new RuntimeException("ID d'utilisateur invalide: " + dto.getUserId());
                }
            }
        }

        // Mise à jour du dossier associé
        if (dto.getDossierId() != null) {
            if (dto.getDossierId().isEmpty()) {
                // Si l'ID est vide, on retire l'association
                existingTache.setDossier(null);
            } else {
                try {
                    Dossier dossier = dossierRepository.findById(UUID.fromString(dto.getDossierId()))
                            .orElseThrow(() -> new RuntimeException("Dossier non trouvé avec l'ID: " + dto.getDossierId()));
                    existingTache.setDossier(dossier);
                } catch (IllegalArgumentException e) {
                    throw new RuntimeException("ID de dossier invalide: " + dto.getDossierId());
                }
            }
        }

        return existingTache;
    }
}