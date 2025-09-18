package myavocat.legit.controller;

import myavocat.legit.repository.EmailAccountRepository;
import myavocat.legit.service.GmailWebhookService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/gmail")
@CrossOrigin(origins = "*")
public class GmailController {

    private final GmailWebhookService gmailWebhookService;
    private final EmailAccountRepository emailAccountRepository;

    public GmailController(GmailWebhookService gmailWebhookService,
                           EmailAccountRepository emailAccountRepository) {
        this.gmailWebhookService = gmailWebhookService;
        this.emailAccountRepository = emailAccountRepository;
    }

    /**
     * Activer le Watch Gmail pour un compte existant (lié à EmailAccount en DB)
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
}
