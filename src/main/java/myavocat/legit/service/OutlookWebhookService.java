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

@Service
public class OutlookWebhookService {

    private static final Logger logger = LoggerFactory.getLogger(OutlookWebhookService.class);

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
     * Traiter une notification webhook Microsoft Graph
     */
    public EmailWebhookLog processOutlookGraphNotification(String notificationPayload) {
        logger.info("Processing Outlook Graph webhook notification");

        try {
            JsonNode notification = objectMapper.readTree(notificationPayload);
            JsonNode value = notification.get("value");

            if (value == null || !value.isArray() || value.size() == 0) {
                logger.warn("Notification Outlook vide ou malformée");
                return null;
            }

            // Traiter chaque notification dans le tableau
            for (JsonNode notificationItem : value) {
                EmailWebhookLog result = processSingleOutlookNotification(notificationItem);
                if (result != null) {
                    return result; // Retourner le premier traitement réussi
                }
            }

            return null;

        } catch (Exception e) {
            logger.error("Erreur lors du traitement de la notification Outlook Graph", e);
            return null;
        }
    }

    /**
     * Traiter une notification individuelle
     */
    private EmailWebhookLog processSingleOutlookNotification(JsonNode notificationItem) {
        try {
            String subscriptionId = notificationItem.get("subscriptionId").asText();
            String changeType = notificationItem.get("changeType").asText();
            String resource = notificationItem.get("resource").asText();

            // Vérifier que c'est une notification de création de message
            if (!"created".equals(changeType)) {
                logger.debug("Notification ignorée, type de changement: {}", changeType);
                return null;
            }

            // Extraire l'ID du message depuis la resource
            // Format typique: /me/messages/{messageId}
            String messageId = extractMessageIdFromResource(resource);
            if (messageId == null) {
                logger.warn("Impossible d'extraire l'ID du message depuis: {}", resource);
                return null;
            }

            // Trouver le compte email associé à cette subscription
            Optional<EmailAccount> accountOpt = emailAccountRepository.findAccountsWithActiveWebhooks()
                    .stream()
                    .filter(account -> subscriptionId.equals(account.getWebhookSubscriptionId()))
                    .findFirst();

            if (accountOpt.isEmpty()) {
                logger.warn("Compte email non trouvé pour la subscription: {}", subscriptionId);
                return null;
            }

            EmailAccount account = accountOpt.get();

            // Traiter le message Outlook
            return processOutlookMessage(account, messageId);

        } catch (Exception e) {
            logger.error("Erreur lors du traitement de la notification individuelle Outlook", e);
            return null;
        }
    }

    /**
     * Extraire l'ID du message depuis la resource URL
     */
    private String extractMessageIdFromResource(String resource) {
        // Format: /me/messages/{messageId} ou users/{userId}/messages/{messageId}
        String[] parts = resource.split("/");
        for (int i = 0; i < parts.length - 1; i++) {
            if ("messages".equals(parts[i]) && i + 1 < parts.length) {
                return parts[i + 1];
            }
        }
        return null;
    }

    /**
     * Traiter un message Outlook spécifique
     */
    private EmailWebhookLog processOutlookMessage(EmailAccount account, String messageId) {
        try {
            // Récupérer le token d'accès valide
            String accessToken = oauthTokenService.getValidAccessToken(account);
            if (accessToken == null) {
                logger.error("Token d'accès invalide pour le compte: {}", account.getEmailAddress());
                return null;
            }

            // Récupérer les détails du message
            String messageUrl = String.format("%s/me/messages/%s?$expand=attachments",
                    oauthConfig.getOutlookApiBase(),
                    messageId);

            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + accessToken);
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<String> entity = new HttpEntity<>(headers);
            ResponseEntity<String> response = restTemplate.exchange(messageUrl, HttpMethod.GET, entity, String.class);

            if (response.getStatusCode() == HttpStatus.OK) {
                return parseOutlookMessage(account, response.getBody());
            } else {
                logger.error("Erreur récupération message Outlook: {}", response.getStatusCode());
                return null;
            }

        } catch (Exception e) {
            logger.error("Erreur lors du traitement du message Outlook {}", messageId, e);
            return null;
        }
    }

    /**
     * Parser un message Outlook et extraire les informations
     */
    private EmailWebhookLog parseOutlookMessage(EmailAccount account, String messageData) {
        try {
            JsonNode message = objectMapper.readTree(messageData);

            String messageId = message.get("id").asText();
            String conversationId = message.has("conversationId") ? message.get("conversationId").asText() : null;
            String subject = message.has("subject") ? message.get("subject").asText() : null;

            // Extraire l'expéditeur
            JsonNode sender = message.get("sender");
            String senderEmail = null;
            if (sender != null && sender.has("emailAddress") && sender.get("emailAddress").has("address")) {
                senderEmail = sender.get("emailAddress").get("address").asText();
            }

            if (senderEmail == null) {
                logger.warn("Email expéditeur non trouvé dans le message: {}", messageId);
                return null;
            }

            // Extraire les pièces jointes
            List<EmailWebhookService.AttachmentData> attachments = extractOutlookAttachments(message, account);

            if (attachments.isEmpty()) {
                logger.info("Aucune pièce jointe trouvée dans le message: {}", messageId);
                return null;
            }

            // Traiter via le service principal
            return emailWebhookService.processEmailWebhook(
                    EmailWebhookLog.WebhookType.OUTLOOK_GRAPH,
                    account.getId().toString(),
                    senderEmail,
                    subject,
                    messageId,
                    conversationId,
                    attachments,
                    messageData
            );

        } catch (Exception e) {
            logger.error("Erreur lors du parsing du message Outlook", e);
            return null;
        }
    }

    /**
     * Extraire les pièces jointes d'un message Outlook
     */
    private List<EmailWebhookService.AttachmentData> extractOutlookAttachments(JsonNode message, EmailAccount account) {
        List<EmailWebhookService.AttachmentData> attachments = new ArrayList<>();

        try {
            if (!message.has("attachments") || !message.get("attachments").isArray()) {
                return attachments;
            }

            JsonNode attachmentsNode = message.get("attachments");

            for (JsonNode attachment : attachmentsNode) {
                if (!"#microsoft.graph.fileAttachment".equals(attachment.get("@odata.type").asText())) {
                    continue; // Ignorer les autres types d'attachments
                }

                String filename = attachment.get("name").asText();
                String contentType = attachment.has("contentType") ?
                        attachment.get("contentType").asText() : "application/octet-stream";

                if (attachment.has("contentBytes")) {
                    String contentBytes = attachment.get("contentBytes").asText();
                    byte[] data = Base64.getDecoder().decode(contentBytes);

                    attachments.add(new EmailWebhookService.AttachmentData(filename, contentType, data));
                    logger.info("Pièce jointe Outlook extraite: {} ({} bytes)", filename, data.length);
                }
            }

        } catch (Exception e) {
            logger.error("Erreur lors de l'extraction des pièces jointes Outlook", e);
        }

        return attachments;
    }

    /**
     * Créer une subscription webhook pour un compte Outlook
     */
    public boolean createOutlookWebhookSubscription(EmailAccount account) {
        try {
            String accessToken = oauthTokenService.getValidAccessToken(account);
            if (accessToken == null) {
                logger.error("Token d'accès invalide pour créer la subscription: {}", account.getEmailAddress());
                return false;
            }

            String subscriptionUrl = oauthConfig.getOutlookApiBase() + "/subscriptions";

            // Calculer l'expiration (maximum 4230 minutes = ~3 jours pour Outlook)
            LocalDateTime expirationDateTime = LocalDateTime.now()
                    .plusHours(webhookConfig.getOutlookWebhookExpiryHours());

            // Créer le payload de subscription
            Map<String, Object> subscriptionData = new HashMap<>();
            subscriptionData.put("changeType", "created");
            subscriptionData.put("notificationUrl", webhookConfig.getOutlookWebhookUrl());
            subscriptionData.put("resource", "me/messages");
            subscriptionData.put("expirationDateTime", expirationDateTime.toString());
            subscriptionData.put("clientState", account.getId().toString()); // Pour la validation

            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + accessToken);
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(subscriptionData, headers);
            ResponseEntity<String> response = restTemplate.exchange(subscriptionUrl, HttpMethod.POST, entity, String.class);

            if (response.getStatusCode() == HttpStatus.CREATED) {
                JsonNode subscriptionResponse = objectMapper.readTree(response.getBody());
                String subscriptionId = subscriptionResponse.get("id").asText();

                // Sauvegarder les détails de la subscription
                account.setWebhookSubscriptionId(subscriptionId);
                account.setWebhookExpiry(expirationDateTime);
                account.setActive(true);
                emailAccountRepository.save(account);

                logger.info("Subscription Outlook créée avec succès: {} pour {}", subscriptionId, account.getEmailAddress());
                return true;
            } else {
                logger.error("Erreur création subscription Outlook: {}", response.getStatusCode());
                return false;
            }

        } catch (Exception e) {
            logger.error("Erreur lors de la création de la subscription Outlook", e);
            return false;
        }
    }

    /**
     * Supprimer une subscription webhook
     */
    public boolean deleteOutlookWebhookSubscription(EmailAccount account) {
        try {
            if (account.getWebhookSubscriptionId() == null) {
                return true; // Déjà supprimée
            }

            String accessToken = oauthTokenService.getValidAccessToken(account);
            if (accessToken == null) {
                logger.warn("Token invalide, impossible de supprimer la subscription pour: {}", account.getEmailAddress());
                // Nettoyer localement quand même
                account.setWebhookSubscriptionId(null);
                account.setWebhookExpiry(null);
                emailAccountRepository.save(account);
                return true;
            }

            String deleteUrl = String.format("%s/subscriptions/%s",
                    oauthConfig.getOutlookApiBase(),
                    account.getWebhookSubscriptionId());

            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + accessToken);

            HttpEntity<String> entity = new HttpEntity<>(headers);
            ResponseEntity<String> response = restTemplate.exchange(deleteUrl, HttpMethod.DELETE, entity, String.class);

            // Nettoyer les données locales
            account.setWebhookSubscriptionId(null);
            account.setWebhookExpiry(null);
            emailAccountRepository.save(account);

            logger.info("Subscription Outlook supprimée pour: {}", account.getEmailAddress());
            return response.getStatusCode() == HttpStatus.NO_CONTENT;

        } catch (Exception e) {
            logger.error("Erreur lors de la suppression de la subscription Outlook", e);
            return false;
        }
    }

    /**
     * Renouveler une subscription qui expire bientôt
     */
    public boolean renewOutlookWebhookSubscription(EmailAccount account) {
        try {
            if (account.getWebhookSubscriptionId() == null) {
                return createOutlookWebhookSubscription(account);
            }

            String accessToken = oauthTokenService.getValidAccessToken(account);
            if (accessToken == null) {
                return false;
            }

            String updateUrl = String.format("%s/subscriptions/%s",
                    oauthConfig.getOutlookApiBase(),
                    account.getWebhookSubscriptionId());

            LocalDateTime newExpiration = LocalDateTime.now()
                    .plusHours(webhookConfig.getOutlookWebhookExpiryHours());

            Map<String, Object> updateData = new HashMap<>();
            updateData.put("expirationDateTime", newExpiration.toString());

            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + accessToken);
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(updateData, headers);
            ResponseEntity<String> response = restTemplate.exchange(updateUrl, HttpMethod.PATCH, entity, String.class);

            if (response.getStatusCode() == HttpStatus.OK) {
                account.setWebhookExpiry(newExpiration);
                emailAccountRepository.save(account);

                logger.info("Subscription Outlook renouvelée pour: {}", account.getEmailAddress());
                return true;
            } else {
                logger.error("Erreur renouvellement subscription: {}", response.getStatusCode());
                return false;
            }

        } catch (Exception e) {
            logger.error("Erreur lors du renouvellement de la subscription Outlook", e);
            return false;
        }
    }
}
