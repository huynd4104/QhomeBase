package com.QhomeBase.marketplaceservice.config;

import io.swagger.v3.oas.models.ExternalDocumentation;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class SwaggerConfig {

    @Value("${swagger.api-gateway-url:http://localhost:8989/marketplace}")
    private String apiGatewayUrl;

    @Bean
    public OpenAPI marketplaceServiceOpenAPI() {
        Server apiGatewayServer = new Server();
        apiGatewayServer.setUrl(apiGatewayUrl);
        apiGatewayServer.setDescription("API Gateway URL (uses ngrok URL if VNPAY_BASE_URL is set)");

        Server localServer = new Server();
        localServer.setUrl("http://localhost:8089");
        localServer.setDescription("Local Development Server");

        return new OpenAPI()
                .info(new Info()
                        .title("Marketplace Service API")
                        .description("API for managing marketplace posts, comments, likes, and categories")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("QhomeBase Marketplace Team")
                                .email("support@qhomebase.com"))
                        .license(new License()
                                .name("Apache 2.0")
                                .url("https://www.apache.org/licenses/LICENSE-2.0.html")))
                .servers(List.of(apiGatewayServer, localServer))
                .externalDocs(new ExternalDocumentation()
                        .description("QhomeBase API Portal")
                        .url("https://docs.qhomebase.com"));
    }

    @Bean
    public GroupedOpenApi marketplaceApi() {
        return GroupedOpenApi.builder()
                .group("marketplace")
                .pathsToMatch("/**")
                .build();
    }
}

