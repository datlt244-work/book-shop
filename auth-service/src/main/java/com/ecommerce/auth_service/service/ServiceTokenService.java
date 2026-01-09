package com.ecommerce.auth_service.service;

import com.ecommerce.auth_service.config.ServiceVaultConfig;
import com.ecommerce.auth_service.dto.request.ServiceTokenRequest;
import com.ecommerce.auth_service.dto.response.ServiceTokenResponse;
import com.ecommerce.common.exception.AppException;
import com.ecommerce.common.exception.ErrorCode;
import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.stereotype.Service;

import java.text.ParseException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

/**
 * Service for generating and validating service-to-service JWT tokens.
 * 
 * This service handles:
 * - Generating JWT tokens for services to authenticate with each other
 * - Validating incoming service tokens
 * - Managing service credentials loaded from Vault
 */
@Slf4j
@Service
@RefreshScope
@RequiredArgsConstructor
public class ServiceTokenService {

    private final ServiceVaultConfig serviceVaultConfig;

    // Map of service credentials loaded from Vault
    // In production, this should be loaded dynamically from Vault
    @Value("${service.credentials.product-service.client-secret:}")
    private String productServiceSecret;

    @Value("${service.credentials.order-service.client-secret:}")
    private String orderServiceSecret;

    @Value("${service.credentials.user-service.client-secret:}")
    private String userServiceSecret;

    @Value("${service.credentials.api-gateway.client-secret:}")
    private String apiGatewaySecret;

    /**
     * Generate a service token for the requesting service.
     * 
     * @param request Contains clientId and clientSecret
     * @return ServiceTokenResponse with JWT token
     */
    public ServiceTokenResponse generateServiceToken(ServiceTokenRequest request) {
        // Validate service credentials
        String serviceName = validateServiceCredentials(request.getClientId(), request.getClientSecret());
        
        if (serviceName == null) {
            log.warn("Invalid service credentials for client: {}", request.getClientId());
            throw new AppException(ErrorCode.UNAUTHENTICATED);
        }

        // Generate JWT token for the service
        String token = generateToken(serviceName, request.getClientId());
        
        log.info("Generated service token for: {}", serviceName);

        return ServiceTokenResponse.builder()
                .accessToken(token)
                .tokenType("Bearer")
                .expiresIn(serviceVaultConfig.getTokenExpiration())
                .serviceName(serviceName)
                .build();
    }

    /**
     * Validate service credentials.
     * 
     * @return service name if valid, null otherwise
     */
    private String validateServiceCredentials(String clientId, String clientSecret) {
        Map<String, String> serviceSecrets = getServiceSecrets();
        
        String expectedSecret = serviceSecrets.get(clientId);
        if (expectedSecret != null && expectedSecret.equals(clientSecret)) {
            return clientId;
        }
        
        return null;
    }

    /**
     * Get map of service credentials.
     * In production, this should be loaded from Vault dynamically.
     */
    private Map<String, String> getServiceSecrets() {
        Map<String, String> secrets = new HashMap<>();
        
        if (productServiceSecret != null && !productServiceSecret.isEmpty()) {
            secrets.put("product-service", productServiceSecret);
        }
        if (orderServiceSecret != null && !orderServiceSecret.isEmpty()) {
            secrets.put("order-service", orderServiceSecret);
        }
        if (userServiceSecret != null && !userServiceSecret.isEmpty()) {
            secrets.put("user-service", userServiceSecret);
        }
        if (apiGatewaySecret != null && !apiGatewaySecret.isEmpty()) {
            secrets.put("api-gateway", apiGatewaySecret);
        }
        
        return secrets;
    }

    /**
     * Generate JWT token for a service.
     */
    private String generateToken(String serviceName, String clientId) {
        String jti = UUID.randomUUID().toString();
        JWSHeader header = new JWSHeader(JWSAlgorithm.HS256);

        JWTClaimsSet jwtClaimsSet = new JWTClaimsSet.Builder()
                .subject(serviceName)
                .issuer("com.ecommerce.auth-service")
                .jwtID(jti)
                .issueTime(new Date())
                .expirationTime(new Date(
                        Instant.now().plus(serviceVaultConfig.getTokenExpiration(), ChronoUnit.SECONDS).toEpochMilli()))
                .claim("clientId", clientId)
                .claim("serviceName", serviceName)
                .claim("type", "SERVICE")  // Distinguish from user tokens
                .claim("scope", "service")
                .build();

        Payload payload = new Payload(jwtClaimsSet.toJSONObject());
        JWSObject jwsObject = new JWSObject(header, payload);

        try {
            jwsObject.sign(new MACSigner(getServiceJwtKey().getBytes()));
            return jwsObject.serialize();
        } catch (JOSEException e) {
            log.error("Cannot create service token", e);
            throw new RuntimeException("Failed to generate service token", e);
        }
    }

    /**
     * Validate a service token.
     * 
     * @param token The JWT token to validate
     * @return ServiceTokenInfo containing service details, or null if invalid
     */
    public ServiceTokenInfo validateToken(String token) {
        try {
            JWSVerifier verifier = new MACVerifier(getServiceJwtKey().getBytes());
            SignedJWT signedJWT = SignedJWT.parse(token);
            JWTClaimsSet claims = signedJWT.getJWTClaimsSet();

            // Verify signature
            if (!signedJWT.verify(verifier)) {
                log.warn("Service token signature verification failed");
                return null;
            }

            // Check expiration
            Date expiryTime = claims.getExpirationTime();
            if (expiryTime == null || expiryTime.before(new Date())) {
                log.warn("Service token expired");
                return null;
            }

            // Check token type
            String type = (String) claims.getClaim("type");
            if (!"SERVICE".equals(type)) {
                log.warn("Token is not a service token");
                return null;
            }

            String serviceName = (String) claims.getClaim("serviceName");
            String clientId = (String) claims.getClaim("clientId");

            return new ServiceTokenInfo(serviceName, clientId, claims.getJWTID());

        } catch (JOSEException | ParseException e) {
            log.error("Error validating service token", e);
            return null;
        }
    }

    /**
     * Get JWT signing key for service tokens.
     */
    private String getServiceJwtKey() {
        String key = serviceVaultConfig.getJwtKey();
        if (key == null || key.isEmpty()) {
            // Fallback: use the user JWT key if service key not configured
            log.warn("Service JWT key not configured, check Vault configuration");
            throw new IllegalStateException("Service JWT key not configured");
        }
        return key;
    }

    /**
     * Inner class to hold validated service token info.
     */
    public record ServiceTokenInfo(String serviceName, String clientId, String jti) {}
}

