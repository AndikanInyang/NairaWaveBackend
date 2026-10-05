package com.Vencedora.NairaWave.controller;

import com.Vencedora.NairaWave.dto.request.PaymentRequest;
import com.Vencedora.NairaWave.dto.request.TransferRequest;
import com.Vencedora.NairaWave.dto.response.ApiResponse;
import com.Vencedora.NairaWave.dto.response.TransactionResponse;
import com.Vencedora.NairaWave.service.TransactionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/transactions")
@RequiredArgsConstructor
public class TransactionController {

    private final TransactionService transactionService;

    @PostMapping("/transfer")
    public ResponseEntity<ApiResponse<TransactionResponse>> transfer(
            @Valid @RequestBody TransferRequest request,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        TransactionResponse response = transactionService.transfer(request, userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success("Transfer completed successfully", response));
    }

    @PostMapping("/payment")
    public ResponseEntity<ApiResponse<TransactionResponse>> makePayment(
            @Valid @RequestBody PaymentRequest request,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        TransactionResponse response = transactionService.makePayment(request, userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success("Payment completed successfully", response));
    }

    @GetMapping("/history")
    public ResponseEntity<ApiResponse<Page<TransactionResponse>>> getHistory(
            @RequestParam String accountNumber,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Page<TransactionResponse> history = transactionService.getHistory(accountNumber, page, size);
        return ResponseEntity.ok(ApiResponse.success("Transaction history retrieved", history));
    }
}