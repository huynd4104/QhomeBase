package com.QhomeBase.assetmaintenanceservice.config;

import io.swagger.v3.oas.models.ExternalDocumentation;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.security.SecurityScheme.In;
import io.swagger.v3.oas.models.security.SecurityScheme.Type;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI assetMaintenanceOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("QhomeBase Asset Maintenance APIs")
                        .description("API specification for asset maintenance and resident service management")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("QhomeBase Platform Team")
                                .email("support@qhomebase.com"))
                        .license(new License().name("Private").url("https://qhomebase.com")))
                .externalDocs(new ExternalDocumentation()
                        .description("API Portal")
                        .url("https://docs.qhomebase.com"))
                .addSecurityItem(new SecurityRequirement().addList("BearerAuth"))
                .components(new io.swagger.v3.oas.models.Components()
                        .addSecuritySchemes("BearerAuth",
                                new SecurityScheme()
                                        .type(Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .in(In.HEADER)
                                        .name("Authorization")));
    }

    @Bean
    public GroupedOpenApi serviceCatalogApi() {
        return GroupedOpenApi.builder()
                .group("service-catalog")
                .pathsToMatch(
                        "/api/asset-maintenance/service-categories/**",
                        "/api/asset-maintenance/services/**",
                        "/api/asset-maintenance/service-options/**",
                        "/api/asset-maintenance/services/*/options/**",
                        "/api/asset-maintenance/service-combos/**",
                        "/api/asset-maintenance/services/*/combos/**",
                        "/api/asset-maintenance/service-tickets/**",
                        "/api/asset-maintenance/services/*/tickets/**"
                )
                .build();
    }

    @Bean
    public GroupedOpenApi bookingApi() {
        return GroupedOpenApi.builder()
                .group("service-booking")
                .pathsToMatch(
                        "/api/asset-maintenance/bookings/**",
                        "/api/asset-maintenance/admin/bookings/**",
                        "/api/asset-maintenance/services/*/booking/**",
                        "/api/asset-maintenance/services/*/availability/**"
                )
                .build();
    }
}

