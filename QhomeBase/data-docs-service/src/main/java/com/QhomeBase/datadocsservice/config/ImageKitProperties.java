package com.QhomeBase.datadocsservice.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "imagekit")
@Data
public class ImageKitProperties {
    private String privateKey;
    private String publicKey;
    private String urlEndpoint;
}
