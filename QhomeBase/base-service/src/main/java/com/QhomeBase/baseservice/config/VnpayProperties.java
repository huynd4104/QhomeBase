package com.QhomeBase.baseservice.config;

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
    private String maintenanceReturnUrl; // Full return URL for maintenance request payment

    private String version;
    private String command;

    /**
     * Get return URL for maintenance request VNPay callback
     * If maintenanceReturnUrl is set, use it; otherwise build from baseUrl
     */
    public String getMaintenanceReturnUrl() {
        if (StringUtils.hasText(maintenanceReturnUrl)) {
            return maintenanceReturnUrl;
        }
        if (StringUtils.hasText(baseUrl)) {
            String base = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
            return base + "/api/maintenance-requests/vnpay/redirect";
        }
        return returnUrl; // Fallback to default return URL
    }
}

