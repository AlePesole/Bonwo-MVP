package com.alessandropesole.bonwoapp.shared.infrastructure.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class JwtService {

    private static final String CLAIM_TYPE = "type";
    private static final String TYPE_ACCESS = "access";
    private static final String TYPE_REFRESH = "refresh";

    private final JwtProperties properties;

    public String generateToken(String subject, Map<String, Object> extraClaims) {
        Map<String, Object> claims = new HashMap<>(extraClaims);
        claims.put(CLAIM_TYPE, TYPE_ACCESS);
        return buildToken(subject, claims, properties.expirationMs(), null);
    }

    public String generateRefreshToken(String subject) {
        return buildToken(subject, Map.of(CLAIM_TYPE, TYPE_REFRESH),
                properties.refreshExpirationMs(), UUID.randomUUID().toString());
    }

    private String buildToken(String subject, Map<String, Object> claims, long expirationMs, String jti) {
        JwtBuilder builder = Jwts.builder()
                .claims(claims)
                .subject(subject)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expirationMs));
        if (jti != null) {
            builder.id(jti);
        }
        return builder.signWith(signingKey()).compact();
    }

    public String extractSubject(String token) {
        return parseClaims(token).getPayload().getSubject();
    }

    public String extractId(String token) {
        return parseClaims(token).getPayload().getId();
    }

    public Instant extractExpiration(String token) {
        return parseClaims(token).getPayload().getExpiration().toInstant();
    }

    public boolean isRefreshToken(String token) {
        return TYPE_REFRESH.equals(parseClaims(token).getPayload().get(CLAIM_TYPE, String.class));
    }

    public boolean isTokenValid(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            log.warn("Invalid JWT token: {}", e.getMessage());
            return false;
        }
    }

    private Jws<Claims> parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(signingKey())
                .build()
                .parseSignedClaims(token);
    }

    private SecretKey signingKey() {
        byte[] keyBytes = Decoders.BASE64.decode(
                java.util.Base64.getEncoder().encodeToString(
                        properties.secret().getBytes()
                )
        );
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
