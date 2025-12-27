package com.QhomeBase.baseservice.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
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
        
        // Validate audience - handle both String and List<String>
        if (!isAudienceValid(claims)) {
            throw new io.jsonwebtoken.security.SecurityException(
                "JWT audience does not include " + expectedAudience
            );
        }
        
        return claims;
    }
    
    private boolean isAudienceValid(Claims claims) {
        Object audClaim = claims.get("aud");
        
        // No audience claim - skip validation for now (lenient mode)
        if (audClaim == null) {
            return true;
        }
        
        // Handle String audience
        if (audClaim instanceof String) {
            String audString = (String) audClaim;
            
            // Check exact match first
            if (expectedAudience.equals(audString)) {
                return true;
            }
            
            // Handle comma-separated audiences: "base-service,finance-service,..."
            if (audString.contains(",")) {
                String[] audiences = audString.split(",");
                for (String aud : audiences) {
                    if (expectedAudience.equals(aud.trim())) {
                        return true;
                    }
                }
            }
            
            return false;
        }
        
        // Handle List<String> audience
        if (audClaim instanceof List) {
            @SuppressWarnings("unchecked")
            List<String> audiences = (List<String>) audClaim;
            return audiences.contains(expectedAudience);
        }
        
        // Unknown audience type
        return false;
    }
}
