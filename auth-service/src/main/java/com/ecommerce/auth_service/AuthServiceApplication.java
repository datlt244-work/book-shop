package com.ecommerce.auth_service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableAsync;

import java.io.File;

@SpringBootApplication
@ComponentScan(basePackages = { "com.ecommerce.auth_service", "com.ecommerce.common" })
@EnableAsync
@EnableFeignClients
public class AuthServiceApplication {

	static {
		// Load .env file from infra directory before Spring Boot starts
		loadDotenv();
	}

	public static void main(String[] args) {
		SpringApplication.run(AuthServiceApplication.class, args);
	}

	private static void loadDotenv() {
		// Try to find .env file in infra directory relative to project root
		String[] possiblePaths = {
				"infra/.env", // From project root
				"../infra/.env", // From auth-service directory
				"../../infra/.env", // From deeper nested paths
				System.getProperty("user.dir") + "/infra/.env" // Absolute path fallback
		};

		for (String path : possiblePaths) {
			File envFile = new File(path);
			if (envFile.exists() && envFile.isFile()) {
				try {
					// Read .env file and set environment variables
					java.nio.file.Files.lines(envFile.toPath())
							.filter(line -> !line.trim().isEmpty() && !line.trim().startsWith("#"))
							.filter(line -> line.contains("="))
							.forEach(line -> {
								String[] parts = line.split("=", 2);
								if (parts.length == 2) {
									String key = parts[0].trim();
									String value = parts[1].trim();
									// Remove quotes if present
									if (value.startsWith("\"") && value.endsWith("\"")) {
										value = value.substring(1, value.length() - 1);
									} else if (value.startsWith("'") && value.endsWith("'")) {
										value = value.substring(1, value.length() - 1);
									}
									// Set as system property (Spring Boot can read these)
									if (System.getProperty(key) == null) {
										System.setProperty(key, value);
									}
								}
							});
					break;
				} catch (Exception e) {
					System.err.println("Warning: Could not load .env file from " + path + ": " + e.getMessage());
				}
			}
		}
	}

}
