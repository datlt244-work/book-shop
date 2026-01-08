package com.ecommerce.product_service.config;

import com.ecommerce.common.security.ServiceAuthFilter;
import com.ecommerce.common.security.ServiceAuthProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

/**
 * Security configuration for Product Service.
 * 
 * This configuration:
 * - Enables service-to-service authentication via ServiceAuthFilter
 * - Configures public endpoints (actuator, swagger, public product APIs)
 * - Protects admin/internal endpoints
 */
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final ServiceAuthFilter serviceAuthFilter;
    private final ServiceAuthProperties serviceAuthProperties;

    // Public endpoints - no authentication required
    private final String[] PUBLIC_GET_ENDPOINTS = {
            "/actuator/**", "/actuator/health/**",
            "/swagger-ui.html", "/swagger-ui/**",
            "/v3/api-docs", "/v3/api-docs/**", "/swagger-resources/**",
            "/webjars/**",
            "/error",
            // Public product APIs
            "/products", "/products/**",
            "/categories", "/categories/**"
    };

    // Internal endpoints - require service authentication
    private final String[] INTERNAL_ENDPOINTS = {
            "/internal/**"
    };

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // Public endpoints
                        .requestMatchers(PUBLIC_GET_ENDPOINTS).permitAll()
                        // Internal endpoints require SERVICE role (from ServiceAuthFilter)
                        .requestMatchers(INTERNAL_ENDPOINTS).hasRole("SERVICE")
                        // All other requests need authentication
                        .anyRequest().authenticated())
                // Add ServiceAuthFilter before standard auth
                .addFilterBefore(serviceAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(List.of("*"));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setExposedHeaders(List.of("Authorization", "Content-Type", "X-Service-Name"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}

