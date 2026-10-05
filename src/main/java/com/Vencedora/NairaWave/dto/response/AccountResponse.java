package com.Vencedora.NairaWave.dto.response;

import com.Vencedora.NairaWave.entity.enums.AccountType;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class AccountResponse {
    private Long id;
    private String accountNumber;
    private AccountType accountType;
    private BigDecimal balance;
    private String currency;
    private boolean active;
    private String bankName;
    private LocalDateTime createdAt;
}