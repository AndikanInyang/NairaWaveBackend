package com.Vencedora.NairaWave.payment.provider;

public record ProviderTransferResult(
        String providerReference,
        String status,
        String message
) {
    public boolean isSuccess() {
        return "SUCCESS".equalsIgnoreCase(status) || "success".equalsIgnoreCase(status);
    }

    public boolean isFailed() {
        return "FAILED".equalsIgnoreCase(status) || "failed".equalsIgnoreCase(status);
    }

    public boolean isPending() {
        return !isSuccess() && !isFailed();
    }
}