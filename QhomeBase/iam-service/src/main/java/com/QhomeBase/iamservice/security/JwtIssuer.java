package com.QhomeBase.iamservice.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.Nullable;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

@Service
public class JwtIssuer {
    private final SecretKey key;
    private final String issuer;
    private final long ttlMinutes;

    public JwtIssuer(
            @Value("${security.jwt.secret}") String secret,
            @Value("${security.jwt.issuer}") String issuer,
            @Value("${security.jwt.accessTtlMinutes}") long ttlMinutes

    ) {
        byte[] raw = secret.getBytes(StandardCharsets.UTF_8);
        if (raw.length < 32) throw new IllegalStateException("JWT_SECRET must be >= 32 bytes");
        this.key = Keys.hmacShaKeyFor(raw);
        this.issuer = issuer;
        this.ttlMinutes = ttlMinutes;

    }
    public String issueForService ( UUID uid,
                                    String username,
                                    String jti,
                                    List<String> roles,
                                    List<String> perms,
                                    String audiences) {
        var builder = Jwts.builder();
        builder.setIssuer(issuer).setSubject(username)
                .setId(UUID.randomUUID().toString())
                .setIssuedAt(Date.from(Instant.now()))
                .setExpiration(new Date(System.currentTimeMillis() + ttlMinutes*60*1000))
                .setAudience(audiences)
                .claim("uid", uid.toString());
        builder.claim("roles", new ArrayList<>(roles));
        builder.claim("perms", new ArrayList<>(perms));
        return builder.signWith(key, SignatureAlgorithm.HS256).compact();
    }
}
