package com.user.pdfservice.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Component
public class JwtUtils {

    private final SecretKey key;
    private final long expirationMillis;

    public JwtUtils(
            @Value("${jwt.secret:BankStatementSecretKey2026SuperSecureKeyWithEnoughLengthForHS256BitAlgorithm!}") String secret,
            @Value("${jwt.expiration-hours:24}") long expirationHours) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationMillis = expirationHours * 3600 * 1000;
    }

    public String generateToken(String userId, String username, String role, String sessionId) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);
        claims.put("role", role);
        claims.put("sessionId", sessionId);

        Date now = new Date();
        Date expiry = new Date(now.getTime() + expirationMillis);

        return Jwts.builder()
                .claims(claims)
                .subject(username)
                .issuedAt(now)
                .expiration(expiry)
                .signWith(key)
                .compact();
    }

    public Claims parseToken(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (Exception e) {
            return null;
        }
    }

    public boolean isTokenValid(String token) {
        Claims claims = parseToken(token);
        if (claims == null) return false;
        Date expiration = claims.getExpiration();
        return expiration != null && expiration.after(new Date());
    }

    public String getUsername(String token) {
        Claims claims = parseToken(token);
        return claims != null ? claims.getSubject() : null;
    }

    public String getSessionId(String token) {
        Claims claims = parseToken(token);
        return claims != null ? (String) claims.get("sessionId") : null;
    }

    public String getRole(String token) {
        Claims claims = parseToken(token);
        return claims != null ? (String) claims.get("role") : null;
    }

    public String getUserId(String token) {
        Claims claims = parseToken(token);
        return claims != null ? (String) claims.get("userId") : null;
    }
}

