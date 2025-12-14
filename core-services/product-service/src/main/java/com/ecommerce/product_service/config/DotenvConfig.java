package com.ecommerce.product_service.config;

import org.springframework.context.annotation.Configuration;

import java.io.File;

/**
 * Configuration to load .env file from infra directory
 * This ensures the .env file in infra/ directory is loaded before Spring Boot starts
 */
@Configuration
public class DotenvConfig {
    
    static {
        // Try to find .env file in infra directory relative to project root
        // This works when running from service directory or project root
        String[] possiblePaths = {
            "infra/.env",                    // From project root
            "../../infra/.env",              // From product-service directory
            "../../../infra/.env",           // From deeper nested paths
            System.getProperty("user.dir") + "/infra/.env"  // Absolute path fallback
        };
        
        for (String path : possiblePaths) {
            File envFile = new File(path);
            if (envFile.exists() && envFile.isFile()) {
                // Set system property for spring-dotenv to use
                System.setProperty("dotenv.directory", envFile.getParent());
                System.setProperty("dotenv.filename", envFile.getName());
                System.setProperty("dotenv.ignoreIfMissing", "true");
                System.setProperty("dotenv.systemProperties", "true");
                break;
            }
        }
    }
}
