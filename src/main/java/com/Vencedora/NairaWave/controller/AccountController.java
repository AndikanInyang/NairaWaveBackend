package com.Vencedora.NairaWave.controller;

import com.Vencedora.NairaWave.dto.response.AccountResponse;
import com.Vencedora.NairaWave.dto.response.ApiResponse;
import com.Vencedora.NairaWave.entity.Account;
import com.Vencedora.NairaWave.entity.User;
import com.Vencedora.NairaWave.exception.ResourceNotFoundException;
import com.Vencedora.NairaWave.repository.AccountRepository;
import com.Vencedora.NairaWave.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/accounts")
@RequiredArgsConstructor
public class AccountController {

    private final AccountRepository accountRepository;
    private final UserRepository userRepository;

    @GetMapping
    public ResponseEntity<ApiResponse<List<AccountResponse>>> getUserAccounts(@AuthenticationPrincipal UserDetails userDetails) {
        User user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        List<AccountResponse> accounts = accountRepository.findByUserId(user.getId())
                .stream()
                .map(this::mapToResponse)
                .toList();

        return ResponseEntity.ok(ApiResponse.success("Accounts retrieved", accounts));
    }

    @GetMapping("/resolve-bank")
    public ResponseEntity<ApiResponse<Map<String, String>>> resolveBankAccount(
            @RequestParam String bankName,
            @RequestParam String accountNumber
    ) {
        // Mock name resolution for inter-bank transfers
        return ResponseEntity.ok(ApiResponse.success("Account resolved", Map.of(
                "accountName", "Chioma Adeleke",
                "bankName", bankName,
                "accountNumber", accountNumber
        )));
    }

    private AccountResponse mapToResponse(Account account) {
        return AccountResponse.builder()
                .id(account.getId())
                .accountNumber(account.getAccountNumber())
                .accountType(account.getAccountType())
                .balance(account.getBalance())
                .currency(account.getCurrency())
                .active(account.isActive())
                .bankName("NairaWave MFB")
                .createdAt(account.getCreatedAt())
                .build();
    }
}