package com.QhomeBase.chatservice.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.lang.NonNull;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Paths;

@Configuration
@Slf4j
public class WebConfig implements WebMvcConfigurer {

    @Value("${chat.upload.directory:uploads/chat}")
    private String uploadDirectory;

    @Override
    public void addResourceHandlers(@NonNull ResourceHandlerRegistry registry) {
        // Serve uploaded chat files
        String uploadPath = Paths.get(uploadDirectory).toAbsolutePath().toString();
        registry.addResourceHandler("/uploads/chat/**")
                .addResourceLocations("file:" + uploadPath + "/");
    }
}

