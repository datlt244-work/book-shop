package com.ecommerce.common.security;

import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.Collection;
import java.util.List;

/**
 * Authentication token representing a service-to-service call.
 * 
 * This is used to represent authenticated service requests in Spring Security.
 */
public class ServiceAuthentication extends AbstractAuthenticationToken {

    private final String serviceName;
    private final String clientId;
    private final String jti;

    /**
     * Create a new ServiceAuthentication.
     * 
     * @param serviceName The name of the calling service
     * @param clientId The client ID of the calling service
     * @param jti JWT ID for tracking
     */
    public ServiceAuthentication(String serviceName, String clientId, String jti) {
        super(buildAuthorities(serviceName));
        this.serviceName = serviceName;
        this.clientId = clientId;
        this.jti = jti;
        setAuthenticated(true);
    }

    private static Collection<? extends GrantedAuthority> buildAuthorities(String serviceName) {
        return List.of(
                new SimpleGrantedAuthority("ROLE_SERVICE"),
                new SimpleGrantedAuthority("SERVICE_" + serviceName.toUpperCase().replace("-", "_"))
        );
    }

    @Override
    public Object getCredentials() {
        return null; // No credentials exposed
    }

    @Override
    public Object getPrincipal() {
        return serviceName;
    }

    public String getServiceName() {
        return serviceName;
    }

    public String getClientId() {
        return clientId;
    }

    public String getJti() {
        return jti;
    }

    @Override
    public String getName() {
        return serviceName;
    }
}

