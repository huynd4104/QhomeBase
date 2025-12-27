package com.QhomeBase.baseservice.config;

import io.swagger.v3.oas.models.ExternalDocumentation;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI baseServiceOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("QhomeBase Base Service APIs")
                        .description("Core master data and building management APIs")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("QhomeBase Platform Team")
                                .email("support@qhomebase.com"))
                        .license(new License()
                                .name("Private")
                                .url("https://qhomebase.com")))
                .externalDocs(new ExternalDocumentation()
                        .description("QhomeBase API Portal")
                        .url("https://docs.qhomebase.com"))
                .addSecurityItem(new SecurityRequirement().addList("BearerAuth"))
                .components(new io.swagger.v3.oas.models.Components()
                        .addSecuritySchemes("BearerAuth",
                                new SecurityScheme()
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .in(SecurityScheme.In.HEADER)
                                        .name("Authorization")));
    }

    @Bean
    public GroupedOpenApi managementApi() {
        return GroupedOpenApi.builder()
                .group("base-service")
                .pathsToMatch("/api/**")
                .build();
    }
}



















