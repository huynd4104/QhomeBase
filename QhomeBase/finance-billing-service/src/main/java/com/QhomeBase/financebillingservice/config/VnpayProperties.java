package com.QhomeBase.financebillingservice.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "vnpay")
public class VnpayProperties {
    private String tmnCode;
    private String hashSecret;
    private String vnpUrl;
    private String baseUrl; // Base URL for building return URLs (e.g., https://xxx.ngrok.io)
    private String returnUrl; // Full return URL or will be built from baseUrl
    private String version;
    private String command;

    /**
     * Get return URL for invoices VNPay callback
     * If returnUrl is set, use it; otherwise build from baseUrl
     */
    public String getReturnUrl() {
        if (StringUtils.hasText(returnUrl)) {
            return returnUrl;
        }
        // Build from baseUrl if available
        if (StringUtils.hasText(baseUrl)) {
            // Remove trailing slash if present
            String base = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
            return base + "/api/invoices/vnpay/redirect";
        }
        return returnUrl; // Return null or empty if neither is set
    }
}


