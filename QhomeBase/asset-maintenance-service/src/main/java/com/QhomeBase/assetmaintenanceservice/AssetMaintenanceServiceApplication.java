package com.QhomeBase.assetmaintenanceservice;

import com.QhomeBase.assetmaintenanceservice.service.FileStorageService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class AssetMaintenanceServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(AssetMaintenanceServiceApplication.class, args);
    }

    @Bean
    CommandLineRunner init(FileStorageService fileStorageService) {
        return args -> {
            fileStorageService.init();
        };
    }
}
