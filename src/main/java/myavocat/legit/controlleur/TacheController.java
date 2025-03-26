package myavocat.legit.controller;

import myavocat.legit.dto.TacheDTO;
import myavocat.legit.model.Tache;
import myavocat.legit.model.User;
import myavocat.legit.response.ApiResponse;
import myavocat.legit.service.TacheService;
import myavocat.legit.service.UserService;
import myavocat.legit.service.mapper.TacheMapperService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/taches")
@CrossOrigin(origins = "http://localhost:4200")
public class TacheController {

    @Autowired
    private TacheService tacheService;

    @Autowired
    private UserService userService;

    @Autowired
    private TacheMapperService tacheMapperService;

    /**
     * Créer une tâche uniquement si les deux utilisateurs (créateur et destinataire) sont dans le même office
     */
    @PostMapping("/create/{userId}/{officeId}")
    public ApiResponse createTache(@PathVariable UUID userId,
                                   @PathVariable UUID officeId,
                                   @RequestBody TacheDTO tacheDTO) {
        try {
            // La vérification d'appartenance à l'office est déjà faite ici, mais également par sécurité dans le service
            User creator = userService.getUserById(userId);
            if (creator == null || !creator.getOffice().getId().equals(officeId)) {
                return new ApiResponse(false, "Vous n'êtes pas autorisé à créer une tâche dans cet office.", null);
            }

            // Conversion du DTO vers l'entité
            Tache tache = tacheMapperService.convertFromDTO(tacheDTO);

            // Vérification de l'utilisateur assigné si spécifié
            if (tache.getUser() != null) {
                User assignedUser = userService.getUserById(tache.getUser().getId());
                if (assignedUser == null || !assignedUser.getOffice().getId().equals(officeId)) {
                    return new ApiResponse(false, "L'utilisateur assigné n'appartient pas à cet office.", null);
                }
            }

            // Appel de la méthode correcte sécurisée
            Tache createdTache = tacheService.createTacheWithOfficeCheck(userId, officeId, tache);

            // Conversion de l'entité vers DTO pour la réponse
            TacheDTO responseDTO = tacheMapperService.convertToDTO(createdTache);

            return new ApiResponse(true, "Tâche créée avec succès.", responseDTO);

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
                    .map(tacheMapperService::convertToDTO)
                    .collect(Collectors.toList());

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

    @GetMapping("/{userId}/{officeId}/{tacheId}")
    public ApiResponse getTacheById(@PathVariable UUID userId,
                                    @PathVariable UUID officeId,
                                    @PathVariable Long tacheId) {
        try {
            User user = userService.getUserById(userId);
            if (user == null || !user.getOffice().getId().equals(officeId)) {
                return new ApiResponse(false, "Utilisateur non autorisé.", null);
            }

            Tache tache = tacheService.findById(tacheId)
                    .orElseThrow(() -> new RuntimeException("Tâche introuvable."));

            // Vérifier si l'utilisateur appartient bien au même office que la tâche
            if (tache.getUser() != null && !tache.getUser().getOffice().getId().equals(officeId)) {
                return new ApiResponse(false, "Vous n'avez pas accès à cette tâche.", null);
            }

            return new ApiResponse(true, "Tâche trouvée.", tacheMapperService.convertToDTO(tache));

        } catch (Exception e) {
            return new ApiResponse(false, "Erreur lors de la récupération de la tâche : " + e.getMessage(), null);
        }
    }

    @PutMapping("/{userId}/{officeId}/{tacheId}")
    public ApiResponse updateTache(@PathVariable UUID userId,
                                   @PathVariable UUID officeId,
                                   @PathVariable Long tacheId,
                                   @RequestBody TacheDTO tacheDTO) {
        try {
            User user = userService.getUserById(userId);
            if (user == null || !user.getOffice().getId().equals(officeId)) {
                return new ApiResponse(false, "Utilisateur non autorisé.", null);
            }

            Tache tache = tacheService.findById(tacheId)
                    .orElseThrow(() -> new RuntimeException("Tâche introuvable."));

            if (tache.getUser() != null && !tache.getUser().getOffice().getId().equals(officeId)) {
                return new ApiResponse(false, "Vous n'êtes pas autorisé à modifier cette tâche.", null);
            }

            // Mise à jour des champs à partir du DTO
            tache = tacheMapperService.updateEntityFromDTO(tache, tacheDTO);

            // Si l'utilisateur a été modifié, vérifier qu'il appartient bien au même office
            if (tache.getUser() != null && !tache.getUser().getOffice().getId().equals(officeId)) {
                return new ApiResponse(false, "L'utilisateur assigné n'appartient pas à cet office.", null);
            }

            Tache saved = tacheService.save(tache);

            return new ApiResponse(true, "Tâche mise à jour avec succès.", tacheMapperService.convertToDTO(saved));

        } catch (RuntimeException e) {
            return new ApiResponse(false, e.getMessage(), null);
        } catch (Exception e) {
            return new ApiResponse(false, "Erreur lors de la mise à jour : " + e.getMessage(), null);
        }
    }
}