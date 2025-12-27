package com.QhomeBase.financebillingservice.config;

import org.flywaydb.core.Flyway;
import org.springframework.boot.autoconfigure.flyway.FlywayMigrationStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FlywayRepairConfig {

    @Bean
    public FlywayMigrationStrategy flywayRepairThenMigrate() {
        return (Flyway flyway) -> {
            // Repair schema history to fix checksum mismatches, then migrate
            flyway.repair();
            flyway.migrate();
        };
    }
}


