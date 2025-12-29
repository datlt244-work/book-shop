package com.ecommerce.user_service;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.servers.Server;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@OpenAPIDefinition(
    info = @Info(
        title = "User Service API",
        version = "1.0",
        description = "User Service - Quản lý thông tin người dùng"
    ),
    servers = @Server(url = "/api/v1", description = "Default Server URL")
)
@ComponentScan(basePackages = {
    "com.ecommerce.user_service",
    "com.ecommerce.common"
})
public class UserServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(UserServiceApplication.class, args);
    }

}
