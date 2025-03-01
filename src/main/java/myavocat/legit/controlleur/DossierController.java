package myavocat.legit.controller;

import myavocat.legit.model.Dossier;
import myavocat.legit.response.ApiResponse;
import myavocat.legit.service.DossierService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/dossiers")
public class DossierController {

    @Autowired
    private DossierService dossierService;

    /**
     * Créer un nouveau dossier dans un cabinet donné
     */
    @PostMapping("/create/{userId}/{officeId}")
    public ApiResponse createDossier(@PathVariable UUID userId,
                                     @PathVariable UUID officeId,
                                     @RequestBody Dossier dossier,
                                     @RequestParam(required = false) UUID clientId,
                                     @RequestParam(required = false) UUID adversaireId) {
        try {
            Dossier createdDossier = dossierService.createDossier(dossier, userId, officeId, clientId, adversaireId);
            return new ApiResponse(true, "Dossier créé avec succès", createdDossier);
        } catch (RuntimeException e) {
            return new ApiResponse(false, "Erreur lors de la création du dossier: " + e.getMessage(), null);
        } catch (Exception e) {
            return new ApiResponse(false, "Erreur inconnue: " + e.getMessage(), null);
        }
    }

    /**
     * Récupérer tous les dossiers accessibles par un utilisateur
     */
    @GetMapping("/{userId}")
    public ApiResponse getAllDossiers(@PathVariable UUID userId) {
        try {
            List<Dossier> dossiers = dossierService.getAllDossiers(userId);
            return new ApiResponse(true, "Dossiers récupérés", dossiers);
        } catch (Exception e) {
            return new ApiResponse(false, "Erreur lors de la récupération des dossiers: " + e.getMessage(), null);
        }
    }

    /**
     * Récupérer un dossier spécifique, si l'utilisateur y a accès
     */
    @GetMapping("/{userId}/{dossierId}")
    public ApiResponse getDossierById(@PathVariable UUID userId, @PathVariable UUID dossierId) {
        try {
            Dossier dossier = dossierService.getDossierById(dossierId, userId);
            return new ApiResponse(true, "Dossier trouvé", dossier);
        } catch (RuntimeException e) {
            return new ApiResponse(false, e.getMessage(), null);
        } catch (Exception e) {
            return new ApiResponse(false, "Erreur inattendue: " + e.getMessage(), null);
        }
    }

    /**
     * Supprimer un dossier (seulement si l'utilisateur y est autorisé)
     */
    @DeleteMapping("/{userId}/{dossierId}")
    public ApiResponse deleteDossier(@PathVariable UUID userId, @PathVariable UUID dossierId) {
        try {
            dossierService.deleteDossier(dossierId, userId);
            return new ApiResponse(true, "Dossier supprimé");
        } catch (RuntimeException e) {
            return new ApiResponse(false, "Erreur lors de la suppression du dossier: " + e.getMessage(), null);
        } catch (Exception e) {
            return new ApiResponse(false, "Erreur inconnue: " + e.getMessage(), null);
        }
    }

    @PatchMapping("/{userId}/{dossierId}/assign-client/{clientId}")
    public ApiResponse assignClientToDossier(
            @PathVariable UUID userId,
            @PathVariable UUID dossierId,
            @PathVariable UUID clientId) {
        try {
            Dossier updatedDossier = dossierService.assignClientToDossier(userId, dossierId, clientId);
            return new ApiResponse(true, "Client assigné au dossier avec succès", updatedDossier);
        } catch (RuntimeException e) {
            return new ApiResponse(false, e.getMessage(), null);
        }
    }

}
