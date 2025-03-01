package myavocat.legit.service;

import myavocat.legit.model.Office;
import myavocat.legit.repository.OfficeRepository;
import myavocat.legit.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OfficeAuthService {

    @Autowired
    private OfficeRepository officeRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtil jwtUtil;

    /**
     * Authentifie un cabinet avec son nom et mot de passe
     * @param officeName Le nom du cabinet
     * @param officePassword Le mot de passe du cabinet
     * @return Le cabinet authentifié
     * @throws RuntimeException si l'authentification échoue
     */
    @Transactional(readOnly = true)
    public Office authenticateOffice(String officeName, String officePassword) {
        Office office = officeRepository.findByName(officeName)
                .orElseThrow(() -> new RuntimeException("Cabinet introuvable"));

        // Vérification du mot de passe
        if (!passwordEncoder.matches(officePassword, office.getPassword())) {
            throw new RuntimeException("Mot de passe du cabinet incorrect");
        }

        // Vérification de l'état du cabinet
        if (!office.isActif()) {
            throw new RuntimeException("Ce cabinet est inactif");
        }

        return office;
    }

    /**
     * Génère un token temporaire pour un cabinet
     * Ce token servira uniquement pour la seconde étape d'authentification
     */
    public String generateTempToken(String officeName, String officeId) {
        return jwtUtil.generateTempOfficeToken(officeName, officeId);
    }

    /**
     * Vérifie qu'un token temporaire est valide
     */
    public boolean validateTempToken(String token, String officeName) {
        return jwtUtil.validateTempOfficeToken(token, officeName);
    }
}