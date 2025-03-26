package myavocat.legit.controller;

import myavocat.legit.dto.TacheDTO;
import myavocat.legit.model.Tache;
import myavocat.legit.model.User;
import myavocat.legit.response.ApiResponse;
import myavocat.legit.service.TacheService;
import myavocat.legit.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/taches")
@CrossOrigin(origins = "http://localhost:4200")
public class TacheController {

    @Autowired
    private TacheService tacheService;

    @Autowired
    private UserService userService;

    /**
     * Créer une tâche uniquement si les deux utilisateurs (créateur et destinataire) sont dans le même office
     */
    @PostMapping("/create/{userId}/{officeId}")
    public ApiResponse createTache(@PathVariable UUID userId,
                                   @PathVariable UUID officeId,
                                   @RequestBody Tache tache) {
        try {
            // La vérification d'appartenance à l'office est déjà faite ici, mais également par sécurité dans le service
            User creator = userService.getUserById(userId);
            if (creator == null || !creator.getOffice().getId().equals(officeId)) {
                return new ApiResponse(false, "Vous n'êtes pas autorisé à créer une tâche dans cet office.", null);
            }

            // Vérification de l'utilisateur assigné si spécifié
            if (tache.getUser() != null) {
                User assignedUser = userService.getUserById(tache.getUser().getId());
                if (assignedUser == null || !assignedUser.getOffice().getId().equals(officeId)) {
                    return new ApiResponse(false, "L'utilisateur assigné n'appartient pas à cet office.", null);
                }
            }

            // Appel de la méthode correcte sécurisée
            Tache createdTache = tacheService.createTacheWithOfficeCheck(userId, officeId, tache);
            return new ApiResponse(true, "Tâche créée avec succès.", convertToDTO(createdTache));


        } catch (RuntimeException e) {
            return new ApiResponse(false, "Erreur : " + e.getMessage(), null);
        } catch (Exception e) {
            return new ApiResponse(false, "Erreur inconnue : " + e.getMessage(), null);
        }
    }


    /**
     * Récupérer toutes les tâches d'un utilisateur si il appartient à l'office spécifié
     */
    @GetMapping("/{userId}/{officeId}")
    public ApiResponse getTachesByUser(@PathVariable UUID userId, @PathVariable UUID officeId) {
        try {
            User user = userService.getUserById(userId);
            if (user == null || !user.getOffice().getId().equals(officeId)) {
                return new ApiResponse(false, "Utilisateur non autorisé.", null);
            }

           List<Tache> taches = tacheService.findByUserId(userId);
            List<TacheDTO> dtoList = taches.stream()
                    .map(this::convertToDTO)
                    .toList();
            return new ApiResponse(true, "Tâches récupérées avec succès.", dtoList);


        } catch (Exception e) {
            return new ApiResponse(false, "Erreur lors de la récupération : " + e.getMessage(), null);
        }
    }

    /**
     * Supprimer une tâche uniquement si elle appartient au même office que l'utilisateur qui effectue l'action
     */
    @DeleteMapping("/{userId}/{officeId}/{tacheId}")
    public ApiResponse deleteTache(@PathVariable UUID userId,
                                   @PathVariable UUID officeId,
                                   @PathVariable Long tacheId) {
        try {
            User user = userService.getUserById(userId);
            if (user == null || !user.getOffice().getId().equals(officeId)) {
                return new ApiResponse(false, "Action non autorisée.", null);
            }

            Tache tache = tacheService.findById(tacheId).orElseThrow(() -> new RuntimeException("Tâche introuvable."));

            if (tache.getUser() != null && !tache.getUser().getOffice().getId().equals(officeId)) {
                return new ApiResponse(false, "Vous n'êtes pas autorisé à supprimer cette tâche.", null);
            }

            tacheService.deleteById(tacheId);
            return new ApiResponse(true, "Tâche supprimée avec succès.");

        } catch (RuntimeException e) {
            return new ApiResponse(false, e.getMessage(), null);
        } catch (Exception e) {
            return new ApiResponse(false, "Erreur inconnue : " + e.getMessage(), null);
        }
    }

    private TacheDTO convertToDTO(Tache tache) {
        TacheDTO dto = new TacheDTO();
        dto.setId(tache.getId());
        dto.setTitre(tache.getTitre());
        dto.setDescription(tache.getDescription());
        dto.setStatut(tache.getStatut().name());
        dto.setDateEcheance(tache.getDateEcheance());
        dto.setCreatedAt(tache.getCreatedAt());
        dto.setUpdatedAt(tache.getUpdatedAt());

        if (tache.getDossier() != null) {
            TacheDTO.DossierSimpleDTO dossierDTO = new TacheDTO.DossierSimpleDTO();
            dossierDTO.setId(tache.getDossier().getId().toString());
            dossierDTO.setReference(tache.getDossier().getReference());
            dossierDTO.setNomDossier(tache.getDossier().getNomDossier());
            dto.setDossier(dossierDTO);
        }

        if (tache.getUser() != null) {
            TacheDTO.UserSimpleDTO userDTO = new TacheDTO.UserSimpleDTO();
            userDTO.setId(tache.getUser().getId().toString());
            userDTO.setNom(tache.getUser().getNom());
            userDTO.setPrenom(tache.getUser().getPrenom());
            userDTO.setEmail(tache.getUser().getEmail());
            dto.setUser(userDTO);
        }

        return dto;
    }

}
