package myavocat.legit.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class EmailOAuthConfig {

    // Configuration Gmail API
    @Value("${oauth.gmail.client-id}")
    private String gmailClientId;

    @Value("${oauth.gmail.client-secret}")
    private String gmailClientSecret;

    @Value("${oauth.gmail.redirect-uri}")
    private String gmailRedirectUri;

    // Configuration Microsoft Graph (Outlook)
    @Value("${oauth.outlook.client-id}")
    private String outlookClientId;

    @Value("${oauth.outlook.client-secret}")
    private String outlookClientSecret;

    @Value("${oauth.outlook.redirect-uri}")
    private String outlookRedirectUri;

    @Value("${oauth.outlook.tenant-id:common}")
    private String outlookTenantId;

    // URLs de base
    private static final String GMAIL_AUTH_URL = "https://accounts.google.com/o/oauth2/v2/auth";
    private static final String GMAIL_TOKEN_URL = "https://oauth2.googleapis.com/token";
    private static final String GMAIL_API_BASE = "https://gmail.googleapis.com/gmail/v1";

    private static final String OUTLOOK_AUTH_URL = "https://login.microsoftonline.com/%s/oauth2/v2.0/authorize";
    private static final String OUTLOOK_TOKEN_URL = "https://login.microsoftonline.com/%s/oauth2/v2.0/token";
    private static final String OUTLOOK_API_BASE = "https://graph.microsoft.com/v1.0";

    // Scopes nécessaires
    private static final String GMAIL_SCOPES = "https://www.googleapis.com/auth/gmail.readonly https://www.googleapis.com/auth/gmail.modify";
    private static final String OUTLOOK_SCOPES = "https://graph.microsoft.com/mail.read https://graph.microsoft.com/mail.readwrite";

    // Getters pour Gmail
    public String getGmailClientId() {
        return gmailClientId;
    }

    public String getGmailClientSecret() {
        return gmailClientSecret;
    }

    public String getGmailRedirectUri() {
        return gmailRedirectUri;
    }

    public String getGmailAuthUrl() {
        return GMAIL_AUTH_URL;
    }

    public String getGmailTokenUrl() {
        return GMAIL_TOKEN_URL;
    }

    public String getGmailApiBase() {
        return GMAIL_API_BASE;
    }

    public String getGmailScopes() {
        return GMAIL_SCOPES;
    }

    // Getters pour Outlook
    public String getOutlookClientId() {
        return outlookClientId;
    }

    public String getOutlookClientSecret() {
        return outlookClientSecret;
    }

    public String getOutlookRedirectUri() {
        return outlookRedirectUri;
    }

    public String getOutlookTenantId() {
        return outlookTenantId;
    }

    public String getOutlookAuthUrl() {
        return String.format(OUTLOOK_AUTH_URL, outlookTenantId);
    }

    public String getOutlookTokenUrl() {
        return String.format(OUTLOOK_TOKEN_URL, outlookTenantId);
    }

    public String getOutlookApiBase() {
        return OUTLOOK_API_BASE;
    }

    public String getOutlookScopes() {
        return OUTLOOK_SCOPES;
    }

    // Méthodes utilitaires pour construire les URLs d'autorisation
    public String buildGmailAuthUrl(String state) {
        return String.format(
                "%s?client_id=%s&redirect_uri=%s&scope=%s&response_type=code&access_type=offline&state=%s",
                GMAIL_AUTH_URL,
                gmailClientId,
                gmailRedirectUri,
                GMAIL_SCOPES.replace(" ", "%20"),
                state
        );
    }

    public String buildOutlookAuthUrl(String state) {
        return String.format(
                "%s?client_id=%s&redirect_uri=%s&scope=%s&response_type=code&state=%s",
                getOutlookAuthUrl(),
                outlookClientId,
                outlookRedirectUri,
                OUTLOOK_SCOPES.replace(" ", "%20"),
                state
        );
    }
}
