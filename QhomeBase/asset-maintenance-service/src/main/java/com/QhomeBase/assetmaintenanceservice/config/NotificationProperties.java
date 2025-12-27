package com.QhomeBase.assetmaintenanceservice.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@ConfigurationProperties(prefix = "notifications")
@Getter
@Setter
public class NotificationProperties {

    /**
     * Comma separated list of email recipients for successful service booking payments.
     */
    private List<String> serviceBookingSuccessRecipients = new ArrayList<>();

    /**
     * Optional CC recipients for service booking payment emails.
     */
    private List<String> serviceBookingSuccessCc = new ArrayList<>();
}

