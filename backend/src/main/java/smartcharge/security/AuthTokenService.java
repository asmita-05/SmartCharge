package smartcharge.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Date;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class AuthTokenService {
    private static final long TOKEN_LIFETIME_SECONDS = 6 * 60 * 60;
    private final SecretKey signingKey;
    private final Map<String, Instant> revokedTokens = new ConcurrentHashMap<>();

    public AuthTokenService(@Value("${smartcharge.auth.jwt-secret:}") String configuredSecret) {
        byte[] keyBytes;
        if (configuredSecret != null && !configuredSecret.isBlank()) {
            keyBytes = configuredSecret.getBytes(StandardCharsets.UTF_8);
            if (keyBytes.length < 32) {
                throw new IllegalStateException("SMARTCHARGE_JWT_SECRET must contain at least 32 UTF-8 bytes");
            }
        } else {
            byte[] generated = new byte[32];
            new SecureRandom().nextBytes(generated);
            keyBytes = generated;
        }
        this.signingKey = Keys.hmacShaKeyFor(keyBytes);
    }

    public String issue(String email, String role) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(email)
                .claim("role", role)
                .id(UUID.randomUUID().toString())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(TOKEN_LIFETIME_SECONDS)))
                .signWith(signingKey)
                .compact();
    }

    public TokenIdentity verify(String token) {
        Claims claims = Jwts.parser().verifyWith(signingKey).build()
                .parseSignedClaims(token).getPayload();
        String tokenId = claims.getId();
        Instant expiration = claims.getExpiration().toInstant();
        revokedTokens.entrySet().removeIf(entry -> !entry.getValue().isAfter(Instant.now()));
        if (tokenId == null || revokedTokens.containsKey(tokenId)) {
            throw new IllegalArgumentException("Token is revoked");
        }
        return new TokenIdentity(claims.getSubject(), claims.get("role", String.class), tokenId, expiration);
    }

    public void revoke(String token) {
        try {
            TokenIdentity identity = verify(token);
            revokedTokens.put(identity.tokenId(), identity.expiresAt());
        } catch (RuntimeException ignored) {
            // Invalid or expired tokens are already unusable.
        }
    }

    public record TokenIdentity(String email, String role, String tokenId, Instant expiresAt) { }
}
