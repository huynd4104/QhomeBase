package com.QhomeBase.datadocsservice.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "file.storage")
@Data
public class FileStorageProperties {
    
    private String location;
    private String baseUrl;
    private String gatewayUrl; // API Gateway URL for public access (e.g., http://localhost:8989)
}






