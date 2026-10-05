package com.Vencedora.NairaWave.controller;

import com.Vencedora.NairaWave.dto.request.LoginRequest;
import com.Vencedora.NairaWave.dto.request.RefreshRequest;
import com.Vencedora.NairaWave.dto.request.RegisterRequest;
import com.Vencedora.NairaWave.dto.response.ApiResponse;
import com.Vencedora.NairaWave.dto.response.AuthResponse;
import com.Vencedora.NairaWave.service.AuthService;
import com.Vencedora.NairaWave.service.RefreshTokenService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final RefreshTokenService refreshTokenService;

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthResponse>> register(@Valid @RequestBody RegisterRequest request) {
        AuthResponse response = authService.register(request);
        return ResponseEntity.ok(ApiResponse.success("Account created successfully", response));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(ApiResponse.success("Login successful", response));
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<AuthResponse>> refresh(
            @Valid @RequestBody RefreshRequest request
    ) {
        AuthResponse response = refreshTokenService.rotate(request.getRefreshToken());
        return ResponseEntity.ok(ApiResponse.success("Token refreshed", response));
    }
}