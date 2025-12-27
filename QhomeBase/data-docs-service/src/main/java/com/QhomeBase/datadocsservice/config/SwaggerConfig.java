package com.QhomeBase.datadocsservice.config;

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
    public OpenAPI dataDocsOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("QhomeBase Data Docs APIs")
                        .description("Contract and document storage APIs")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("QhomeBase Platform Team")
                                .email("support@qhomebase.com"))
                        .license(new License()
                                .name("Private")
                                .url("https://qhomebase.com")));
    }

    @Bean
    public GroupedOpenApi dataDocsApi() {
        return GroupedOpenApi.builder()
                .group("data-docs")
                .pathsToMatch("/api/**")
                .build();
    }
}



















