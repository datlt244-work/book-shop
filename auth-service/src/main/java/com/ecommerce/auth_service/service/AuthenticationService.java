package com.ecommerce.auth_service.service;

import com.ecommerce.auth_service.config.VaultConfig;
import com.ecommerce.auth_service.dto.request.AuthenticationRequest;
import com.ecommerce.auth_service.dto.request.IntrospectRequest;
import com.ecommerce.auth_service.dto.request.RegisterRequest;
import com.ecommerce.auth_service.dto.response.AuthenticationResponse;
import com.ecommerce.auth_service.dto.response.IntrospectResponse;
import com.ecommerce.auth_service.entity.User;
import com.ecommerce.auth_service.repository.UserRepository;
import com.ecommerce.common.exception.AppException;
import com.ecommerce.common.exception.ErrorCode;
import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.text.ParseException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthenticationService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final VaultConfig vaultConfig;

    // --- 1. INTROSPECT (Verify Token) ---
    public IntrospectResponse introspect(IntrospectRequest request) throws JOSEException, ParseException{
        var token = request.getToken();
        boolean isValid = true;
        try {
            verifyToken(token);
        }catch (AppException | JOSEException | ParseException e){
            isValid = false;
        }

        return IntrospectResponse.builder()
                .valid(isValid)
                .build();
    }

    // --- 2. AUTHENTICATE (Login) ---
    public AuthenticationResponse authenticate(AuthenticationRequest request, String clientIp) {
        var user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        // Check if user is active
        if (user.getStatus() != com.ecommerce.auth_service.entity.UserStatus.ACTIVE) {
            throw new AppException(ErrorCode.UNAUTHENTICATED);
        }

        boolean authenticated = passwordEncoder.matches(request.getPassword(), user.getPasswordHash());

        if (!authenticated)
            throw new AppException(ErrorCode.UNAUTHENTICATED);

        // Update login tracking
        user.setLastLoginAt(java.time.LocalDateTime.now());
        user.setLastLoginIp(clientIp);
        user.setLoginCount(user.getLoginCount() + 1);
        userRepository.save(user);

        var token = generateToken(user);

        return AuthenticationResponse.builder()
                .token(token)
                .authenticated(true)
                .build();
    }

    // --- 3. REGISTER ---
    public User register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail()))
            throw new AppException(ErrorCode.USER_EXISTED);

        User user = User.builder()
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .fullName(request.getFullName())
                .phoneNumber(request.getPhoneNumber())
                .role(com.ecommerce.auth_service.entity.UserRole.CUSTOMER)
                .status(com.ecommerce.auth_service.entity.UserStatus.PENDING_VERIFICATION)
                .emailVerified(false)
                .loginCount(0)
                .build();

        return userRepository.save(user);
    }

    // --- Helper: Generate Token ---
    private String generateToken(User user) {
        JWSHeader header = new JWSHeader(JWSAlgorithm.HS512);

        JWTClaimsSet jwtClaimsSet = new JWTClaimsSet.Builder()
                .subject(user.getEmail())
                .issuer("com.ecommerce")
                .issueTime(new Date())
                .expirationTime(new Date(
                        Instant.now().plus(vaultConfig.getExpiration(), ChronoUnit.SECONDS).toEpochMilli()
                ))
                .claim("userId", user.getId())
                .claim("email", user.getEmail())
                .claim("role", user.getRole().name())
                .claim("scope", user.getRole().name())
                .build();

        Payload payload = new Payload(jwtClaimsSet.toJSONObject());

        JWSObject jwsObject = new JWSObject(header, payload);

        try {
            jwsObject.sign(new MACSigner(vaultConfig.getSignerKey().getBytes()));
            return jwsObject.serialize();
        } catch (JOSEException e) {
            log.error("Cannot create token", e);
            throw new RuntimeException(e);
        }
    }

    // --- Helper: Verify Token ---
    private void verifyToken(String token) throws JOSEException, ParseException {
        JWSVerifier verifier = new MACVerifier(vaultConfig.getSignerKey().getBytes());

        SignedJWT signedJWT = SignedJWT.parse(token);

        Date expiryTime = signedJWT.getJWTClaimsSet().getExpirationTime();

        var verified = signedJWT.verify(verifier);

        if (!(verified && expiryTime.after(new Date())))
            throw new AppException(ErrorCode.UNAUTHENTICATED);

    }
}
