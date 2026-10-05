package com.Vencedora.NairaWave.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class TransferRequest {

    @NotBlank(message = "Source account number is required")
    private String sourceAccountNumber;

    @NotBlank(message = "Destination account number is required")
    private String destinationAccountNumber;

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "100.00", message = "Minimum transfer amount is ₦100.00")
    private BigDecimal amount;

    private String description;
    private String category;
    private Long otpChallengeId;
}