package com.Vencedora.NairaWave.controller;

import com.Vencedora.NairaWave.dto.request.BankTransferRequest;
import com.Vencedora.NairaWave.dto.response.ApiResponse;
import com.Vencedora.NairaWave.dto.response.BankResolveResponse;
import com.Vencedora.NairaWave.dto.response.BankTransferResponse;
import com.Vencedora.NairaWave.service.BankTransferService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/transfers/bank")
@RequiredArgsConstructor
public class BankTransferController {

    private final BankTransferService bankTransferService;

    @GetMapping("/resolve")
    public ResponseEntity<ApiResponse<BankResolveResponse>> resolve(
            @RequestParam String bankCode,
            @RequestParam String accountNumber
    ) {
        BankResolveResponse result = bankTransferService.resolveAccount(bankCode, accountNumber);
        return ResponseEntity.ok(ApiResponse.success("Account resolved", result));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<BankTransferResponse>> transfer(
            @Valid @RequestBody BankTransferRequest request,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        BankTransferResponse result = bankTransferService.initiateTransfer(request, userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success("Bank transfer initiated", result));
    }
}