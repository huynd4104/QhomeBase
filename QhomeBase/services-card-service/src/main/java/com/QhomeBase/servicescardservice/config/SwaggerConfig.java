package com.QhomeBase.servicescardservice.config;

import io.swagger.v3.oas.models.ExternalDocumentation;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI servicesCardOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("QhomeBase Services Card APIs")
                        .description("Card registration and service request APIs")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("QhomeBase Platform Team")
                                .email("support@qhomebase.com"))
                        .license(new License()
                                .name("Private")
                                .url("https://qhomebase.com")))
                .externalDocs(new ExternalDocumentation()
                        .description("QhomeBase API Portal")
                        .url("https://docs.qhomebase.com"));
    }

    @Bean
    public GroupedOpenApi servicesCardApi() {
        return GroupedOpenApi.builder()
                .group("services-card")
                .pathsToMatch("/api/**")
                .build();
    }
}



















