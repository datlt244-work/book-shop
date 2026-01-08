package com.ecommerce.user.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration properties loaded from Vault.
 */
@Configuration
@ConfigurationProperties(prefix = "jwt")
@Getter
@Setter
public class VaultConfig {

    /**
     * JWT signing key loaded from Vault (for validating user tokens)
     */
    private String signerKey;
}

