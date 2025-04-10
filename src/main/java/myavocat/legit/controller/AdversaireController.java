package myavocat.legit.controller;

import myavocat.legit.dto.AdversaireDTO;
import myavocat.legit.response.ApiResponse;
import myavocat.legit.service.AdversaireService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/adversaires")
public class AdversaireController {

    @Autowired
    private AdversaireService adversaireService;

    @PostMapping
    public ApiResponse createAdversaire(@RequestBody AdversaireDTO adversaireDTO) {
        try {
            AdversaireDTO createdAdversaire = adversaireService.createAdversaire(adversaireDTO);
            return new ApiResponse(true, "Adversaire créé avec succès", createdAdversaire);
        } catch (Exception e) {
            return new ApiResponse(false, "Erreur lors de la création de l'adversaire: " + e.getMessage());
        }
    }

    @GetMapping("/{userId}")
    public ApiResponse getAllAdversaires(@PathVariable UUID userId) {
        try {
            List<AdversaireDTO> adversaires = adversaireService.getAllAdversaires(userId);
            return new ApiResponse(true, "Adversaires récupérés", adversaires);
        } catch (Exception e) {
            return new ApiResponse(false, "Erreur lors de la récupération des adversaires: " + e.getMessage());
        }
    }

    @GetMapping("/{userId}/{adversaireId}")
    public ApiResponse getAdversaireById(@PathVariable UUID userId, @PathVariable UUID adversaireId) {
        try {
            AdversaireDTO adversaire = adversaireService.getAdversaireById(adversaireId, userId);
            return new ApiResponse(true, "Adversaire trouvé", adversaire);
        } catch (Exception e) {
            return new ApiResponse(false, "Accès refusé ou adversaire non trouvé: " + e.getMessage());
        }
    }
    @GetMapping("/all/{officeId}")
    public ApiResponse getAllAdversairesByOffice(@PathVariable UUID officeId) {
        try {
            List<AdversaireDTO> adversaires = adversaireService.getAllAdversairesByOffice(officeId);
            return new ApiResponse(true, "Tous les adversaires du cabinet récupérés", adversaires);
        } catch (Exception e) {
            return new ApiResponse(false, "Erreur lors de la récupération des adversaires: " + e.getMessage());
        }
    }
    @DeleteMapping("/{userId}/{adversaireId}")
    public ApiResponse deleteAdversaire(@PathVariable UUID userId, @PathVariable UUID adversaireId) {
        try {
            adversaireService.deleteAdversaire(adversaireId, userId);
            return new ApiResponse(true, "Adversaire supprimé");
        } catch (Exception e) {
            return new ApiResponse(false, "Erreur lors de la suppression: " + e.getMessage());
        }
    }
}