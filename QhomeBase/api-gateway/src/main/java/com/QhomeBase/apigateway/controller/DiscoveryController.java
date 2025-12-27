package com.QhomeBase.apigateway.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

/**
 * Discovery Controller - Simplified for DEV LOCAL mode
 * Flutter app uses fixed baseUrl from AppConfig, but this endpoint is kept for compatibility
 * Only returns VNPAY_BASE_URL for monitoring/debugging purposes
 */
@RestController
public class DiscoveryController {
    
    @GetMapping("/")
    public ResponseEntity<Map<String, Object>> root() {
        Map<String, Object> info = new HashMap<>();
        info.put("service", "QhomeBase API Gateway");
        info.put("status", "running");
        info.put("version", "1.0.0");
        info.put("mode", "DEV_LOCAL");
        Map<String, String> endpoints = new HashMap<>();
        endpoints.put("discovery", "/api/discovery/info");
        endpoints.put("health", "/api/health");
        info.put("endpoints", endpoints);
        return ResponseEntity.ok(info);
    }

    /**
     * Handle favicon.ico requests to avoid 404 logs
     * Returns 204 No Content (no favicon file, but no error)
     */
    @GetMapping("/favicon.ico")
    public ResponseEntity<Void> favicon() {
        return ResponseEntity.noContent().build();
    }

    @Value("${vnpay.base-url:}")
    private String vnpayBaseUrl;

    @Value("${server.port:8989}")
    private int serverPort;

    /**
     * Get backend discovery information (simplified for DEV LOCAL mode)
     * Flutter app uses fixed baseUrl from AppConfig, so this endpoint is mainly for monitoring
     * Only returns VNPAY_BASE_URL if set (for VNPay callbacks - backend-only)
     */
    @GetMapping("/api/discovery/info")
    public ResponseEntity<Map<String, Object>> getDiscoveryInfo() {
        Map<String, Object> info = new HashMap<>();
        
        // DEV LOCAL mode: Only return VNPAY_BASE_URL for monitoring/debugging
        // Flutter app doesn't use this for connection - it uses fixed baseUrl from AppConfig
        String publicUrl = null;
        
        if (vnpayBaseUrl != null && !vnpayBaseUrl.isEmpty() && !vnpayBaseUrl.contains("your-ngrok-url")) {
            if (isValidUrl(vnpayBaseUrl)) {
                publicUrl = vnpayBaseUrl.trim();
            }
        }
        
        info.put("publicUrl", publicUrl); // VNPAY_BASE_URL if set (for VNPay callbacks only)
        info.put("localPort", serverPort);
        info.put("apiBasePath", "/api");
        info.put("discoveryMethod", "fixed");
        info.put("mode", "DEV_LOCAL");
        info.put("note", "Flutter app uses fixed baseUrl from AppConfig. VNPay callbacks use VNPAY_BASE_URL env var.");
        
        return ResponseEntity.ok(info);
    }
    
    /**
     * Validate if URL is in correct format
     */
    private boolean isValidUrl(String url) {
        if (url == null || url.isEmpty() || url.trim().isEmpty()) {
            return false;
        }
        try {
            String trimmed = url.trim();
            if (!trimmed.startsWith("http://") && !trimmed.startsWith("https://")) {
                return false;
            }
            if (trimmed.length() < 10) {
                return false;
            }
            URI uri = URI.create(trimmed);
            return uri.getHost() != null && !uri.getHost().isEmpty();
        } catch (Exception e) {
            return false;
        }
    }

}
