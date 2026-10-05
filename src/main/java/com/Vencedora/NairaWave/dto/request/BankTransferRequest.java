package com.Vencedora.NairaWave.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class BankTransferRequest {
    @NotBlank
    private String sourceAccountNumber;

    @NotBlank
    private String bankCode;

    @NotBlank
    private String destinationAccountNumber;

    @NotBlank
    private String accountName;

    @NotNull
    @DecimalMin(value = "100.00", message = "Minimum transfer is ₦100")
    private BigDecimal amount;

    private String narration;
    private String clientReference;
    private String otpChallengeId;
}