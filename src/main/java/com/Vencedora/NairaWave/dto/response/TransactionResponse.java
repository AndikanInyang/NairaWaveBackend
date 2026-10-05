package com.Vencedora.NairaWave.dto.response;

import com.Vencedora.NairaWave.entity.enums.TransactionStatus;
import com.Vencedora.NairaWave.entity.enums.TransactionType;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class TransactionResponse {
    private Long id;
    private String referenceNumber;
    private TransactionType type;
    private String category;
    private BigDecimal amount;
    private String description;
    private TransactionStatus status;
    private String sourceAccountNumber;
    private String destinationAccountNumber;
    private String providerReference;
    private LocalDateTime createdAt;
    private String token;
}