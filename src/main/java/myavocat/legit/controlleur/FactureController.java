package myavocat.legit.controller;

import myavocat.legit.dto.FactureDTO;
import myavocat.legit.model.StatutPaiement;
import myavocat.legit.service.FactureService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/factures/{userId}") // `userId` est inclus dans l'URL
public class FactureController {

    private final FactureService factureService;

    public FactureController(FactureService factureService) {
        this.factureService = factureService;
    }

    /**
     * ✅ Création d'une facture (liée à un dossier et un client)
     * Seul un utilisateur appartenant au même office peut créer la facture.
     */
    @PostMapping
    public ResponseEntity<FactureDTO> creerFacture(
            @PathVariable UUID userId,
            @RequestBody Map<String, Object> request) {

        UUID clientId = UUID.fromString((String) request.get("clientId"));
        UUID dossierId = UUID.fromString((String) request.get("dossierId"));
        String intitule = (String) request.get("intitule");
        BigDecimal montantHt = new BigDecimal(request.get("montantHt").toString());
        boolean tvaApplicable = (boolean) request.get("tvaApplicable");

        FactureDTO factureDTO = factureService.creerFacture(userId, clientId, dossierId, intitule, montantHt, tvaApplicable);
        return ResponseEntity.ok(factureDTO);
    }

    /**
     * ✅ Récupérer toutes les factures accessibles par l'utilisateur (dans le même office)
     */
    @GetMapping
    public ResponseEntity<List<FactureDTO>> getAllFactures(@PathVariable UUID userId) {
        return ResponseEntity.ok(factureService.getAllFactures(userId));
    }

    /**
     * ✅ Récupérer une facture par ID
     * Seul un utilisateur du même office peut accéder aux factures.
     */
    @GetMapping("/{id}")
    public ResponseEntity<FactureDTO> getFactureById(@PathVariable UUID userId, @PathVariable UUID id) {
        return ResponseEntity.ok(factureService.getFactureById(userId, id));
    }

    /**
     * ✅ Mettre à jour le statut d'une facture (Réglée / En attente)
     * Seul un utilisateur du même office peut modifier une facture.
     */
    @PutMapping("/{id}/statut")
    public ResponseEntity<FactureDTO> updateStatutFacture(
            @PathVariable UUID userId,
            @PathVariable UUID id,
            @RequestParam StatutPaiement statut) {

        FactureDTO updatedFacture = factureService.updateStatutFacture(userId, id, statut);
        return ResponseEntity.ok(updatedFacture);
    }

    /**
     * ✅ Calculer les statistiques des factures (Total édité, payé, en attente)
     * Seul un utilisateur du même office peut voir les statistiques.
     */
    @GetMapping("/statistiques")
    public ResponseEntity<Map<String, BigDecimal>> getStatistiquesFactures(@PathVariable UUID userId) {
        return ResponseEntity.ok(factureService.getStatistiquesFactures(userId));
    }

    /**
     * ✅ Relancer les clients pour les factures impayées
     * Seul un utilisateur du même office peut relancer les factures.
     */
    @PostMapping("/relance")
    public ResponseEntity<String> relancerFacturesImpayees(@PathVariable UUID userId) {
        factureService.relancerFacturesImpayees(userId);
        return ResponseEntity.ok("Relances envoyées aux clients avec factures impayées.");
    }
}
