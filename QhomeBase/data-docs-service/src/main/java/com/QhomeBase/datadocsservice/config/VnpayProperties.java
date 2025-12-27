package com.QhomeBase.datadocsservice.config;

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
    private String contractRenewalReturnUrl; // Full return URL for contract renewal
    private String version;
    private String command;

    /**
     * Get return URL for contract renewal VNPay callback
     */
    public String getContractRenewalReturnUrl() {
        if (StringUtils.hasText(contractRenewalReturnUrl)) {
            return contractRenewalReturnUrl;
        }
        if (StringUtils.hasText(baseUrl)) {
            String base = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
            return base + "/api/contracts/vnpay/callback";
        }
        return returnUrl; // Fallback to default return URL
    }
}
