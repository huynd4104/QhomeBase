package com.QhomeBase.servicescardservice.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.lang.NonNull;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Paths;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(@NonNull ResourceHandlerRegistry registry) {
        // Get absolute path to uploads directory
        String uploadPath = Paths.get("uploads").toAbsolutePath().toUri().toString();
        // Ensure path ends with /
        if (!uploadPath.endsWith("/")) {
            uploadPath += "/";
        }
        
        // Handle /uploads/** path
        // API Gateway will rewrite /api/uploads/** to /uploads/** before forwarding
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations(uploadPath)
                .setCachePeriod(3600);
    }
}


