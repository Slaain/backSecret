package myavocat.legit.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Date;
import java.util.UUID;

@Component
public class JwtUtil {

    @Value("${JWT_SECRET}")
    private String secretKey;  // La clé est stockée en Base64

    @Value("${JWT_EXPIRATION}")
    private long expirationTime; // En secondes

    // Expiration courte pour les tokens temporaires (5 minutes)
    private static final long TEMP_TOKEN_EXPIRATION = 300; // En secondes

    private SecretKey key;

    // Initialisation après injection des valeurs
    @PostConstruct
    public void init() {
        byte[] decodedKey = Base64.getDecoder().decode(secretKey);
        this.key = Keys.hmacShaKeyFor(decodedKey);
    }

    public String generateToken(String username) {
        return Jwts.builder()
                .setSubject(username)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + (expirationTime * 1000))) // Convertir en ms
                .signWith(key)
                .compact();
    }

    // Génère un token complet avec toutes les informations nécessaires
    public String generateToken(String username, UUID officeId, String officeName) {
        return Jwts.builder()
                .setSubject(username)
                .claim("officeId", officeId.toString())
                .claim("officeName", officeName)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + (expirationTime * 1000))) // Convertir en ms
                .signWith(key)
                .compact();
    }

    // Génère un token temporaire pour l'authentification du cabinet
    public String generateTempOfficeToken(String officeName, String officeId) {
        return Jwts.builder()
                .setSubject(officeName)
                .claim("officeId", officeId)
                .claim("type", "temp")
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + (TEMP_TOKEN_EXPIRATION * 1000))) // 5 minutes
                .signWith(key)
                .compact();
    }

    public boolean validateToken(String token, String username) {
        String tokenUsername = extractUsername(token);
        return (username.equals(tokenUsername) && !isTokenExpired(token));
    }

    // Valide un token temporaire de cabinet
    public boolean validateTempOfficeToken(String token, String officeName) {
        try {
            Claims claims = extractClaims(token);
            String tokenType = (String) claims.get("type");
            String tokenOfficeName = claims.getSubject();

            return "temp".equals(tokenType) &&
                    officeName.equals(tokenOfficeName) &&
                    !isTokenExpired(token);
        } catch (Exception e) {
            return false;
        }
    }

    public String extractUsername(String token) {
        return extractClaims(token).getSubject();
    }

    public UUID extractOfficeId(String token) {
        String officeIdStr = (String) extractClaims(token).get("officeId");
        return officeIdStr != null ? UUID.fromString(officeIdStr) : null;
    }

    public String extractOfficeName(String token) {
        return (String) extractClaims(token).get("officeName");
    }

    public boolean isTokenExpired(String token) {
        return extractClaims(token).getExpiration().before(new Date());
    }

    public Claims extractClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }
}