package com.ecommerce.auth_service.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

        @Value("${server.port:8088}")
        private String serverPort;

        @Bean
        public OpenAPI authServiceOpenAPI() {
                return new OpenAPI()
                                .info(new Info()
                                                .title("Auth Service API")
                                                .description("Authentication and Authorization Service for E-commerce System")
                                                .version("1.0.0")
                                                .contact(new Contact()
                                                                .name("E-commerce Team")
                                                                .email("support@ecommerce.com"))
                                                .license(new License()
                                                                .name("MIT License")
                                                                .url("https://opensource.org/licenses/MIT")))
                                .servers(List.of(
                                                new Server()
                                                                .url("http://localhost:" + serverPort + "/api/v1")
                                                                .description("Local Development Server"),
                                                new Server()
                                                                .url("http://localhost:8080/api/v1")
                                                                .description("API Gateway")))
                                .components(new Components()
                                                .addSecuritySchemes("bearerAuth", new SecurityScheme()
                                                                .name("bearerAuth")
                                                                .type(SecurityScheme.Type.HTTP)
                                                                .scheme("bearer")
                                                                .bearerFormat("JWT")
                                                                .description("Enter JWT token")));
        }
}
