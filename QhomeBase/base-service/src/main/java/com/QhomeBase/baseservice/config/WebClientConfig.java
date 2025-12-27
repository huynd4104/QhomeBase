package com.QhomeBase.baseservice.config;

import com.QhomeBase.baseservice.security.UserPrincipal;
import io.netty.channel.ChannelOption;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;

@Configuration
public class WebClientConfig {

    @Bean
    public WebClient iamWebClient(@Value("${iam.service.url:http://localhost:8088}") String iamServiceUrl) {
        // Configure HTTP client with timeout for DEV LOCAL mode
        // Increased timeouts to handle slow IAM service responses
        HttpClient httpClient = HttpClient.create()
                .responseTimeout(Duration.ofSeconds(30)) // 30 seconds timeout for IAM service (increased from 5s)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 10000); // 10 seconds connection timeout (increased from 3s)
        
        return WebClient.builder()
                .baseUrl(iamServiceUrl)
                .clientConnector(new ReactorClientHttpConnector(httpClient))
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

    @Bean
    public WebClient contractWebClient(@Value("${contract.service.url:http://localhost:8082}") String contractServiceUrl) {
        return WebClient.builder()
                .baseUrl(contractServiceUrl)
                .defaultHeader("Content-Type", "application/json")
                .filter(addJwtTokenFilter())
                .build();
    }


    private ExchangeFilterFunction addJwtTokenFilter() {
        return ExchangeFilterFunction.ofRequestProcessor(clientRequest -> {
            try {
                var auth = SecurityContextHolder.getContext().getAuthentication();
                if (auth != null && auth.getPrincipal() instanceof UserPrincipal principal) {
                    String token = principal.token();
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
