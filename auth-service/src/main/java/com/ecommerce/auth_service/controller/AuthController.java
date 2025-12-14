package com.ecommerce.auth_service.controller;

import com.ecommerce.auth_service.dto.request.AuthenticationRequest;
import com.ecommerce.auth_service.dto.request.IntrospectRequest;
import com.ecommerce.auth_service.dto.request.RegisterRequest;
import com.ecommerce.auth_service.dto.response.AuthenticationResponse;
import com.ecommerce.auth_service.dto.response.IntrospectResponse;
import com.ecommerce.auth_service.service.AuthenticationService;
import com.ecommerce.common.dto.ApiResponse;
import com.nimbusds.jose.JOSEException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.var;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.text.ParseException;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "APIs for user authentication and authorization")
public class AuthController {
    private final AuthenticationService authenticationService;

    @Operation(summary = "Get access token", description = "Authenticate user and return JWT token")
    @PostMapping("/token")
    public ApiResponse<AuthenticationResponse> authenticate(@Valid @RequestBody AuthenticationRequest request) {
        var result = authenticationService.authenticate(request);
        return ApiResponse.<AuthenticationResponse>builder()
                .result(result)
                .build();
    }

    @Operation(summary = "Introspect token", description = "Validate and get information about a JWT token")
    @PostMapping("/introspect")
    public ApiResponse<IntrospectResponse> introspect(@Valid @RequestBody IntrospectRequest request)
            throws ParseException, JOSEException {
        var result = authenticationService.introspect(request);
        return ApiResponse.<IntrospectResponse>builder()
                .result(result)
                .build();
    }

    @Operation(summary = "Register new user", description = "Create a new user account")
    @PostMapping("/register")
    public ApiResponse<String> register(@Valid @RequestBody RegisterRequest request) {
        authenticationService.register(request);
        return ApiResponse.success("User registered successfully");
    }
}
