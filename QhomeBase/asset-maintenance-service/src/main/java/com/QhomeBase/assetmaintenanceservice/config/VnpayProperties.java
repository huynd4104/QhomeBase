package com.QhomeBase.assetmaintenanceservice.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@ConfigurationProperties(prefix = "vnpay")
@Getter
@Setter
public class VnpayProperties {

    private String tmnCode;
    private String hashSecret;
    private String vnpUrl;
    private String baseUrl; 
    private String returnUrl; 
    private String serviceBookingReturnUrl;
    private String version;
    private String command;

    public String getReturnUrl() {
        if (StringUtils.hasText(returnUrl)) {
            return returnUrl;
        }
        if (StringUtils.hasText(baseUrl)) {
            String base = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
            return base + "/api/asset-maintenance/bookings/vnpay/redirect";
        }
        return returnUrl;
    }


    public String getServiceBookingReturnUrl() {
        if (StringUtils.hasText(serviceBookingReturnUrl)) {
            return serviceBookingReturnUrl;
        }
        // Build from baseUrl if available
        if (StringUtils.hasText(baseUrl)) {
            String base = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
            return base + "/api/asset-maintenance/bookings/vnpay/redirect";
        }
        return serviceBookingReturnUrl; // Return null or empty if neither is set
    }
}

