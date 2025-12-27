package com.QhomeBase.baseservice.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Filter to bypass ngrok browser warning page by adding the required header.
 * This allows VNPay and other external services to call our endpoints without
 * being blocked by ngrok's browser warning.
 */
@Component
@Order(1) // Run before security filters
@Slf4j
public class NgrokSkipBrowserWarningFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {
        
        // Add header to bypass ngrok browser warning
        response.setHeader("ngrok-skip-browser-warning", "true");
        
        // Also set X-Frame-Options to allow iframe embedding (for payment gateways)
        response.setHeader("X-Frame-Options", "SAMEORIGIN");
        
        filterChain.doFilter(request, response);
    }
}

