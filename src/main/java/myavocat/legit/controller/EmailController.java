package myavocat.legit.controller;

import myavocat.legit.model.Client;
import myavocat.legit.model.EmailWebhookLog;
import myavocat.legit.repository.EmailWebhookLogRepository;
import myavocat.legit.response.ApiResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/emails")
@CrossOrigin(origins = "*")
public class EmailController {

    @Autowired
    private EmailWebhookLogRepository emailWebhookLogRepository;

    /**
     * Récupérer les X derniers emails (par défaut 20)
     */
    @GetMapping("/recent")
    public ResponseEntity<ApiResponse> getRecentEmails(
            @RequestParam(defaultValue = "20") int limit) {

        List<EmailWebhookLog> emails = emailWebhookLogRepository
                .findAll(PageRequest.of(0, limit))
                .getContent();

        return ResponseEntity.ok(new ApiResponse(true, "Emails récents récupérés", emails));
    }

    /**
     * Récupérer les emails d’un compte utilisateur
     */
    @GetMapping("/by-account/{accountId}")
    public ResponseEntity<ApiResponse> getEmailsByAccount(@PathVariable UUID accountId,
                                                          @RequestParam(defaultValue = "20") int limit) {
        List<EmailWebhookLog> emails = emailWebhookLogRepository
                .findByEmailAccountIdOrderByWebhookReceivedAtDesc(accountId, PageRequest.of(0, limit))
                .getContent();

        return ResponseEntity.ok(new ApiResponse(true, "Emails pour le compte " + accountId, emails));
    }

    /**
     * Récupérer les emails par expéditeur (adresse email brute)
     */
    @GetMapping("/by-sender")
    public ResponseEntity<ApiResponse> getEmailsBySender(@RequestParam String sender) {
        List<EmailWebhookLog> emails = emailWebhookLogRepository.findBySenderEmail(sender);
        return ResponseEntity.ok(new ApiResponse(true, "Emails envoyés par " + sender, emails));
    }

    /**
     * Récupérer les emails liés à un client
     */
    @GetMapping("/by-client/{clientId}")
    public ResponseEntity<ApiResponse> getEmailsByClient(@PathVariable UUID clientId,
                                                         @RequestParam(defaultValue = "20") int limit) {
        Client client = new Client();
        client.setId(clientId);

        List<EmailWebhookLog> emails = emailWebhookLogRepository
                .findByClient(client)
                .stream()
                .sorted(Comparator.comparing(EmailWebhookLog::getWebhookReceivedAt).reversed())
                .limit(limit)
                .toList();

        return ResponseEntity.ok(new ApiResponse(true, "Emails pour le client " + clientId, emails));
    }
}
