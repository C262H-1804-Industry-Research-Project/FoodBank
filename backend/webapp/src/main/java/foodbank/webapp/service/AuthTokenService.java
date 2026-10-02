package foodbank.webapp.service;

import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import io.jsonwebtoken.Claims;

@Service
public class AuthTokenService {

    @Value("${JWT_SECRET}")
    private String jwtSecret;

    @Value("${JWT_EXPIRATION_MS:3600000}")
    private long jwtExpirationMs;

    private final Map<String, ActiveToken> activeTokens =
            new ConcurrentHashMap<>();

    public String createToken(Integer userId, String role) {
        Date issuedAt = new Date();
        Date expiresAt = new Date(
                issuedAt.getTime() + jwtExpirationMs
        );

        String token = Jwts.builder()
                .subject(userId.toString())
                .claim("role", role)
                .id(UUID.randomUUID().toString())
                .issuedAt(issuedAt)
                .expiration(expiresAt)
                .signWith(signingKey())
                .compact();

        activeTokens.put(
                token,
                new ActiveToken(userId, role)
        );

        return token;
    }

    public boolean isActive(String token) {
        if (!activeTokens.containsKey(token)) {
            return false;
        }

        try {
            Jwts.parser()
                    .verifyWith(signingKey())
                    .build()
                    .parseSignedClaims(token);

            return true;
        } catch (JwtException | IllegalArgumentException exception) {
            activeTokens.remove(token);
            return false;
        }
    }

    public boolean revokeToken(String token) {
        return activeTokens.remove(token) != null;
    }
    public void revokeTokensForUser(Integer userId) {
        activeTokens.entrySet().removeIf(entry -> {
            ActiveToken activeToken = entry.getValue();

            boolean sameUser =
                    activeToken.userId().equals(userId);

            boolean userRole =
                    activeToken.role().equalsIgnoreCase("donor")
                            || activeToken.role().equalsIgnoreCase(
                            "recipient"
                    );

            return sameUser && userRole;
        });
    }
    public Claims getClaims(String token) {
        return Jwts.parser()
                .verifyWith(signingKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private SecretKey signingKey() {
        byte[] keyBytes = Decoders.BASE64.decode(jwtSecret);
        return Keys.hmacShaKeyFor(keyBytes);
    }
    private record ActiveToken(
            Integer userId,
            String role
    ) {
    }
}