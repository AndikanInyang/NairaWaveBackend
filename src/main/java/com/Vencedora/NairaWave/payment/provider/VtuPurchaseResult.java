package com.Vencedora.NairaWave.payment.provider;

public record VtuPurchaseResult(
        String providerReference,
        String status,
        String token,
        String message
) {
    public boolean isSuccess() {
        return "SUCCESS".equalsIgnoreCase(status);
    }
}