package com.QhomeBase.servicescardservice.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.UUID;

@Component
@Slf4j
public class JwtUtil {

    @Value("${services-card.jwt.secret:qhome-base-secret-key-2024}")
    private String jwtSecret;

    @Value("${services-card.jwt.issuer:qhome-iam}")
    private String issuer;

    public UUID getUserIdFromHeader(String authHeader) {
        return parseToken(extractToken(authHeader));
    }

    public UUID getUserIdFromHeaders(HttpHeaders headers) {
        return parseToken(extractToken(headers.getFirst(HttpHeaders.AUTHORIZATION)));
    }

    private String extractToken(String header) {
        if (header == null || header.isBlank()) {
            return null;
        }
        if (header.toLowerCase().startsWith("bearer ")) {
            return header.substring(7);
        }
        return header;
    }

    private UUID parseToken(String token) {
        if (token == null || token.isBlank()) {
            return null;
        }
        try {
            byte[] raw = jwtSecret.getBytes(StandardCharsets.UTF_8);
            if (raw.length < 32) {
                log.warn("JWT secret must be at least 32 bytes");
                return null;
            }
            SecretKey key = Keys.hmacShaKeyFor(raw);

            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(key)
                    .requireIssuer(issuer)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();

            Object uidClaim = Optional.ofNullable(claims.get("uid"))
                    .orElse(claims.get("userId"));
            if (uidClaim == null) {
                return null;
            }
            return UUID.fromString(uidClaim.toString());
        } catch (Exception e) {
            log.error("Error parsing JWT token: {}", e.getMessage());
            return null;
        }
    }
}


