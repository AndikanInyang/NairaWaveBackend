package com.Vencedora.NairaWave.payment.provider;

import java.math.BigDecimal;

public record BankTransferCommand(
        String clientReference,
        String recipientCode,
        String bankCode,
        String accountNumber,
        String accountName,
        BigDecimal amount,
        String narration
) {}