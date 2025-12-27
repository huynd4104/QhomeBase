package com.QhomeBase.iamservice.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

@Component
public class JwtVerifier {

    private final SecretKey key;
    private final String issuer;
    private final String expectedAudience;

    public JwtVerifier(
            @Value("${security.jwt.secret}") String secret,
            @Value("${security.jwt.issuer}") String issuer
    ) {
        byte[] raw = secret.getBytes(StandardCharsets.UTF_8);
        if (raw.length < 32)
            throw new IllegalStateException("JWT_SECRET must be >= 32 bytes");
        this.key = Keys.hmacShaKeyFor(raw);
        this.issuer = issuer;
        this.expectedAudience = null;
    }
    public Claims verify(String token) {
        var builder = Jwts.parserBuilder()
                .setSigningKey(key)
                .requireIssuer(issuer)
                .setAllowedClockSkewSeconds(Duration.ofSeconds(30).getSeconds());
        

        if (expectedAudience != null) {
            builder.requireAudience(expectedAudience);
        }
        
        return (Claims) builder.build()
                .parseClaimsJws(token)
                .getBody();
    }

}
