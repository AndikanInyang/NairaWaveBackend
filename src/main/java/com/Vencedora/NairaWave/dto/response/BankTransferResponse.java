package com.Vencedora.NairaWave.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class BankTransferResponse {
    private String referenceNumber;
    private String status;
    private BigDecimal amount;
    private BigDecimal fee;
    private String destinationAccountName;
    private String destinationAccountNumber;
    private String bankName;
    private String message;
}