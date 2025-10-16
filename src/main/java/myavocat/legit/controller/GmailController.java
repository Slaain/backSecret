package myavocat.legit.controller;

import myavocat.legit.model.EmailAccount;
import myavocat.legit.model.EmailWebhookLog;
import myavocat.legit.repository.EmailAccountRepository;
import myavocat.legit.service.GmailWebhookService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/gmail")
@CrossOrigin(origins = "*")
public class GmailController {


    private final GmailWebhookService gmailWebhookService;
    private static final Logger logger = LoggerFactory.getLogger(GmailWebhookService.class);
    private final EmailAccountRepository emailAccountRepository;

    public GmailController(GmailWebhookService gmailWebhookService,
                           EmailAccountRepository emailAccountRepository) {
        this.gmailWebhookService = gmailWebhookService;
        this.emailAccountRepository = emailAccountRepository;
    }

    /**
     * Activer le Watch Gmail pour un compte existant
     */
    @PostMapping("/subscribe/{accountId}")
    public ResponseEntity<String> subscribe(@PathVariable UUID accountId) {
        return emailAccountRepository.findById(accountId)
                .map(account -> {
                    gmailWebhookService.subscribeToGmail(account);
                    return ResponseEntity.ok("Watch Gmail activé pour " + account.getEmailAddress());
                })
                .orElse(ResponseEntity.badRequest().body("Compte email introuvable"));
    }

    /**
     * Forcer une synchronisation Gmail (manuelle)
     */
    @GetMapping("/emails/force-sync/{accountId}")
    public ResponseEntity<?> forceSync(@PathVariable UUID accountId) {
        EmailAccount account = emailAccountRepository.findById(accountId)
                .orElseThrow(() -> new RuntimeException("Compte introuvable"));

        try {
            EmailWebhookLog log = gmailWebhookService.forceSync(account);
            return ResponseEntity.ok(log);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/webhook")
    public ResponseEntity<String> receiveGmailWebhook(@RequestBody String pubsubMessage) {
        logger.info("🔵 Webhook Pub/Sub Gmail reçu !");

        try {
            EmailWebhookLog result = gmailWebhookService.processGmailPubSubNotification(pubsubMessage);
            return ResponseEntity.ok("Message processed");
        } catch (Exception e) {
            logger.error("Erreur webhook Pub/Sub", e);
            return ResponseEntity.status(500).body("Error");
        }
    }
}
