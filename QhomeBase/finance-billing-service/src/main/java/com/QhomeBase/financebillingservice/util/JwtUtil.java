package com.QhomeBase.financebillingservice.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

@Component
@Slf4j
public class JwtUtil {
    
    @Value("${security.jwt.secret:qhome-iam-secret-key-2024-very-long-and-secure-key-for-jwt-token-generation}")
    private String jwtSecret;
    
    @Value("${security.jwt.issuer:qhome-iam}")
    private String issuer;
    
    public UUID getUserIdFromToken(String token) {
        try {
            byte[] raw = jwtSecret.getBytes(StandardCharsets.UTF_8);
            if (raw.length < 32) {
                log.warn("JWT_SECRET must be >= 32 bytes");
                return null;
            }
            SecretKey key = Keys.hmacShaKeyFor(raw);
            
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(key)
                    .requireIssuer(issuer)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
            
            String uid = claims.get("uid", String.class);
            if (uid != null) {
                return UUID.fromString(uid);
            }
        } catch (Exception e) {
            log.error("Error parsing JWT token: {}", e.getMessage());
        }
        return null;
    }
    
    public UUID getUserIdFromHeader(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return null;
        }
        String token = authHeader.substring(7);
        return getUserIdFromToken(token);
    }
}

