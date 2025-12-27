package com.QhomeBase.marketplaceservice.config;

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
        ImageKit imageKit = ImageKit.getInstance();
        io.imagekit.sdk.config.Configuration config = new io.imagekit.sdk.config.Configuration(
                imageKitProperties.getPublicKey(),
                imageKitProperties.getPrivateKey(),
                imageKitProperties.getUrlEndpoint()
        );
        imageKit.setConfig(config);
        log.info("✅ ImageKit configured with endpoint: {}", imageKitProperties.getUrlEndpoint());
        return imageKit;
    }
}
