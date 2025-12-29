package com.ecommerce.user_service.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@Configuration
@EnableJpaAuditing
public class JpaAuditingConfig {
    // EnableJpaAuditing for createdAt, updatedAt auto-filling
}
