package myavocat.legit.controller;

import myavocat.legit.dto.AuthRequestDTO;
import myavocat.legit.dto.AuthResponseDTO;
import myavocat.legit.dto.OfficeAuthRequestDTO;
import myavocat.legit.dto.OfficeAuthResponseDTO;
import myavocat.legit.model.Office;
import myavocat.legit.model.User;
import myavocat.legit.service.OfficeAuthService;
import myavocat.legit.service.UserService;
import myavocat.legit.util.JwtUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

    @Autowired
    private OfficeAuthService officeAuthService;

    @Autowired
    private UserService userService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtil jwtUtil;

    /**
     * Première étape: authentification du cabinet
     */
    @PostMapping("/office")
    public ResponseEntity<?> authenticateOffice(@RequestBody OfficeAuthRequestDTO request) {
        try {
            logger.info("Tentative d'authentification pour le cabinet: {}", request.getOfficeName());

            // Vérifier si le nom du cabinet est vide
            if (request.getOfficeName() == null || request.getOfficeName().trim().isEmpty()) {
                return ResponseEntity.badRequest().body(
                        new OfficeAuthResponseDTO(false, "Le nom du cabinet est requis", null, null, null)
                );
            }

            // Vérifier si le mot de passe est vide
            if (request.getOfficePassword() == null || request.getOfficePassword().trim().isEmpty()) {
                return ResponseEntity.badRequest().body(
                        new OfficeAuthResponseDTO(false, "Le mot de passe du cabinet est requis", null, null, null)
                );
            }

            // Authentifier le cabinet
            Office office = officeAuthService.authenticateOffice(
                    request.getOfficeName(),
                    request.getOfficePassword()
            );

            // Génération d'un token temporaire
            String tempToken = officeAuthService.generateTempToken(
                    office.getName(),
                    office.getId().toString()
            );

            logger.info("Cabinet authentifié avec succès: {}", office.getName());

            return ResponseEntity.ok(new OfficeAuthResponseDTO(
                    true,
                    "Cabinet authentifié avec succès, veuillez vous authentifier",
                    office.getId(),
                    office.getName(),
                    tempToken
            ));

        } catch (Exception e) {
            logger.error("Erreur lors de l'authentification du cabinet", e);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                    new OfficeAuthResponseDTO(false, e.getMessage(), null, null, null)
            );
        }
    }

    @PostMapping("/user")
    public ResponseEntity<?> authenticateUser(@RequestBody AuthRequestDTO request,
                                              @RequestHeader("X-Office-Token") String officeToken) {
        try {
            logger.info("Tentative d'authentification utilisateur pour: {}", request.getEmail());

            // Vérifier si l'email est vide
            if (request.getEmail() == null || request.getEmail().trim().isEmpty()) {
                return ResponseEntity.badRequest().body(new AuthResponseDTO("L'adresse email est requise."));
            }

            // Vérifier si le mot de passe est vide
            if (request.getPassword() == null || request.getPassword().trim().isEmpty()) {
                return ResponseEntity.badRequest().body(new AuthResponseDTO("Le mot de passe est requis."));
            }

            // Extraire les informations du token temporaire
            String officeName = jwtUtil.extractUsername(officeToken);
            String officeId = jwtUtil.extractOfficeId(officeToken).toString();

            // Vérifier que le token temporaire est valide
            if (!officeAuthService.validateTempToken(officeToken, officeName)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new AuthResponseDTO("Token de cabinet invalide ou expiré."));
            }

            // Trouver l'utilisateur par email
            User user = userService.findByEmail(request.getEmail());

            if (user == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new AuthResponseDTO("Utilisateur introuvable."));
            }

            if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new AuthResponseDTO("Mot de passe incorrect."));
            }

            if (user.getOffice() == null || !user.getOffice().getId().toString().equals(officeId)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new AuthResponseDTO("Utilisateur non autorisé à accéder à ce cabinet."));
            }

            // Génération du token
            String token = jwtUtil.generateToken(
                    user.getEmail(),
                    user.getOffice().getId(),
                    user.getOffice().getName()
            );

            logger.info("✅ Utilisateur authentifié avec succès: {}", user.getEmail());

            // ✅ Retourne bien `userId` et `officeId`
            return ResponseEntity.ok(new AuthResponseDTO(
                    token,
                    user.getEmail(),
                    user.getRole().getName(),
                    user.getId().toString(),
                    user.getOffice().getId().toString()
            ));

        } catch (Exception e) {
            logger.error("❌ Erreur lors de l'authentification utilisateur", e);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new AuthResponseDTO("Une erreur est survenue lors de l'authentification."));
        }
    }

}