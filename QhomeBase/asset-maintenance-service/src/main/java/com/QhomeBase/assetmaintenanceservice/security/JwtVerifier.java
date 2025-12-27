package com.QhomeBase.assetmaintenanceservice.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Component
public class JwtVerifier {
    private final SecretKey key;
    private final String issuer;
    private final Set<String> acceptedAudiences;
    
    public JwtVerifier(@Value("${security.jwt.secret}") String secret,
                       @Value("${security.jwt.issuer}") String issuer,
                       @Value("${security.jwt.audience:}") String audienceProperty) {
        byte[] raw = secret.getBytes(StandardCharsets.UTF_8);
        if (raw.length < 32)
            throw new IllegalStateException("JWT_SECRET must be >= 32 bytes");
        this.key = Keys.hmacShaKeyFor(raw);
        this.issuer = issuer;
        this.acceptedAudiences = parseAudiences(audienceProperty);
    }

    public Claims verify(String token) {
        Claims claims = (Claims) Jwts.parserBuilder()
                .setSigningKey(key)
                .requireIssuer(issuer)
                .setAllowedClockSkewSeconds(Duration.ofMinutes(5).getSeconds())
                .build()
                .parseClaimsJws(token)
                .getBody();
        
        if (!isAudienceValid(claims)) {
            throw new io.jsonwebtoken.security.SecurityException(
                    "JWT audience is not in accepted list: " + acceptedAudiences
            );
        }
        
        return claims;
    }
    
    private boolean isAudienceValid(Claims claims) {
        Object audClaim = claims.get("aud");
        
        if (acceptedAudiences.isEmpty() || audClaim == null) {
            return true;
        }

        if (audClaim instanceof String audString) {
            return extractAudiences(audString).stream().anyMatch(acceptedAudiences::contains);
        }

        if (audClaim instanceof List<?>) {
            @SuppressWarnings("unchecked")
            List<String> audiences = (List<String>) audClaim;
            return audiences.stream()
                    .filter(aud -> aud != null && !aud.isBlank())
                    .map(String::trim)
                    .anyMatch(acceptedAudiences::contains);
        }

        return true;
    }

    private Set<String> parseAudiences(String property) {
        Set<String> result = new HashSet<>();
        if (property == null || property.isBlank()) {
            return result;
        }
        extractAudiences(property).forEach(result::add);
        return result;
    }

    private List<String> extractAudiences(String value) {
        return Arrays.stream(value.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
    }
}










