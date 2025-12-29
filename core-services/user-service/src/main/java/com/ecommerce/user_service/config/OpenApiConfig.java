package com.ecommerce.user_service.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {
    
    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
            .info(new Info()
                .title("User Service API")
                .version("1.0")
                .description("User Service - Quản lý thông tin người dùng trong hệ thống Book Shop"))
            .servers(List.of(
                new Server().url("/api/v1").description("Default Server")
            ));
    }
}
