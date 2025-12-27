package com.QhomeBase.financebillingservice.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;

import jakarta.servlet.http.HttpServletRequest;
import java.time.Duration;

@Configuration
public class webClientConfig {

    @Bean
    public WebClient webClient() {
        HttpClient httpClient = HttpClient.create()
                .responseTimeout(Duration.ofSeconds(10));

        // Increase buffer size to handle large JSON responses (default is 256KB)
        // Set to 2MB to handle large household member lists
        ExchangeStrategies strategies = ExchangeStrategies.builder()
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(2 * 1024 * 1024)) // 2MB
                .build();

        return WebClient.builder()
                .baseUrl("http://localhost:8081")
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .exchangeStrategies(strategies)
                .defaultHeader("Content-Type", "application/json")
                .defaultHeader("Accept", "application/json")
                .filter(addJwtTokenFilter()) // Add JWT token filter for authentication
                .build();
    }

    /**
     * Add JWT token from request header or SecurityContext to WebClient requests
     * This allows finance-billing-service to authenticate when calling base-service
     * Priority: 1. Request header (Authorization), 2. SecurityContext UserPrincipal, 3. SecurityContext credentials
     */
    private ExchangeFilterFunction addJwtTokenFilter() {
        return ExchangeFilterFunction.ofRequestProcessor(clientRequest -> {
            try {
                String token = null;
                
                // First, try to get token from current HTTP request header
                try {
                    ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
                    if (attributes != null) {
                        HttpServletRequest request = attributes.getRequest();
                        String authHeader = request.getHeader("Authorization");
                        if (authHeader != null && authHeader.startsWith("Bearer ")) {
                            token = authHeader.substring(7);
                        }
                    }
                } catch (Exception e) {
                    // RequestContextHolder might not be available in async context
                }
                
                // Fallback: try SecurityContext
                if (token == null || token.isEmpty()) {
                    var auth = SecurityContextHolder.getContext().getAuthentication();
                    if (auth != null && auth.getPrincipal() != null) {
                        try {
                            // Try to get token from UserPrincipal.token() method
                            var principal = auth.getPrincipal();
                            var principalClass = principal.getClass();
                            var tokenMethod = principalClass.getMethod("token");
                            token = (String) tokenMethod.invoke(principal);
                        } catch (Exception e) {
                            // Fallback: try to get from credentials
                            if (auth.getCredentials() != null) {
                                token = auth.getCredentials().toString();
                            }
                        }
                    }
                }
                
                if (token != null && !token.isEmpty()) {
                    ClientRequest newRequest = ClientRequest.from(clientRequest)
                            .header("Authorization", "Bearer " + token)
                            .build();
                    return Mono.just(newRequest);
                } else {
                    System.err.println("⚠️ [WebClientConfig] No token found for request to: " + clientRequest.url());
                }
            } catch (Exception e) {
                // Log error but don't fail the request
                System.err.println("❌ [WebClientConfig] Failed to add JWT token to request: " + e.getMessage());
            }
            return Mono.just(clientRequest);
        });
    }
}