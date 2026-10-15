package com.frexsos.backend.controller;

import com.frexsos.backend.dto.AuthResponse;
import com.frexsos.backend.dto.LoginRequest;
import com.frexsos.backend.dto.RegisterRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final RestTemplate restTemplate;

    @Value("${auth.service.url:http://localhost:8082}")
    private String authServiceUrl;

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        String url = authServiceUrl + "/api/auth/register";
        return restTemplate.postForEntity(url, request, AuthResponse.class);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        String url = authServiceUrl + "/api/auth/login";
        return restTemplate.postForEntity(url, request, AuthResponse.class);
    }
}
