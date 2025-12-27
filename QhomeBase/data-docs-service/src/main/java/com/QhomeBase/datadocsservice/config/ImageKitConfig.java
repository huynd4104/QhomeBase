package com.QhomeBase.datadocsservice.config;

import io.imagekit.sdk.ImageKit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class ImageKitConfig {

    private final ImageKitProperties imageKitProperties;

    @Bean
    public ImageKit imageKit() {
        try {
        ImageKit imageKit = ImageKit.getInstance();
            
            // Validate configuration
            if (imageKitProperties.getPublicKey() == null || imageKitProperties.getPublicKey().isEmpty()) {
                log.error("❌ [ImageKitConfig] Public key is null or empty");
                throw new IllegalStateException("ImageKit public key is not configured");
            }
            if (imageKitProperties.getPrivateKey() == null || imageKitProperties.getPrivateKey().isEmpty()) {
                log.error("❌ [ImageKitConfig] Private key is null or empty");
                throw new IllegalStateException("ImageKit private key is not configured");
            }
            if (imageKitProperties.getUrlEndpoint() == null || imageKitProperties.getUrlEndpoint().isEmpty()) {
                log.error("❌ [ImageKitConfig] URL endpoint is null or empty");
                throw new IllegalStateException("ImageKit URL endpoint is not configured");
            }
            
        io.imagekit.sdk.config.Configuration config = new io.imagekit.sdk.config.Configuration(
                imageKitProperties.getPublicKey(),
                imageKitProperties.getPrivateKey(),
                imageKitProperties.getUrlEndpoint()
        );
        imageKit.setConfig(config);
            log.info("✅ [ImageKitConfig] ImageKit configured successfully");
            log.info("✅ [ImageKitConfig] Endpoint: {}", imageKitProperties.getUrlEndpoint());
            log.info("✅ [ImageKitConfig] Public key: {}...", 
                    imageKitProperties.getPublicKey().substring(0, Math.min(20, imageKitProperties.getPublicKey().length())));
        return imageKit;
        } catch (Exception e) {
            log.error("❌ [ImageKitConfig] Failed to configure ImageKit: {}", e.getMessage(), e);
            throw new IllegalStateException("Failed to configure ImageKit: " + e.getMessage(), e);
        }
    }
}
