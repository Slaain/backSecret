package myavocat.legit.controller;

import myavocat.legit.dto.PaiementDTO;
import myavocat.legit.dto.CreatePaiementDTO;
import myavocat.legit.response.ApiResponse;
import myavocat.legit.service.PaiementService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/paiements/{userId}")
public class PaiementController {

    @Autowired
    private PaiementService paiementService;

    // 🔥 CRÉATION ET MODIFICATION DE PAIEMENTS

    /**
     * Créer un nouveau paiement pour une facture
     * POST /api/paiements/{userId}
     */
    @PostMapping
    public ResponseEntity<ApiResponse> creerPaiement(
            @PathVariable UUID userId,
            @Valid @RequestBody CreatePaiementDTO createPaiementDTO) {
        try {
            PaiementDTO paiement = paiementService.creerPaiement(userId, createPaiementDTO);
            return ResponseEntity.ok(new ApiResponse(
                    true,
                    "Paiement créé avec succès. Statut de la facture mis à jour automatiquement.",
                    paiement
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(new ApiResponse(false, "Erreur lors de la création du paiement : " + e.getMessage(), null));
        }
    }

    /**
     * Modifier un paiement existant
     * PUT /api/paiements/{userId}/{paiementId}
     */
    @PutMapping("/{paiementId}")
    public ResponseEntity<ApiResponse> modifierPaiement(
            @PathVariable UUID userId,
            @PathVariable UUID paiementId,
            @Valid @RequestBody CreatePaiementDTO updateData) {
        try {
            PaiementDTO paiement = paiementService.modifierPaiement(userId, paiementId, updateData);
            return ResponseEntity.ok(new ApiResponse(
                    true,
                    "Paiement modifié avec succès. Statut de la facture mis à jour automatiquement.",
                    paiement
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(new ApiResponse(false, "Erreur lors de la modification du paiement : " + e.getMessage(), null));
        }
    }

    /**
     * Supprimer un paiement
     * DELETE /api/paiements/{userId}/{paiementId}
     */
    @DeleteMapping("/{paiementId}")
    public ResponseEntity<ApiResponse> supprimerPaiement(
            @PathVariable UUID userId,
            @PathVariable UUID paiementId) {
        try {
            paiementService.supprimerPaiement(userId, paiementId);
            return ResponseEntity.ok(new ApiResponse(
                    true,
                    "Paiement supprimé avec succès. Statut de la facture mis à jour automatiquement.",
                    null
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(new ApiResponse(false, "Erreur lors de la suppression du paiement : " + e.getMessage(), null));
        }
    }

    // 🔥 CONSULTATION DES PAIEMENTS

    /**
     * Récupérer un paiement par ID
     * GET /api/paiements/{userId}/{paiementId}
     */
    @GetMapping("/{paiementId}")
    public ResponseEntity<ApiResponse> getPaiementById(
            @PathVariable UUID userId,
            @PathVariable UUID paiementId) {
        try {
            PaiementDTO paiement = paiementService.getPaiementById(userId, paiementId);
            return ResponseEntity.ok(new ApiResponse(true, "Paiement récupéré avec succès", paiement));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(new ApiResponse(false, "Erreur lors de la récupération du paiement : " + e.getMessage(), null));
        }
    }

    /**
     * Récupérer tous les paiements d'une facture
     * GET /api/paiements/{userId}/facture/{factureId}
     */
    @GetMapping("/facture/{factureId}")
    public ResponseEntity<ApiResponse> getPaiementsByFacture(
            @PathVariable UUID userId,
            @PathVariable UUID factureId) {
        try {
            List<PaiementDTO> paiements = paiementService.getPaiementsByFacture(userId, factureId);
            return ResponseEntity.ok(new ApiResponse(
                    true,
                    "Paiements de la facture récupérés avec succès (" + paiements.size() + " paiements)",
                    paiements
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(new ApiResponse(false, "Erreur lors de la récupération des paiements : " + e.getMessage(), null));
        }
    }

    /**
     * Récupérer tous les paiements du cabinet
     * GET /api/paiements/{userId}
     */
    @GetMapping
    public ResponseEntity<ApiResponse> getAllPaiements(@PathVariable UUID userId) {
        try {
            List<PaiementDTO> paiements = paiementService.getAllPaiements(userId);
            return ResponseEntity.ok(new ApiResponse(
                    true,
                    "Tous les paiements récupérés avec succès (" + paiements.size() + " paiements)",
                    paiements
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(new ApiResponse(false, "Erreur lors de la récupération des paiements : " + e.getMessage(), null));
        }
    }

    /**
     * Récupérer les paiements par période
     * GET /api/paiements/{userId}/periode?dateDebut=2025-01-01&dateFin=2025-01-31
     */
    @GetMapping("/periode")
    public ResponseEntity<ApiResponse> getPaiementsByPeriode(
            @PathVariable UUID userId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateDebut,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFin) {
        try {
            List<PaiementDTO> paiements = paiementService.getPaiementsByPeriode(userId, dateDebut, dateFin);
            return ResponseEntity.ok(new ApiResponse(
                    true,
                    "Paiements de la période récupérés avec succès (" + paiements.size() + " paiements)",
                    Map.of(
                            "dateDebut", dateDebut,
                            "dateFin", dateFin,
                            "nombrePaiements", paiements.size(),
                            "paiements", paiements
                    )
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(new ApiResponse(false, "Erreur lors de la récupération des paiements : " + e.getMessage(), null));
        }
    }

    // 🔥 STATISTIQUES ET KPI

    /**
     * Récupérer les statistiques des paiements du cabinet
     * GET /api/paiements/{userId}/statistiques
     */
    @GetMapping("/statistiques")
    public ResponseEntity<ApiResponse> getStatistiquesPaiements(@PathVariable UUID userId) {
        try {
            Map<String, Object> statistiques = paiementService.getStatistiquesPaiements(userId);
            return ResponseEntity.ok(new ApiResponse(
                    true,
                    "Statistiques des paiements récupérées avec succès",
                    statistiques
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(new ApiResponse(false, "Erreur lors de la récupération des statistiques : " + e.getMessage(), null));
        }
    }

    /**
     * Récupérer le récapitulatif complet des paiements d'une facture
     * GET /api/paiements/{userId}/facture/{factureId}/recapitulatif
     */
    @GetMapping("/facture/{factureId}/recapitulatif")
    public ResponseEntity<ApiResponse> getRecapitulatifPaiements(
            @PathVariable UUID userId,
            @PathVariable UUID factureId) {
        try {
            Map<String, Object> recapitulatif = paiementService.getRecapitulatifPaiements(userId, factureId);
            return ResponseEntity.ok(new ApiResponse(
                    true,
                    "Récapitulatif des paiements récupéré avec succès",
                    recapitulatif
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(new ApiResponse(false, "Erreur lors de la récupération du récapitulatif : " + e.getMessage(), null));
        }
    }

    // 🔥 ENDPOINTS UTILITAIRES

    /**
     * Paiement rapide : créer un paiement avec données minimales
     * POST /api/paiements/{userId}/rapide
     */
    @PostMapping("/rapide")
    public ResponseEntity<ApiResponse> paiementRapide(
            @PathVariable UUID userId,
            @RequestBody Map<String, Object> request) {
        try {
            UUID factureId = UUID.fromString((String) request.get("factureId"));
            String montantStr = request.get("montant").toString();

            CreatePaiementDTO paiementRapide = new CreatePaiementDTO();
            paiementRapide.setFactureId(factureId);
            paiementRapide.setMontant(new java.math.BigDecimal(montantStr));
            paiementRapide.setDatePaiement(LocalDate.now());

            // Mode de paiement optionnel
            if (request.get("modePaiement") != null) {
                paiementRapide.setModePaiement(
                        myavocat.legit.model.ModePaiement.valueOf((String) request.get("modePaiement"))
                );
            }

            PaiementDTO paiement = paiementService.creerPaiement(userId, paiementRapide);
            return ResponseEntity.ok(new ApiResponse(
                    true,
                    "Paiement rapide créé avec succès",
                    paiement
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(new ApiResponse(false, "Erreur lors du paiement rapide : " + e.getMessage(), null));
        }
    }

    /**
     * Marquer une facture comme entièrement payée
     * POST /api/paiements/{userId}/facture/{factureId}/solder
     */
    @PostMapping("/facture/{factureId}/solder")
    public ResponseEntity<ApiResponse> solderFacture(
            @PathVariable UUID userId,
            @PathVariable UUID factureId,
            @RequestBody(required = false) Map<String, Object> request) {
        try {
            // Récupérer les informations de la facture via le service des factures
            // Pour obtenir le montant restant dû
            CreatePaiementDTO paiementSolde = new CreatePaiementDTO();
            paiementSolde.setFactureId(factureId);
            paiementSolde.setDatePaiement(LocalDate.now());

            // Le montant sera calculé automatiquement comme le restant dû
            // Mode de paiement par défaut ou depuis la requête
            if (request != null && request.get("modePaiement") != null) {
                paiementSolde.setModePaiement(
                        myavocat.legit.model.ModePaiement.valueOf((String) request.get("modePaiement"))
                );
            }

            // Note: Il faudrait modifier le service pour permettre le calcul automatique du montant
            // Pour l'instant, on retourne une erreur explicative
            return ResponseEntity.badRequest()
                    .body(new ApiResponse(false, "Fonctionnalité 'solder automatiquement' à implémenter dans le service", null));

        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(new ApiResponse(false, "Erreur lors du solde de la facture : " + e.getMessage(), null));
        }
    }
}
