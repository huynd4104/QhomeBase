package com.QhomeBase.iamservice.config;

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
    public OpenAPI iamOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("QhomeBase IAM APIs")
                        .description("Identity & Access Management service APIs")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("QhomeBase IAM Team")
                                .email("iam@qhomebase.com"))
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
                                        .type(Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .in(In.HEADER)
                                        .name("Authorization")));
    }

    @Bean
    public GroupedOpenApi authApi() {
        return GroupedOpenApi.builder()
                .group("auth")
                .pathsToMatch("/api/auth/**")
                .build();
    }

    @Bean
    public GroupedOpenApi userManagementApi() {
        return GroupedOpenApi.builder()
                .group("users")
                .pathsToMatch(
                        "/api/users/**",
                        "/api/user-permissions/**",
                        "/api/user-grants/**")
                .build();
    }

    @Bean
    public GroupedOpenApi roleManagementApi() {
        return GroupedOpenApi.builder()
                .group("roles-permissions")
                .pathsToMatch(
                        "/api/roles/**",
                        "/api/permissions/**",
                        "/api/role-permissions/**",
                        "/api/employee-roles/**",
                        "/api/employees/**")
                .build();
    }
}

