package myavocat.legit.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class WebhookConfig {

    @Value("${webhook.base-url}")
    private String baseUrl;

    @Value("${webhook.gmail.endpoint}")
    private String gmailEndpoint;

    @Value("${webhook.outlook.endpoint}")
    private String outlookEndpoint;

    @Value("${webhook.secret}")
    private String webhookSecret;

    // Configuration timeouts
    @Value("${webhook.timeout.processing:300000}")
    private long processingTimeoutMs = 300000; // 5 minutes

    @Value("${webhook.timeout.connection:30000}")
    private long connectionTimeoutMs = 30000; // 30 seconds

    // Configuration retry
    @Value("${webhook.retry.max-attempts:3}")
    private int maxRetryAttempts = 3;

    @Value("${webhook.retry.delay:5000}")
    private long retryDelayMs = 5000; // 5 seconds

    // Configuration webhook expiry
    @Value("${webhook.expiry.gmail-hours:168}")
    private int gmailWebhookExpiryHours = 168; // 7 jours

    @Value("${webhook.expiry.outlook-hours:4200}")
    private int outlookWebhookExpiryHours = 4200; // ~6 mois (max Microsoft)

    // Getters
    public String getBaseUrl() {
        return baseUrl;
    }

    public String getGmailEndpoint() {
        return gmailEndpoint;
    }

    public String getOutlookEndpoint() {
        return outlookEndpoint;
    }

    public String getWebhookSecret() {
        return webhookSecret;
    }

    public long getProcessingTimeoutMs() {
        return processingTimeoutMs;
    }

    public long getConnectionTimeoutMs() {
        return connectionTimeoutMs;
    }

    public int getMaxRetryAttempts() {
        return maxRetryAttempts;
    }

    public long getRetryDelayMs() {
        return retryDelayMs;
    }

    public int getGmailWebhookExpiryHours() {
        return gmailWebhookExpiryHours;
    }

    public int getOutlookWebhookExpiryHours() {
        return outlookWebhookExpiryHours;
    }

    // URLs complètes
    public String getGmailWebhookUrl() {
        return baseUrl + gmailEndpoint;
    }

    public String getOutlookWebhookUrl() {
        return baseUrl + outlookEndpoint;
    }
}
