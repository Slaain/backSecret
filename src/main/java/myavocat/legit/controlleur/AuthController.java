package myavocat.legit.controller;

import myavocat.legit.dto.AuthRequestDTO;
import myavocat.legit.dto.AuthResponseDTO;
import myavocat.legit.dto.AuthErrorDTO;
import myavocat.legit.model.User;
import myavocat.legit.service.UserService;
import myavocat.legit.util.JwtUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

    @Autowired
    private UserService userService;

    @Autowired
    private BCryptPasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtil jwtUtil;

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody AuthRequestDTO loginRequest) {
        logger.info("Login attempt for email: {}", loginRequest.getEmail());

        // Vérifier si l'email est vide
        if (loginRequest.getEmail() == null || loginRequest.getEmail().trim().isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(new AuthErrorDTO("Email is required", "MISSING_EMAIL", HttpStatus.BAD_REQUEST.value()));
        }

        // Vérifier si le mot de passe est vide
        if (loginRequest.getPassword() == null || loginRequest.getPassword().trim().isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(new AuthErrorDTO("Password is required", "MISSING_PASSWORD", HttpStatus.BAD_REQUEST.value()));
        }

        // Trouver l'utilisateur
        User user = userService.findByEmail(loginRequest.getEmail());
        if (user == null || !passwordEncoder.matches(loginRequest.getPassword(), user.getPassword())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new AuthErrorDTO("Invalid email or password", "INVALID_CREDENTIALS", HttpStatus.UNAUTHORIZED.value()));
        }

        // Générer le token JWT
        String token = jwtUtil.generateToken(user.getEmail());
        logger.info("Login successful for email: {}", loginRequest.getEmail());

        return ResponseEntity.ok(new AuthResponseDTO(token, user.getEmail(), user.getRole().getName()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<?> handleException(Exception e) {
        logger.error("An error occurred", e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new AuthErrorDTO("An internal error occurred", "INTERNAL_ERROR", HttpStatus.INTERNAL_SERVER_ERROR.value()));
    }
}
