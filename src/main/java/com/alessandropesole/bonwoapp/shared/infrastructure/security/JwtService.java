package com.alessandropesole.bonwoapp.shared.infrastructure.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class JwtService {

    private final JwtProperties properties;

    public String generateToken(String subject, Map<String, Object> extraClaims) {
        return buildToken(subject, extraClaims, properties.expirationMs());
    }

    public String generateRefreshToken(String subject) {
        return buildToken(subject, Map.of(), properties.refreshExpirationMs());
    }

    private String buildToken(String subject, Map<String, Object> claims, long expirationMs) {
        return Jwts.builder()
                .claims(claims)
                .subject(subject)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expirationMs))
                .signWith(signingKey())
                .compact();
    }

    public String extractSubject(String token) {
        return parseClaims(token).getPayload().getSubject();
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
