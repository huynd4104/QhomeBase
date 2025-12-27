package com.QhomeBase.customerinteractionservice.controller;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/test")
public class TestController {

    @Value("${security.jwt.secret}")
    private String jwtSecret;

    @Value("${security.jwt.issuer}")
    private String jwtIssuer;

    @Value("${security.jwt.audience}")
    private String jwtAudience;

    @PostMapping("/generate-token")
    public ResponseEntity<TokenResponse> generateTestToken(@RequestBody GenerateTokenRequest request) {
        SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));

        Instant now = Instant.now();
        Instant expiry = now.plus(24, ChronoUnit.HOURS);

        String token = Jwts.builder()
                .setIssuer(jwtIssuer)
                .setAudience(jwtAudience)
                .setSubject(request.getUsername())
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(expiry))
                .claim("uid", request.getUid())
                .claim("tenant", request.getTenant())
                .claim("roles", request.getRoles())
                .claim("perms", request.getPermissions())
                .signWith(key)
                .compact();

        TokenResponse response = new TokenResponse();
        response.setToken(token);
        response.setExpiresAt(expiry.toString());
        response.setUid(request.getUid());
        response.setUsername(request.getUsername());
        response.setTenant(request.getTenant());
        response.setRoles(request.getRoles());
        response.setPermissions(request.getPermissions());
        response.setTokenType("Bearer");

        return ResponseEntity.ok(response);
    }

    @GetMapping("/public")
    public ResponseEntity<Map<String, Object>> publicEndpoint() {
        return ResponseEntity.ok(Map.of(
                "message", "This is a public endpoint - no authentication required",
                "service", "customer-interaction-service",
                "timestamp", Instant.now().toString(),
                "status", "OK"
        ));
    }

    @GetMapping("/verify-token")
    public ResponseEntity<Map<String, Object>> verifyToken(
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.badRequest().body(Map.of(
                    "valid", false,
                    "error", "No Bearer token provided"
            ));
        }

        try {
            SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
            String token = authHeader.substring(7);
            
            var claims = Jwts.parserBuilder()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();

            return ResponseEntity.ok(Map.of(
                    "valid", true,
                    "subject", claims.getSubject(),
                    "issuer", claims.getIssuer(),
                    "audience", claims.getAudience(),
                    "issuedAt", claims.getIssuedAt().toInstant().toString(),
                    "expiresAt", claims.getExpiration().toInstant().toString(),
                    "uid", claims.get("uid"),
                    "tenant", claims.get("tenant"),
                    "roles", claims.get("roles"),
                    "permissions", claims.get("perms")
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "valid", false,
                    "error", e.getMessage()
            ));
        }
    }

    @Data
    public static class GenerateTokenRequest {
        private String uid;
        private String username;
        private String tenant;
        private List<String> roles;
        private List<String> permissions;
    }

    @Data
    public static class TokenResponse {
        private String token;
        private String tokenType;
        private String expiresAt;
        private String uid;
        private String username;
        private String tenant;
        private List<String> roles;
        private List<String> permissions;
    }
}































