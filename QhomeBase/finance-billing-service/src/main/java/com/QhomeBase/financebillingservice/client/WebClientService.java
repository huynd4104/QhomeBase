package com.QhomeBase.financebillingservice.client;

import com.QhomeBase.financebillingservice.dto.BuildingDto;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.UUID;

@Service
@Slf4j
public class WebClientService {


    private WebClient webClient;

    // @EventListener(ApplicationReadyEvent.class)
    // public void testWebClient() {
    //     webClient = WebClient.builder()
    //             .baseUrl("http://localhost:8081")
    //             .defaultHeader("Accept", "application/json")
    //             .build();
    //     log.info("=== TESTING WEBCLIENT ===");

    //     // Generate a test JWT token for testing
    //     String testToken = generateTestToken();
    //     log.info("Generated test token: {}", testToken.substring(0, 50) + "...");

    //     UUID tenantId = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");

    //     try {
    //         List<BuildingDto> buildings = getAllBuildings(tenantId, testToken).collectList().block();
    //         log.info("Buildings found: {}", buildings.size());
    //         buildings.forEach(building -> log.info("Building: {} - {}", building.getCode(), building.getName()));
    //     } catch (Exception e) {
    //         log.error("Error testing WebClient: {}", e.getMessage());
    //     }
    // }

    public Flux<BuildingDto> getAllBuildings(UUID tenantId, String token) {
        return webClient.get()
                .uri("/api/buildings?tenantId={tenantId}", tenantId)
                .header("Authorization", "Bearer " + token)
                .retrieve()
                .bodyToFlux(BuildingDto.class)
                .doOnNext(building -> log.info("Received building: {}", building.getName()))
                .doOnError(error -> log.error("Error fetching buildings: {}", error.getMessage()));
    }

    public Flux<BuildingDto> getAllBuildings(UUID tenantId) {
        return getAllBuildings(tenantId, "");
    }

    public Mono<BuildingDto> getBuildingById(UUID buildingId) {
        return webClient.get()
                .uri("/api/buildings/{id}", buildingId)
                .retrieve()
                .bodyToMono(BuildingDto.class);
    }

    private String generateTestToken() {
        String secret = "qhome-iam-secret-key-2024-very-long-and-secure-key-for-jwt-token-generation";
        String issuer = "qhome-iam";
        String audience = "qhome-base";
        
        SecretKey key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        
        return Jwts.builder()
                .setIssuer(issuer)
                .setSubject("testuser")
                .setId(UUID.randomUUID().toString())
                .setIssuedAt(Date.from(Instant.now()))
                .setExpiration(Date.from(Instant.now().plusSeconds(3600))) // 1 hour
                .setAudience(audience)
                .claim("uid", "550e8400-e29b-41d4-a716-446655440000")
                .claim("tenant", "123e4567-e89b-12d3-a456-426614174000")
                .claim("roles", List.of("tenant_manager"))
                .claim("perms", List.of("base.tenant.delete.request", "base.tenant.delete.approve"))
                .signWith(key)
                .compact();
    }
}