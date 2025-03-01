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

    @GetMapping("/dossier/{dossierId}")
    public ApiResponse getAdversairesByDossier(@PathVariable UUID dossierId) {
        List<AdversaireDTO> adversaires = adversaireService.getAdversairesByDossier(dossierId);
        return new ApiResponse(true, "Adversaires récupérés", adversaires);
    }

    @GetMapping("/{id}")
    public ApiResponse getAdversaireById(@PathVariable UUID id) {
        try {
            AdversaireDTO adversaire = adversaireService.getAdversaireById(id);
            return new ApiResponse(true, "Adversaire trouvé", adversaire);
        } catch (Exception e) {
            return new ApiResponse(false, "Adversaire non trouvé: " + e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public ApiResponse deleteAdversaire(@PathVariable UUID id) {
        try {
            adversaireService.deleteAdversaire(id);
            return new ApiResponse(true, "Adversaire supprimé");
        } catch (Exception e) {
            return new ApiResponse(false, "Erreur lors de la suppression: " + e.getMessage());
        }
    }
}
