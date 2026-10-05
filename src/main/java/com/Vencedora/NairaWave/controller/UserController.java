package com.Vencedora.NairaWave.controller;

import com.Vencedora.NairaWave.dto.response.ApiResponse;
import com.Vencedora.NairaWave.dto.response.UserDTO;
import com.Vencedora.NairaWave.entity.User;
import com.Vencedora.NairaWave.exception.ResourceNotFoundException;
import com.Vencedora.NairaWave.repository.AccountRepository;
import com.Vencedora.NairaWave.repository.UserRepository;
import com.Vencedora.NairaWave.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final AccountRepository accountRepository;

    private final AuthService authService;
    private final UserRepository userRepository;

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserDTO>> getProfile(@AuthenticationPrincipal UserDetails userDetails) {
        UserDTO profile = authService.getCurrentUser(userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success("Profile retrieved", profile));
    }

    // Resolves recipient name & NUBAN by phone number (used in SendMoney.tsx)
    @GetMapping("/resolve")
    public ResponseEntity<ApiResponse<Map<String, String>>> resolveUser(@RequestParam String phone) {
        User user = userRepository.findByPhoneNumber(phone)
                .orElseThrow(() -> new ResourceNotFoundException("User with phone " + phone + " not found on NairaWave"));

        String nuban = user.getAccounts().isEmpty() ? "" : accountRepository.findByUserId(user.getId()).get(0).getAccountNumber();

        return ResponseEntity.ok(ApiResponse.success("User resolved", Map.of(
                "fullName", user.getFirstName() + " " + user.getLastName(),
                "accountNumber", nuban
        )));
    }
}