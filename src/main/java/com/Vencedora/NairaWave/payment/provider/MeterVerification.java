package com.Vencedora.NairaWave.payment.provider;

public record MeterVerification(
        String customerName,
        String meterNo,
        String disco,
        String address
) {}