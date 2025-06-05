package com.api.digicell.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.License;

/**
 * Central configuration for Springdoc-OpenAPI (Swagger UI).
 *
 * Once the application is running, the documentation will be available at:
 *   • http://localhost:8080/swagger-ui/index.html (interactive UI)
 *   • http://localhost:8080/v3/api-docs             (raw JSON)
 */
@OpenAPIDefinition(
        info = @Info(
                title = "Digicell Support API",
                description = "REST APIs for Digicell agent–user conversation system.",
                version = "1.0.0",
                contact = @Contact(name = "Digicell Dev Team", email = "support@digicell.com"),
                license = @License(name = "Apache 2.0", url = "https://www.apache.org/licenses/LICENSE-2.0.html")
        )
)
public class OpenApiConfig {
    // No implementation needed; annotations provide the configuration.
} 