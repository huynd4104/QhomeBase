package com.QhomeBase.assetmaintenanceservice.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Configuration
public class WebClientConfig {

    @Bean
    public WebClient baseServiceWebClient(@Value("${base.service.url:http://localhost:8081}") String baseServiceUrl) {
        return WebClient.builder()
                .baseUrl(baseServiceUrl)
                .defaultHeader("Content-Type", "application/json")
                .filter(addJwtTokenFilter())
                .build();
    }

    @Bean
    public WebClient financeWebClient(@Value("${finance.billing.service.url:http://localhost:8085}") String financeServiceUrl) {
        return WebClient.builder()
                .baseUrl(financeServiceUrl)
                .defaultHeader("Content-Type", "application/json")
                .filter(addJwtTokenFilter())
                .build();
    }

    private ExchangeFilterFunction addJwtTokenFilter() {
        return ExchangeFilterFunction.ofRequestProcessor(clientRequest -> {
            try {
                var auth = SecurityContextHolder.getContext().getAuthentication();
                if (auth != null && auth.getPrincipal() != null) {
                    String token = null;
                    try {
                        var principalClass = auth.getPrincipal().getClass();
                        var tokenMethod = principalClass.getMethod("token");
                        token = (String) tokenMethod.invoke(auth.getPrincipal());
                    } catch (Exception ignored) {
                        if (auth.getCredentials() != null) {
                            token = auth.getCredentials().toString();
                        }
                    }
                    
                    if (token != null && !token.isEmpty()) {
                        ClientRequest newRequest = ClientRequest.from(clientRequest)
                                .header("Authorization", "Bearer " + token)
                                .build();
                        return Mono.just(newRequest);
                    }
                }
            } catch (Exception e) {
                System.err.println("Failed to add JWT token to request: " + e.getMessage());
            }
            return Mono.just(clientRequest);
        });
    }
}

