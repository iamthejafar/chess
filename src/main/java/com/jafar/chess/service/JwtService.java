package com.jafar.chess.service;

import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.WeakKeyException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.Key;
import java.util.Date;
import java.time.Duration;

@Slf4j
@Service
public class JwtService {

    private static final String REVOKED_TOKEN_PREFIX = "auth:revoked:";

    private final StringRedisTemplate stringRedisTemplate;

    public JwtService(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${jwt.expiration:86400000}") // Default: 24 hours
    private Long jwtExpiration;

    /**
     * Generate JWT token for a user
     * @param userId User's unique identifier
     * @param email User's email (optional, can be null for guest users)
     * @return JWT token string
     */
    public String generateToken(String userId, String email) {
        try {
            Key signingKey = getSigningKey();

            JwtBuilder builder = Jwts.builder()
                    .setSubject(userId)
                    .setIssuedAt(new Date())
                    .setExpiration(new Date(System.currentTimeMillis() + jwtExpiration))
                    .claim("userId", userId);

            // Add email claim only if provided (not for guest users)
            if (email != null && !email.isBlank()) {
                builder.claim("email", email);
            }

            return builder
                    .signWith(signingKey, SignatureAlgorithm.HS256)
                    .compact();

        } catch (WeakKeyException e) {
            log.error("JWT secret is too weak. Use at least 32 bytes for HS256.", e);
            throw new IllegalStateException("JWT configuration error", e);
        } catch (Exception e) {
            log.error("Error generating JWT token", e);
            throw new RuntimeException("Failed to generate token", e);
        }
    }

    /**
     * Validate token and extract user ID
     * @param token JWT token
     * @return User ID if valid, null if invalid
     */
    public String validateTokenAndGetUserId(String token) {
        try {
            if (isTokenRevoked(token)) {
                log.warn("JWT token is revoked");
                return null;
            }

            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(getSigningKey())
                    .build()
                    .parseClaimsJws(token)
                    .getBody();

            return claims.get("userId", String.class);

        } catch (ExpiredJwtException e) {
            log.warn("JWT token expired: {}", e.getMessage());
            return null;
        } catch (UnsupportedJwtException e) {
            log.warn("Unsupported JWT token: {}", e.getMessage());
            return null;
        } catch (MalformedJwtException e) {
            log.warn("Malformed JWT token: {}", e.getMessage());
            return null;
        } catch (SignatureException e) {
            log.warn("Invalid JWT signature: {}", e.getMessage());
            return null;
        } catch (IllegalArgumentException e) {
            log.warn("JWT claims string is empty: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Revoke a token until its expiration time.
     * @param token JWT token to revoke
     */
    public void revokeToken(String token) {
        try {
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(getSigningKey())
                    .build()
                    .parseClaimsJws(token)
                    .getBody();

            Date expiration = claims.getExpiration();
            if (expiration == null) {
                return;
            }

            long ttlMillis = expiration.getTime() - System.currentTimeMillis();
            if (ttlMillis <= 0) {
                return;
            }

            stringRedisTemplate.opsForValue().set(revokedTokenKey(token), "true", Duration.ofMillis(ttlMillis));
        } catch (Exception e) {
            log.warn("Unable to revoke JWT token: {}", e.getMessage());
        }
    }

    /**
     * Extract email from token
     * @param token JWT token
     * @return Email if present, null otherwise
     */
    public String getEmailFromToken(String token) {
        try {
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(getSigningKey())
                    .build()
                    .parseClaimsJws(token)
                    .getBody();

            return claims.get("email", String.class);
        } catch (Exception e) {
            log.warn("Error extracting email from token: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Check if token is expired
     * @param token JWT token
     * @return true if expired, false otherwise
     */
    public boolean isTokenExpired(String token) {
        try {
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(getSigningKey())
                    .build()
                    .parseClaimsJws(token)
                    .getBody();

            return claims.getExpiration().before(new Date());
        } catch (Exception e) {
            return true;
        }
    }

    private boolean isTokenRevoked(String token) {
        try {
            Boolean revoked = stringRedisTemplate.hasKey(revokedTokenKey(token));
            return Boolean.TRUE.equals(revoked);
        } catch (Exception e) {
            log.warn("Unable to check JWT revocation status: {}", e.getMessage());
            return false;
        }
    }

    private String revokedTokenKey(String token) {
        return REVOKED_TOKEN_PREFIX + sha256Hex(token);
    }

    private String sha256Hex(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(hash.length * 2);
            for (byte b : hash) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (Exception e) {
            throw new IllegalStateException("Unable to hash JWT token", e);
        }
    }

    /**
     * Get signing key from configured secret
     * Supports plain text, base64, and base64url encoded secrets
     */
    private Key getSigningKey() {
        if (jwtSecret == null || jwtSecret.isBlank()) {
            throw new IllegalStateException("jwt.secret is not configured");
        }

        String secret = jwtSecret.trim();
        byte[] keyBytes;

        if (secret.startsWith("base64:")) {
            keyBytes = Decoders.BASE64.decode(secret.substring("base64:".length()));
        } else if (secret.startsWith("base64url:")) {
            keyBytes = Decoders.BASE64URL.decode(secret.substring("base64url:".length()));
        } else {
            keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        }

        return Keys.hmacShaKeyFor(keyBytes);
    }
}
