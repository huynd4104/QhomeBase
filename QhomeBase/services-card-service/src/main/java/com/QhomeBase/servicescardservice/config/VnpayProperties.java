package com.QhomeBase.servicescardservice.config;

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
    private String elevatorReturnUrl; // Full return URL or will be built from baseUrl
    private String residentReturnUrl; // Full return URL or will be built from baseUrl
    private String version;
    private String command;

    /**
     * Get return URL for vehicle registration VNPay callback
     * If returnUrl is set, use it; otherwise build from baseUrl
     */
    public String getReturnUrl() {
        if (StringUtils.hasText(returnUrl)) {
            return returnUrl;
        }
        if (StringUtils.hasText(baseUrl)) {
            String base = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
            return base + "/api/register-service/vnpay/redirect";
        }
        return returnUrl;
    }

    /**
     * Get return URL for resident card VNPay callback
     */
    public String getResidentReturnUrl() {
        if (StringUtils.hasText(residentReturnUrl)) {
            return residentReturnUrl;
        }
        if (StringUtils.hasText(baseUrl)) {
            String base = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
            return base + "/api/resident-card/vnpay/redirect";
        }
        return getReturnUrl(); // Fallback to default return URL
    }

    /**
     * Get return URL for elevator card VNPay callback
     */
    public String getElevatorReturnUrl() {
        if (StringUtils.hasText(elevatorReturnUrl)) {
            return elevatorReturnUrl;
        }
        if (StringUtils.hasText(baseUrl)) {
            String base = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
            return base + "/api/elevator-card/vnpay/redirect";
        }
        return getReturnUrl(); // Fallback to default return URL
    }
}


