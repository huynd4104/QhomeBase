package com.QhomeBase.customerinteractionservice.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
@Component
public class JwtVerifier {
    private final SecretKey key;
    private final String issuer;
    private final String expectedAudience;
    
    public JwtVerifier(@Value("${security.jwt.secret}") String secret,
                       @Value("${security.jwt.issuer}") String issuer,
                       @Value("${security.jwt.audience}") String expectedAudience) {
        byte[] raw = secret.getBytes(StandardCharsets.UTF_8);
        if (raw.length < 32)
            throw new IllegalStateException("JWT_SECRET must be >= 32 bytes");
        this.key = Keys.hmacShaKeyFor(raw);
        this.issuer = issuer;
        this.expectedAudience = expectedAudience;
    }

    public Claims verify(String token) {
        Claims claims = (Claims) Jwts.parserBuilder()
                .setSigningKey(key)
                .requireIssuer(issuer)
                .setAllowedClockSkewSeconds(Duration.ofMinutes(5).getSeconds())
                .build()
                .parseClaimsJws(token)
                .getBody();
        
        String aud = claims.getAudience();
        if (aud != null) {
            boolean hasValidAudience = aud.contains("customer-service") || 
                                      aud.contains("base-service") || 
                                      aud.contains(expectedAudience);
            
            if (!hasValidAudience) {
                throw new io.jsonwebtoken.security.SecurityException(
                    "JWT audience does not include valid service identifier"
                );
            }
        }
        
        return claims;
    }
}
