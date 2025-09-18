package myavocat.legit.service;

import myavocat.legit.config.EmailOAuthConfig;
import myavocat.legit.model.EmailAccount;
import myavocat.legit.repository.EmailAccountRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.time.LocalDateTime;
import java.util.Base64;

@Service
public class OAuthTokenService {

    private static final Logger logger = LoggerFactory.getLogger(OAuthTokenService.class);

    @Autowired
    private EmailOAuthConfig oauthConfig;

    @Autowired
    private EmailAccountRepository emailAccountRepository;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    // Clé de chiffrement (à externaliser en production)
    private static final String ENCRYPTION_KEY = "MySecretKey12345"; // 16 caractères pour AES-128
    private static final String ALGORITHM = "AES";

    /**
     * Échanger un code d'autorisation contre des tokens pour Gmail
     */
    public boolean exchangeGmailAuthCode(EmailAccount account, String authCode) {
        try {
            logger.info("Échange du code d'autorisation Gmail pour: {}", account.getEmailAddress());

            MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
            params.add("client_id", oauthConfig.getGmailClientId());
            params.add("client_secret", oauthConfig.getGmailClientSecret());
            params.add("code", authCode);
            params.add("grant_type", "authorization_code");
            params.add("redirect_uri", oauthConfig.getGmailRedirectUri());

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            HttpEntity<MultiValueMap<String, String>> entity = new HttpEntity<>(params, headers);
            ResponseEntity<String> response = restTemplate.exchange(
                    oauthConfig.getGmailTokenUrl(),
                    HttpMethod.POST,
                    entity,
                    String.class
            );

            if (response.getStatusCode() == HttpStatus.OK) {
                return processTokenResponse(account, response.getBody());
            } else {
                logger.error("Erreur échange code Gmail: {}", response.getStatusCode());
                return false;
            }

        } catch (Exception e) {
            logger.error("Erreur lors de l'échange du code Gmail", e);
            return false;
        }
    }

    /**
     * Échanger un code d'autorisation contre des tokens pour Outlook
     */
    public boolean exchangeOutlookAuthCode(EmailAccount account, String authCode) {
        try {
            logger.info("Échange du code d'autorisation Outlook pour: {}", account.getEmailAddress());

            MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
            params.add("client_id", oauthConfig.getOutlookClientId());
            params.add("client_secret", oauthConfig.getOutlookClientSecret());
            params.add("code", authCode);
            params.add("grant_type", "authorization_code");
            params.add("redirect_uri", oauthConfig.getOutlookRedirectUri());
            params.add("scope", oauthConfig.getOutlookScopes());

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            HttpEntity<MultiValueMap<String, String>> entity = new HttpEntity<>(params, headers);
            ResponseEntity<String> response = restTemplate.exchange(
                    oauthConfig.getOutlookTokenUrl(),
                    HttpMethod.POST,
                    entity,
                    String.class
            );

            if (response.getStatusCode() == HttpStatus.OK) {
                return processTokenResponse(account, response.getBody());
            } else {
                logger.error("Erreur échange code Outlook: {}", response.getStatusCode());
                return false;
            }

        } catch (Exception e) {
            logger.error("Erreur lors de l'échange du code Outlook", e);
            return false;
        }
    }

    /**
     * Traiter la réponse token et stocker les tokens chiffrés
     */
    private boolean processTokenResponse(EmailAccount account, String responseBody) {
        try {
            JsonNode tokenData = objectMapper.readTree(responseBody);

            String accessToken = tokenData.get("access_token").asText();
            String refreshToken = tokenData.has("refresh_token") ?
                    tokenData.get("refresh_token").asText() : null;
            int expiresIn = tokenData.get("expires_in").asInt();

            // Chiffrer les tokens
            String encryptedAccessToken = encrypt(accessToken);
            String encryptedRefreshToken = refreshToken != null ? encrypt(refreshToken) : null;

            // Calculer l'expiration
            LocalDateTime expiresAt = LocalDateTime.now().plusSeconds(expiresIn - 300); // 5 min de marge

            // Sauvegarder dans le compte
            account.setEncryptedAccessToken(encryptedAccessToken);
            account.setEncryptedRefreshToken(encryptedRefreshToken);
            account.setTokenExpiresAt(expiresAt);
            account.resetErrorCount();

            emailAccountRepository.save(account);

            logger.info("Tokens OAuth sauvegardés pour: {}", account.getEmailAddress());
            return true;

        } catch (Exception e) {
            logger.error("Erreur lors du traitement de la réponse token", e);
            return false;
        }
    }

    /**
     * Obtenir un token d'accès valide (refresh si nécessaire)
     */
    public String getValidAccessToken(EmailAccount account) {
        try {
            if (account.hasValidOAuthTokens()) {
                logger.info("✅ Access token encore valide pour {}", account.getEmailAddress());
                return decrypt(account.getEncryptedAccessToken());
            }

            logger.warn("⚠️ Access token expiré ou invalide pour {} (expiresAt={}, hasRefresh={})",
                    account.getEmailAddress(),
                    account.getTokenExpiresAt(),
                    account.getEncryptedRefreshToken() != null);

            if (account.getEncryptedRefreshToken() != null) {
                logger.info("🔄 Tentative de refresh du token pour {}", account.getEmailAddress());
                if (refreshTokens(account)) {
                    logger.info("✅ Refresh réussi pour {}", account.getEmailAddress());
                    return decrypt(account.getEncryptedAccessToken());
                } else {
                    logger.error("❌ Échec du refresh du token pour {}", account.getEmailAddress());
                }
            } else {
                logger.error("❌ Aucun refresh_token dispo pour {}", account.getEmailAddress());
            }

            return null;

        } catch (Exception e) {
            logger.error("Erreur lors de l'obtention du token d'accès pour {}", account.getEmailAddress(), e);
            return null;
        }
    }

    /**
     * Rafraîchir les tokens OAuth
     */
    private boolean refreshTokens(EmailAccount account) {
        try {
            logger.info("Rafraîchissement des tokens pour: {}", account.getEmailAddress());

            String refreshToken = decrypt(account.getEncryptedRefreshToken());
            String tokenUrl;
            MultiValueMap<String, String> params = new LinkedMultiValueMap<>();

            if (account.getProvider() == EmailAccount.EmailProvider.GMAIL) {
                tokenUrl = oauthConfig.getGmailTokenUrl();
                params.add("client_id", oauthConfig.getGmailClientId());
                params.add("client_secret", oauthConfig.getGmailClientSecret());
            } else {
                tokenUrl = oauthConfig.getOutlookTokenUrl();
                params.add("client_id", oauthConfig.getOutlookClientId());
                params.add("client_secret", oauthConfig.getOutlookClientSecret());
            }

            params.add("grant_type", "refresh_token");
            params.add("refresh_token", refreshToken);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            HttpEntity<MultiValueMap<String, String>> entity = new HttpEntity<>(params, headers);
            ResponseEntity<String> response = restTemplate.exchange(tokenUrl, HttpMethod.POST, entity, String.class);

            if (response.getStatusCode() == HttpStatus.OK) {
                return processTokenResponse(account, response.getBody());
            } else {
                logger.error("Erreur rafraîchissement token: {}", response.getStatusCode());
                // Marquer le compte comme nécessitant une nouvelle autorisation
                account.setEncryptedAccessToken(null);
                account.setEncryptedRefreshToken(null);
                account.setTokenExpiresAt(null);
                account.incrementErrorCount("Token refresh failed");
                emailAccountRepository.save(account);
                return false;
            }

        } catch (Exception e) {
            logger.error("Erreur lors du rafraîchissement des tokens", e);
            return false;
        }
    }

    /**
     * Révoquer les tokens OAuth
     */
    public void revokeTokens(EmailAccount account) {
        try {
            if (account.getEncryptedAccessToken() == null) {
                return;
            }

            String accessToken = decrypt(account.getEncryptedAccessToken());
            String revokeUrl = "";

            if (account.getProvider() == EmailAccount.EmailProvider.GMAIL) {
                revokeUrl = "https://oauth2.googleapis.com/revoke?token=" + accessToken;
            } else {
                // Pour Outlook, la révocation se fait différemment
                logger.info("Révocation Outlook non implémentée directement");
            }

            if (account.getProvider() == EmailAccount.EmailProvider.GMAIL) {
                restTemplate.postForEntity(revokeUrl, null, String.class);
            }

            // Nettoyer les tokens localement
            account.setEncryptedAccessToken(null);
            account.setEncryptedRefreshToken(null);
            account.setTokenExpiresAt(null);
            emailAccountRepository.save(account);

            logger.info("Tokens révoqués pour: {}", account.getEmailAddress());

        } catch (Exception e) {
            logger.error("Erreur lors de la révocation des tokens", e);
        }
    }

    /**
     * Chiffrer une chaîne de caractères
     */
    private String encrypt(String plainText) {
        try {
            SecretKeySpec keySpec = new SecretKeySpec(ENCRYPTION_KEY.getBytes(), ALGORITHM);
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, keySpec);

            byte[] encrypted = cipher.doFinal(plainText.getBytes());
            return Base64.getEncoder().encodeToString(encrypted);

        } catch (Exception e) {
            logger.error("Erreur lors du chiffrement", e);
            throw new RuntimeException("Erreur de chiffrement", e);
        }
    }

    /**
     * Déchiffrer une chaîne de caractères
     */
    private String decrypt(String encryptedText) {
        try {
            SecretKeySpec keySpec = new SecretKeySpec(ENCRYPTION_KEY.getBytes(), ALGORITHM);
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, keySpec);

            byte[] decoded = Base64.getDecoder().decode(encryptedText);
            byte[] decrypted = cipher.doFinal(decoded);
            return new String(decrypted);

        } catch (Exception e) {
            logger.error("Erreur lors du déchiffrement", e);
            throw new RuntimeException("Erreur de déchiffrement", e);
        }
    }

    /**
     * Vérifier si un compte a besoin d'une nouvelle autorisation
     */
    public boolean needsReauthorization(EmailAccount account) {
        return account.getEncryptedAccessToken() == null ||
                account.getEncryptedRefreshToken() == null;
    }

    /**
     * Nettoyer les tokens expirés sans refresh token
     */
    public void cleanupExpiredTokens() {
        LocalDateTime cutoff = LocalDateTime.now();

        emailAccountRepository.findAccountsWithExpiringTokens(cutoff)
                .stream()
                .filter(account -> account.getEncryptedRefreshToken() == null)
                .forEach(account -> {
                    logger.info("Nettoyage des tokens expirés pour: {}", account.getEmailAddress());
                    account.setEncryptedAccessToken(null);
                    account.setTokenExpiresAt(null);
                    emailAccountRepository.save(account);
                });
    }
}
