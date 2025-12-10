package com.ecommerce.common.config;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.mongodb.config.EnableMongoAuditing;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

@AutoConfiguration
@EnableMongoAuditing(auditorAwareRef = "mongoAuditorProvider")
@ConditionalOnClass(name = "org.springframework.data.mongodb.core.MongoTemplate")
public class MongoAuditingConfig {

    @Bean
    public AuditorAware<String> mongoAuditorProvider() {
        return () -> {
            try {
                Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
                if (authentication != null && authentication.isAuthenticated()
                        && !"anonymousUser".equals(authentication.getPrincipal())) {
                    return Optional.of(authentication.getName());
                }
            } catch (Exception ignored) {
                // Security not available
            }
            return Optional.of("system");
        };
    }
}

