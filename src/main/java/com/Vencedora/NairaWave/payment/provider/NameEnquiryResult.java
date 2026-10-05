package com.Vencedora.NairaWave.payment.provider;

public record NameEnquiryResult(
        String accountName,
        String bankCode,
        String accountNumber,
        String bankName
) {}