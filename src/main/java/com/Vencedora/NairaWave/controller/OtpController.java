package com.Vencedora.NairaWave.controller;

import com.Vencedora.NairaWave.dto.response.ApiResponse;
import com.Vencedora.NairaWave.service.OtpService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/security/otp")
@RequiredArgsConstructor
public class OtpController {

    private final OtpService otpService;

    @PostMapping("/request")
    public ResponseEntity<ApiResponse<Map<String, Object>>> requestOtp(
            @RequestParam String purpose,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        Long challengeId = otpService.generateOtp(userDetails.getUsername(), purpose);
        return ResponseEntity.ok(ApiResponse.success("Verification code sent successfully", Map.of(
                "challengeId", challengeId,
                "sentTo", userDetails.getUsername()
        )));
    }

    @PostMapping("/verify")
    public ResponseEntity<ApiResponse<Void>> verifyOtp(
            @RequestParam Long challengeId,
            @RequestParam String code,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        otpService.verifyOtp(challengeId, code, userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success("OTP verification successful", null));
    }
}