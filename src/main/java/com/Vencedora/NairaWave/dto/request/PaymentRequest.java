package com.Vencedora.NairaWave.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class PaymentRequest {

    @NotBlank(message = "Account number is required")
    private String accountNumber;

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "50.00", message = "Minimum payment is ₦50.00")
    private BigDecimal amount;

    @NotBlank(message = "Category is required")
    private String category;

    private String provider;
    private String recipientId;
    private String description;
    private String clientReference;
    private String meterType; // prepaid | postpaid

    private Long otpChallengeId;
}