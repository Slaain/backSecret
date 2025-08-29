package myavocat.legit.service;

import myavocat.legit.config.EmailOAuthConfig;
import myavocat.legit.config.WebhookConfig;
import myavocat.legit.model.EmailAccount;
import myavocat.legit.model.EmailWebhookLog;
import myavocat.legit.repository.EmailAccountRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class GmailWebhookService {

    private static final Logger logger = LoggerFactory.getLogger(GmailWebhookService.class);

    @Autowired
    private EmailOAuthConfig oauthConfig;

    @Autowired
    private WebhookConfig webhookConfig;

    @Autowired
    private EmailAccountRepository emailAccountRepository;

    @Autowired
    private EmailWebhookService emailWebhookService;

    @Autowired
    private OAuthTokenService oauthTokenService;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Traiter une notification Pub/Sub de Gmail
     */
    public EmailWebhookLog processGmailPubSubNotification(String pubsubMessage) {
        logger.info("Processing Gmail Pub/Sub notification");

        try {
            // Décoder le message Pub/Sub
            JsonNode pubsubData = objectMapper.readTree(pubsubMessage);
            String data = pubsubData.get("message").get("data").asText();
            String decodedData = new String(Base64.getDecoder().decode(data));

            logger.debug("Decoded Pub/Sub data: {}", decodedData);

            JsonNode gmailData = objectMapper.readTree(decodedData);
            String historyId = gmailData.get("historyId").asText();
            String emailAddress = gmailData.get("emailAddress").asText();

            // Trouver le compte email correspondant
            Optional<EmailAccount> accountOpt = emailAccountRepository.findByEmailAddressAndIsActive(emailAddress, true);
            if (accountOpt.isEmpty()) {
                logger.warn("Compte email non trouvé ou inactif: {}", emailAddress);
                return null;
            }

            EmailAccount account = accountOpt.get();

            // Récupérer l'historique Gmail depuis le dernier sync
            return processGmailHistory(account, historyId);

        } catch (Exception e) {
            logger.error("Erreur lors du traitement de la notification Pub/Sub Gmail", e);
            return null;
        }
    }

    /**
     * Traiter l'historique Gmail pour récupérer les nouveaux emails
     */
    private EmailWebhookLog processGmailHistory(EmailAccount account, String historyId) {
        try {
            // Récupérer le token d'accès valide
            String accessToken = oauthTokenService.getValidAccessToken(account);
            if (accessToken == null) {
                logger.error("Token d'accès invalide pour le compte: {}", account.getEmailAddress());
                return null;
            }

            // Appeler l'API Gmail History
            String historyUrl = String.format("%s/users/%s/history?startHistoryId=%s",
                    oauthConfig.getGmailApiBase(),
                    "me", // ou account.getEmailAddress()
                    historyId);

            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + accessToken);
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<String> entity = new HttpEntity<>(headers);
            ResponseEntity<String> response = restTemplate.exchange(historyUrl, HttpMethod.GET, entity, String.class);

            if (response.getStatusCode() == HttpStatus.OK) {
                return processGmailHistoryResponse(account, response.getBody());
            } else {
                logger.error("Erreur API Gmail History: {}", response.getStatusCode());
                return null;
            }

        } catch (Exception e) {
            logger.error("Erreur lors de la récupération de l'historique Gmail", e);
            return null;
        }
    }

    /**
     * Traiter la réponse de l'API Gmail History
     */
    private EmailWebhookLog processGmailHistoryResponse(EmailAccount account, String responseBody) {
        try {
            JsonNode historyResponse = objectMapper.readTree(responseBody);

            if (!historyResponse.has("history")) {
                logger.info("Aucun nouvel historique trouvé");
                return null;
            }

            JsonNode history = historyResponse.get("history");

            // Parcourir l'historique pour trouver les nouveaux messages
            for (JsonNode historyItem : history) {
                if (historyItem.has("messagesAdded")) {
                    JsonNode messagesAdded = historyItem.get("messagesAdded");

                    for (JsonNode messageAdded : messagesAdded) {
                        JsonNode message = messageAdded.get("message");
                        String messageId = message.get("id").asText();

                        // Traiter chaque nouveau message
                        EmailWebhookLog result = processGmailMessage(account, messageId);
                        if (result != null) {
                            return result; // Retourner le premier traitement réussi
                        }
                    }
                }
            }

            return null;

        } catch (Exception e) {
            logger.error("Erreur lors du traitement de la réponse Gmail History", e);
            return null;
        }
    }

    /**
     * Traiter un message Gmail spécifique
     */
    private EmailWebhookLog processGmailMessage(EmailAccount account, String messageId) {
        try {
            // Récupérer le token d'accès valide
            String accessToken = oauthTokenService.getValidAccessToken(account);
            if (accessToken == null) {
                return null;
            }

            // Récupérer les détails du message
            String messageUrl = String.format("%s/users/%s/messages/%s",
                    oauthConfig.getGmailApiBase(),
                    "me",
                    messageId);

            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + accessToken);
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<String> entity = new HttpEntity<>(headers);
            ResponseEntity<String> response = restTemplate.exchange(messageUrl, HttpMethod.GET, entity, String.class);

            if (response.getStatusCode() == HttpStatus.OK) {
                return parseGmailMessage(account, response.getBody());
            } else {
                logger.error("Erreur récupération message Gmail: {}", response.getStatusCode());
                return null;
            }

        } catch (Exception e) {
            logger.error("Erreur lors du traitement du message Gmail {}", messageId, e);
            return null;
        }
    }

    /**
     * Parser un message Gmail et extraire les informations
     */
    private EmailWebhookLog parseGmailMessage(EmailAccount account, String messageData) {
        try {
            JsonNode message = objectMapper.readTree(messageData);

            String messageId = message.get("id").asText();
            String threadId = message.get("threadId").asText();

            JsonNode payload = message.get("payload");
            JsonNode headers = payload.get("headers");

            // Extraire les headers importants
            String senderEmail = null;
            String subject = null;

            for (JsonNode header : headers) {
                String name = header.get("name").asText();
                String value = header.get("value").asText();

                if ("From".equals(name)) {
                    senderEmail = value;
                } else if ("Subject".equals(name)) {
                    subject = value;
                }
            }

            if (senderEmail == null) {
                logger.warn("Email expéditeur non trouvé dans le message: {}", messageId);
                return null;
            }

            // Extraire les pièces jointes
            List<EmailWebhookService.AttachmentData> attachments = extractGmailAttachments(payload, messageId, account);

            if (attachments.isEmpty()) {
                logger.info("Aucune pièce jointe trouvée dans le message: {}", messageId);
                return null;
            }

            // Traiter via le service principal
            return emailWebhookService.processEmailWebhook(
                    EmailWebhookLog.WebhookType.GMAIL_PUBSUB,
                    account.getId().toString(),
                    senderEmail,
                    subject,
                    messageId,
                    threadId,
                    attachments,
                    messageData
            );

        } catch (Exception e) {
            logger.error("Erreur lors du parsing du message Gmail", e);
            return null;
        }
    }

    /**
     * Extraire les pièces jointes d'un message Gmail
     */
    private List<EmailWebhookService.AttachmentData> extractGmailAttachments(JsonNode payload, String messageId, EmailAccount account) {
        List<EmailWebhookService.AttachmentData> attachments = new ArrayList<>();

        try {
            extractAttachmentsFromParts(payload, attachments, messageId, account);
        } catch (Exception e) {
            logger.error("Erreur lors de l'extraction des pièces jointes Gmail", e);
        }

        return attachments;
    }

    /**
     * Extraction récursive des pièces jointes depuis les parties du message
     */
    private void extractAttachmentsFromParts(JsonNode part, List<EmailWebhookService.AttachmentData> attachments,
                                             String messageId, EmailAccount account) {

        // Vérifier si cette partie est une pièce jointe
        if (part.has("filename") && part.get("filename").asText().length() > 0) {
            String filename = part.get("filename").asText();

            if (part.has("body") && part.get("body").has("attachmentId")) {
                String attachmentId = part.get("body").get("attachmentId").asText();

                // Télécharger la pièce jointe
                byte[] attachmentData = downloadGmailAttachment(messageId, attachmentId, account);
                if (attachmentData != null) {
                    String contentType = part.has("mimeType") ? part.get("mimeType").asText() : "application/octet-stream";
                    attachments.add(new EmailWebhookService.AttachmentData(filename, contentType, attachmentData));
                    logger.info("Pièce jointe extraite: {} ({})", filename, attachmentData.length + " bytes");
                }
            }
        }

        // Traitement récursif des sous-parties
        if (part.has("parts")) {
            for (JsonNode subPart : part.get("parts")) {
                extractAttachmentsFromParts(subPart, attachments, messageId, account);
            }
        }
    }

    /**
     * Télécharger une pièce jointe Gmail via son ID
     */
    private byte[] downloadGmailAttachment(String messageId, String attachmentId, EmailAccount account) {
        try {
            String accessToken = oauthTokenService.getValidAccessToken(account);
            if (accessToken == null) {
                return null;
            }

            String attachmentUrl = String.format("%s/users/%s/messages/%s/attachments/%s",
                    oauthConfig.getGmailApiBase(),
                    "me",
                    messageId,
                    attachmentId);

            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + accessToken);

            HttpEntity<String> entity = new HttpEntity<>(headers);
            ResponseEntity<String> response = restTemplate.exchange(attachmentUrl, HttpMethod.GET, entity, String.class);

            if (response.getStatusCode() == HttpStatus.OK) {
                JsonNode attachmentResponse = objectMapper.readTree(response.getBody());
                String data = attachmentResponse.get("data").asText();

                // Décoder les données Base64 URL-safe
                return Base64.getUrlDecoder().decode(data);
            } else {
                logger.error("Erreur téléchargement pièce jointe Gmail: {}", response.getStatusCode());
                return null;
            }

        } catch (Exception e) {
            logger.error("Erreur lors du téléchargement de la pièce jointe Gmail", e);
            return null;
        }
    }

    public void subscribeToGmail(EmailAccount account) {
        String accessToken = oauthTokenService.getValidAccessToken(account);

        String url = "https://gmail.googleapis.com/gmail/v1/users/me/watch";

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> body = new HashMap<>();
        body.put("labelIds", List.of("INBOX"));
        body.put("topicName", "projects/<TON_PROJET_GCP>/topics/<TON_TOPIC>");

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
        ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);

        logger.info("Réponse Gmail watch: {}", response.getBody());
    }

}
