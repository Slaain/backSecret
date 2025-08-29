package myavocat.legit.controller;

import jakarta.servlet.http.HttpServletResponse;
import myavocat.legit.config.EmailOAuthConfig;
import myavocat.legit.model.EmailAccount;
import myavocat.legit.model.User;
import myavocat.legit.service.GmailWebhookService;
import myavocat.legit.repository.EmailAccountRepository;
import myavocat.legit.repository.UserRepository;
import myavocat.legit.response.ApiResponse;
import myavocat.legit.service.OAuthTokenService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

@RestController
@RequestMapping("/api/oauth")
@CrossOrigin(origins = "*")
public class OAuthController {

    private static final Logger logger = LoggerFactory.getLogger(OAuthController.class);

    @Autowired
    private OAuthTokenService oauthTokenService;

    @Autowired
    private EmailOAuthConfig oauthConfig;

    @Autowired
    private EmailAccountRepository emailAccountRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private GmailWebhookService gmailWebhookService;


    // ======================
    // Gmail Login
    // ======================
    @GetMapping("/gmail/login")
    public void loginWithGmail(HttpServletResponse response,
                               @RequestParam("userId") UUID userId) throws IOException {

        logger.info("Initiation connexion Gmail pour utilisateur: {}", userId);
        logger.info("URI de redirection utilisée: {}", oauthConfig.getGmailRedirectUri());
        try {
            // Créer ou récupérer l'EmailAccount
            EmailAccount account = getOrCreateEmailAccount(userId, EmailAccount.EmailProvider.GMAIL);

            String url = "https://accounts.google.com/o/oauth2/v2/auth" +
                    "?client_id=" + URLEncoder.encode(oauthConfig.getGmailClientId(), StandardCharsets.UTF_8) +
                    "&redirect_uri=" + URLEncoder.encode(oauthConfig.getGmailRedirectUri(), StandardCharsets.UTF_8) +
                    "&response_type=code" +
                    "&scope=" + URLEncoder.encode(oauthConfig.getGmailScopes(), StandardCharsets.UTF_8) +
                    "&access_type=offline" +
                    "&prompt=consent" +
                    "&state=" + account.getId(); // Utiliser l'ID du compte comme state

            logger.info("Redirection vers Gmail OAuth pour compte: {}", account.getId());
            response.sendRedirect(url);

        } catch (Exception e) {
            logger.error("Erreur lors de l'initiation Gmail OAuth", e);
            response.sendError(500, "Erreur serveur: " + e.getMessage());
        }
    }

    // ======================
// Gmail Callback
// ======================
    @GetMapping("/gmail/callback")
    public void gmailCallback(
            @RequestParam(value = "code", required = false) String code,
            @RequestParam(value = "state", required = false) String state,
            @RequestParam(value = "error", required = false) String error,
            HttpServletResponse response) throws IOException {

        logger.info("Callback Gmail reçu - code: {}, state: {}, error: {}", code, state, error);

        try {
            // Cas d'erreur OAuth
            if (error != null) {
                logger.warn("Erreur OAuth Gmail: {}", error);
                response.sendRedirect("http://localhost:4200/dashboard?gmail=error&reason=" + error);
                return;
            }

            // Vérifier que code et state sont présents
            if (code == null || code.trim().isEmpty()) {
                logger.error("Aucun code fourni dans le callback Gmail");
                response.sendRedirect("http://localhost:4200/dashboard?gmail=error&reason=no_code");
                return;
            }

            if (state == null || state.trim().isEmpty()) {
                logger.error("Aucun state fourni dans le callback Gmail");
                response.sendRedirect("http://localhost:4200/dashboard?gmail=error&reason=no_state");
                return;
            }

            UUID accountId = UUID.fromString(state);
            EmailAccount account = emailAccountRepository.findById(accountId)
                    .orElseThrow(() -> new RuntimeException("EmailAccount introuvable avec ID: " + accountId));

            boolean success = oauthTokenService.exchangeGmailAuthCode(account, code);

            if (success) {
                account.setActive(true);
                emailAccountRepository.save(account);

                // Activer le "watch" Gmail pour recevoir les webhooks
                try {
                    gmailWebhookService.subscribeToGmail(account);
                    logger.info("Watch Gmail activé pour: {}", account.getEmailAddress());
                } catch (Exception ex) {
                    logger.error("Erreur lors de l'activation du watch Gmail pour {}", account.getEmailAddress(), ex);
                }

                logger.info("Connexion Gmail réussie pour: {}", account.getEmailAddress());
                logger.info("Email à envoyer : {}", account.getEmailAddress());
                String emailParam = URLEncoder.encode(account.getEmailAddress(), StandardCharsets.UTF_8);
                response.sendRedirect("http://localhost:4200/dashboard?code=" + code + "&state=" + accountId + "&email=" + emailParam);

            } else {
                logger.error("Échec échange code Gmail pour compte: {}", accountId);
                response.sendRedirect("http://localhost:4200/dashboard?gmail=error&reason=token_exchange_failed");
            }

        } catch (Exception e) {
            logger.error("Exception Gmail OAuth callback", e);
            response.sendRedirect("http://localhost:4200/dashboard?gmail=error&reason=server_error");
        }
    }

    // ======================
    // Outlook Login
    // ======================
    @GetMapping("/outlook/login")
    public void loginWithOutlook(HttpServletResponse response,
                                 @RequestParam("userId") UUID userId) throws IOException {

        logger.info("Initiation connexion Outlook pour utilisateur: {}", userId);

        try {
            // Créer ou récupérer l'EmailAccount
            EmailAccount account = getOrCreateEmailAccount(userId, EmailAccount.EmailProvider.OUTLOOK);

            String url = "https://login.microsoftonline.com/" + oauthConfig.getOutlookTenantId() + "/oauth2/v2.0/authorize" +
                    "?client_id=" + URLEncoder.encode(oauthConfig.getOutlookClientId(), StandardCharsets.UTF_8) +
                    "&response_type=code" +
                    "&redirect_uri=" + URLEncoder.encode(oauthConfig.getOutlookRedirectUri(), StandardCharsets.UTF_8) +
                    "&response_mode=query" +
                    "&scope=" + URLEncoder.encode(oauthConfig.getOutlookScopes(), StandardCharsets.UTF_8) +
                    "&state=" + account.getId(); // Utiliser l'ID du compte comme state

            logger.info("Redirection vers Outlook OAuth pour compte: {}", account.getId());
            response.sendRedirect(url);

        } catch (Exception e) {
            logger.error("Erreur lors de l'initiation Outlook OAuth", e);
            response.sendError(500, "Erreur serveur: " + e.getMessage());
        }
    }

    // ======================
    // Outlook Callback
    // ======================
    @GetMapping("/outlook/callback")
    public ResponseEntity<ApiResponse> outlookCallback(
            @RequestParam("code") String code,
            @RequestParam("state") UUID accountId,
            @RequestParam(value = "error", required = false) String error) {

        logger.info("Callback Outlook reçu pour compte: {}, erreur: {}", accountId, error);

        try {
            if (error != null) {
                logger.warn("Erreur OAuth Outlook: {}", error);
                return ResponseEntity.badRequest()
                        .body(new ApiResponse(false, "Erreur OAuth Outlook: " + error, null));
            }

            EmailAccount account = emailAccountRepository.findById(accountId)
                    .orElseThrow(() -> new RuntimeException("EmailAccount introuvable avec ID: " + accountId));

            boolean success = oauthTokenService.exchangeOutlookAuthCode(account, code);

            if (success) {
                // Activer le compte après succès
                account.setActive(true);
                emailAccountRepository.save(account);

                logger.info("Connexion Outlook réussie pour: {}", account.getEmailAddress());
                return ResponseEntity.ok(new ApiResponse(true, "Connexion Outlook réussie", null));
            } else {
                logger.error("Échec échange code Outlook pour compte: {}", accountId);
                return ResponseEntity.internalServerError()
                        .body(new ApiResponse(false, "Erreur lors de l'échange du code OAuth", null));
            }

        } catch (Exception e) {
            logger.error("Exception Outlook OAuth callback", e);
            return ResponseEntity.internalServerError()
                    .body(new ApiResponse(false, "Erreur serveur: " + e.getMessage(), null));
        }
    }

    // ======================
    // Statut des connexions
    // ======================
    @GetMapping("/status/{userId}")
    public ResponseEntity<ApiResponse> getConnectionStatus(@PathVariable UUID userId) {
        try {
            logger.info("Récupération statut connexions pour utilisateur: {}", userId);

            var accountOpt = emailAccountRepository.findByUserId(userId);

            if (accountOpt.isEmpty()) {
                return ResponseEntity.ok(new ApiResponse(true, "Aucune connexion email configurée", null));
            }

            EmailAccount account = accountOpt.get();
            var status = new java.util.HashMap<String, Object>();
            status.put("provider", account.getProvider().name().toLowerCase());
            status.put("email", account.getEmailAddress());
            status.put("connected", account.isActive());
            status.put("hasValidTokens", account.hasValidOAuthTokens());
            status.put("lastSync", account.getLastSyncAt());
            status.put("errorCount", account.getSyncErrorsCount());

            return ResponseEntity.ok(new ApiResponse(true, "Statut récupéré", status));

        } catch (Exception e) {
            logger.error("Erreur récupération statut connexions", e);
            return ResponseEntity.badRequest()
                    .body(new ApiResponse(false, "Erreur: " + e.getMessage(), null));
        }
    }

    // ======================
    // Déconnecter un compte
    // ======================
    @PostMapping("/disconnect/{userId}")
    public ResponseEntity<ApiResponse> disconnectAccount(@PathVariable UUID userId) {
        try {
            logger.info("Déconnexion demandée pour utilisateur: {}", userId);

            var accountOpt = emailAccountRepository.findByUserId(userId);

            if (accountOpt.isEmpty()) {
                return ResponseEntity.ok(new ApiResponse(false, "Aucun compte email trouvé", null));
            }

            EmailAccount account = accountOpt.get();

            // Révoquer les tokens
            oauthTokenService.revokeTokens(account);

            // Désactiver le compte
            account.setActive(false);
            emailAccountRepository.save(account);

            logger.info("Compte email déconnecté: {}", account.getEmailAddress());
            return ResponseEntity.ok(new ApiResponse(true, "Compte déconnecté avec succès", null));

        } catch (Exception e) {
            logger.error("Erreur déconnexion compte", e);
            return ResponseEntity.badRequest()
                    .body(new ApiResponse(false, "Erreur déconnexion: " + e.getMessage(), null));
        }
    }

    // ======================
    // Méthode utilitaire privée
    // ======================
    private EmailAccount getOrCreateEmailAccount(UUID userId, EmailAccount.EmailProvider provider) {
        // Chercher un compte existant pour cet utilisateur
        var existingOpt = emailAccountRepository.findByUserId(userId);

        if (existingOpt.isPresent()) {
            EmailAccount existing = existingOpt.get();
            // Mettre à jour le provider si nécessaire
            if (existing.getProvider() != provider) {
                existing.setProvider(provider);
                existing = emailAccountRepository.save(existing);
            }
            return existing;
        }

        // Créer un nouveau compte
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé avec ID: " + userId));

        EmailAccount newAccount = new EmailAccount();
        newAccount.setUser(user);
        newAccount.setEmailAddress(user.getEmail()); // Utiliser l'email de l'utilisateur
        newAccount.setProvider(provider);
        newAccount.setActive(false); // Activé après autorisation réussie

        EmailAccount saved = emailAccountRepository.save(newAccount);
        logger.info("Nouveau EmailAccount créé: {} pour utilisateur: {}", saved.getId(), userId);

        return saved;
    }
}
