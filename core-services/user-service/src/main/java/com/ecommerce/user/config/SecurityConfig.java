package com.ecommerce.user.config;

import com.ecommerce.common.security.ServiceAuthFilter;
import com.ecommerce.common.security.ServiceAuthProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import javax.crypto.spec.SecretKeySpec;
import java.util.Arrays;
import java.util.List;

/**
 * Security configuration for User Service.
 */
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final VaultConfig vaultConfig;
    
    // Optional - may be null if service.auth.enabled=false
    @Autowired(required = false)
    private ServiceAuthFilter serviceAuthFilter;
    
    @Autowired(required = false)
    private ServiceAuthProperties serviceAuthProperties;

    // Public endpoints - no authentication required
    private final String[] PUBLIC_GET_ENDPOINTS = {
            "/actuator/**", "/actuator/health/**",
            "/swagger-ui.html", "/swagger-ui/**",
            "/v3/api-docs", "/v3/api-docs/**", "/swagger-resources/**",
            "/webjars/**",
            "/error"
    };

    // Internal endpoints - require service authentication (ROLE_SERVICE)
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
                        // Internal endpoints require SERVICE role
                        .requestMatchers(INTERNAL_ENDPOINTS).hasRole("SERVICE")
                        // User endpoints require authentication
                        .requestMatchers("/users/me/**").authenticated()
                        // Public user profile (limited info)
                        .requestMatchers(HttpMethod.GET, "/users/{id}").permitAll()
                        // All other requests need authentication
                        .anyRequest().authenticated());
        
        // Add ServiceAuthFilter only if service auth is enabled
        if (serviceAuthFilter != null) {
            http.addFilterBefore(serviceAuthFilter, UsernamePasswordAuthenticationFilter.class);
        } else if (serviceAuthProperties == null || !serviceAuthProperties.isEnabled()) {
            // Create a no-op filter or use default properties
            ServiceAuthProperties defaultProps = new ServiceAuthProperties();
            defaultProps.setEnabled(false);
            http.addFilterBefore(new ServiceAuthFilter(defaultProps), UsernamePasswordAuthenticationFilter.class);
        }
        
        // OAuth2 Resource Server for user JWT tokens
        http.oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> jwt.decoder(jwtDecoder())));

        return http.build();
    }

    @Bean
    public JwtDecoder jwtDecoder() {
        String signerKey = vaultConfig.getSignerKey();
        if (signerKey == null || signerKey.isBlank()) {
            throw new IllegalStateException(
                    "JWT signer key is not configured! " +
                    "Please set JWT_SIGNER_KEY in .env file (dev) or configure Vault (prod). " +
                    "This key MUST match the auth-service's jwt-signer-key."
            );
        }
        SecretKeySpec secretKeySpec = new SecretKeySpec(
                signerKey.getBytes(),
                "HmacSHA256");
        return NimbusJwtDecoder.withSecretKey(secretKeySpec)
                .macAlgorithm(MacAlgorithm.HS256)
                .build();
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

