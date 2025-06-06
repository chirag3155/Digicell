package com.api.digicell.config;

import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;

import java.util.List;

/**
 * Central configuration for Springdoc-OpenAPI (Swagger UI).
 *
 * Once the application is running, the documentation will be available at:
 *   • /swagger-ui/index.html (interactive UI)
 *   • /v3/api-docs          (raw JSON)
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Digicell API")
                        .version("1.0")
                        .description("API for Digicell Agent Management System")
                        .contact(new Contact()
                                .name("Digicell Support")
                                .email("support@digicell.com")
                                .url("https://digicell.com/support"))
                        .license(new License()
                                .name("Proprietary")
                                .url("https://digicell.com/license")))
                .servers(List.of(
                        new Server()
                                .url("https://eva-sandbox.bngrenew.com/digicel")
                                .description("Sandbox Server")
                ));
    }

    @Bean
    public GroupedOpenApi publicApi() {
        return GroupedOpenApi.builder()
                .group("public")
                .packagesToScan("com.api.digicell.controllers")
                .build();
    }
} 