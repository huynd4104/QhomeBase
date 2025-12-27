package com.QhomeBase.apigateway.filter;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Global filter to automatically bypass ngrok warning page
 * Adds ngrok-skip-browser-warning header to all requests when ngrok is detected
 */
@Component
public class NgrokWarningBypassFilter implements GlobalFilter, Ordered {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        
        // Check if request is coming through ngrok (via Host header or Referer)
        String host = request.getURI().getHost();
        boolean isNgrokRequest = host != null && (
            host.contains("ngrok-free.dev") || 
            host.contains("ngrok.io") ||
            host.contains("ngrok.app")
        );
        
        // Also check Referer header
        String referer = request.getHeaders().getFirst("Referer");
        if (!isNgrokRequest && referer != null) {
            isNgrokRequest = referer.contains("ngrok-free.dev") || 
                            referer.contains("ngrok.io") ||
                            referer.contains("ngrok.app");
        }
        
        if (isNgrokRequest) {
            // Add ngrok-skip-browser-warning header to bypass ngrok warning page
            ServerHttpRequest modifiedRequest = request.mutate()
                .header("ngrok-skip-browser-warning", "true")
                .build();
            
            return chain.filter(exchange.mutate().request(modifiedRequest).build());
        }
        
        return chain.filter(exchange);
    }

    @Override
    public int getOrder() {
        // Run early in the filter chain, before routing
        return -100;
    }
}

