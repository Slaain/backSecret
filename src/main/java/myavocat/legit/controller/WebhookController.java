package myavocat.legit.controller;

import myavocat.legit.config.WebhookConfig;
import myavocat.legit.model.EmailWebhookLog;
import myavocat.legit.response.ApiResponse;
import myavocat.legit.service.GmailWebhookService;
import myavocat.legit.service.OutlookWebhookService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

@RestController
@RequestMapping("/api/webhooks")
@CrossOrigin(origins = "*")
public class WebhookController {

    private static final Logger logger = LoggerFactory.getLogger(WebhookController.class);

    @Autowired
    private WebhookConfig webhookConfig;

    @Autowired
    private GmailWebhookService gmailWebhookService;

    @Autowired
    private OutlookWebhookService outlookWebhookService;

    /**
     * Endpoint pour les webhooks Gmail Pub/Sub
     */
    @PostMapping("/gmail")
    public ResponseEntity<?> handleGmailWebhook(
            @RequestBody String payload,
            @RequestHeader(value = "Authorization", required = false) String authorization) {

        logger.info("Webhook Gmail reçu");

        try {
            // Validation basique de sécurité (optionnelle pour Gmail Pub/Sub)
            if (authorization != null && !validateGmailAuthorization(authorization)) {
                logger.warn("Authorization Gmail invalide");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(new ApiResponse(false, "Unauthorized"));
            }

            // Traiter la notification
            EmailWebhookLog result = gmailWebhookService.processGmailPubSubNotification(payload);

            if (result != null) {
                logger.info("Webhook Gmail traité avec succès: {}", result.getId());
                return ResponseEntity.ok(new ApiResponse(true, "Webhook processed", result.getId()));
            } else {
                logger.info("Webhook Gmail traité mais aucun résultat");
                return ResponseEntity.ok(new ApiResponse(true, "Webhook processed - no action needed"));
            }

        } catch (Exception e) {
            logger.error("Erreur traitement webhook Gmail", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse(false, "Processing error: " + e.getMessage()));
        }
    }

    /**
     * Endpoint pour les webhooks Microsoft Graph (Outlook)
     */
    @PostMapping("/outlook")
    public ResponseEntity<?> handleOutlookWebhook(
            @RequestBody String payload,
            @RequestHeader(value = "X-Hub-Signature", required = false) String signature,
            @RequestParam(value = "validationToken", required = false) String validationToken) {

        logger.info("Webhook Outlook reçu");

        try {
            // Gestion de la validation initiale Microsoft Graph
            if (validationToken != null) {
                logger.info("Validation token reçu pour Outlook: {}", validationToken);
                return ResponseEntity.ok(validationToken);
            }

            // Validation de signature (si configurée)
            if (signature != null && !validateOutlookSignature(payload, signature)) {
                logger.warn("Signature Outlook invalide");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(new ApiResponse(false, "Invalid signature"));
            }

            // Traiter la notification
            EmailWebhookLog result = outlookWebhookService.processOutlookGraphNotification(payload);

            if (result != null) {
                logger.info("Webhook Outlook traité avec succès: {}", result.getId());
                return ResponseEntity.ok(new ApiResponse(true, "Webhook processed", result.getId()));
            } else {
                logger.info("Webhook Outlook traité mais aucun résultat");
                return ResponseEntity.ok(new ApiResponse(true, "Webhook processed - no action needed"));
            }

        } catch (Exception e) {
            logger.error("Erreur traitement webhook Outlook", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse(false, "Processing error: " + e.getMessage()));
        }
    }

    /**
     * Endpoint de test pour vérifier que les webhooks fonctionnent
     */
    @GetMapping("/test")
    public ResponseEntity<ApiResponse> testWebhooks() {
        logger.info("Test des webhooks demandé");

        try {
            return ResponseEntity.ok(new ApiResponse(true, "Webhook endpoints are working",
                    "Gmail: " + webhookConfig.getGmailWebhookUrl() +
                            ", Outlook: " + webhookConfig.getOutlookWebhookUrl()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse(false, "Test failed: " + e.getMessage()));
        }
    }

    /**
     * Endpoint pour récupérer les statistiques des webhooks
     */
    @GetMapping("/stats")
    public ResponseEntity<ApiResponse> getWebhookStats() {
        logger.info("Statistiques des webhooks demandées");

        try {
            // TODO: Implémenter les statistiques depuis EmailWebhookLogRepository
            return ResponseEntity.ok(new ApiResponse(true, "Stats endpoint working",
                    "Feature to be implemented"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse(false, "Stats error: " + e.getMessage()));
        }
    }

    /**
     * Endpoint pour désactiver les webhooks d'urgence
     */
    @PostMapping("/emergency-stop")
    public ResponseEntity<ApiResponse> emergencyStop(@RequestParam String reason) {
        logger.warn("Arrêt d'urgence des webhooks demandé: {}", reason);

        try {
            // TODO: Implémenter l'arrêt d'urgence
            // - Désactiver tous les comptes
            // - Supprimer les subscriptions
            // - Logger l'événement

            return ResponseEntity.ok(new ApiResponse(true, "Emergency stop triggered", reason));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse(false, "Emergency stop failed: " + e.getMessage()));
        }
    }

    // Méthodes de validation privées

    /**
     * Valider l'autorisation Gmail (basique)
     */
    private boolean validateGmailAuthorization(String authorization) {
        // Gmail Pub/Sub utilise généralement JWT ou des tokens simples
        // Pour la sécurité, on pourrait vérifier un token pré-partagé
        return authorization.startsWith("Bearer ") &&
                authorization.length() > 20; // Validation basique
    }

    /**
     * Valider la signature Outlook avec HMAC
     */
    private boolean validateOutlookSignature(String payload, String signature) {
        try {
            // La signature Outlook utilise HMAC-SHA256
            String expectedSignature = calculateHmacSha256(payload, webhookConfig.getWebhookSecret());
            return signature.equals("sha256=" + expectedSignature);

        } catch (Exception e) {
            logger.error("Erreur validation signature Outlook", e);
            return false;
        }
    }

    /**
     * Calculer HMAC-SHA256
     */
    private String calculateHmacSha256(String data, String key)
            throws NoSuchAlgorithmException, InvalidKeyException {

        SecretKeySpec secretKeySpec = new SecretKeySpec(key.getBytes(), "HmacSHA256");
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(secretKeySpec);

        byte[] hash = mac.doFinal(data.getBytes());
        return Base64.getEncoder().encodeToString(hash);
    }

    /**
     * Gestion globale des erreurs du contrôleur
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse> handleGlobalException(Exception e) {
        logger.error("Erreur non gérée dans WebhookController", e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiResponse(false, "Internal server error", e.getMessage()));
    }
}
