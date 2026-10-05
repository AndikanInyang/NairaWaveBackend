package com.Vencedora.NairaWave.payment.provider;

import java.math.BigDecimal;
import java.util.Map;

public interface VtuProvider {
    MeterVerification verifyMeter(String disco, String meterNo, String meterType);
    VtuPurchaseResult buyAirtime(String clientReference, String network, String phone, BigDecimal amount);
    VtuPurchaseResult buyData(String clientReference, String network, String phone, String planCode, BigDecimal amount);
    VtuPurchaseResult payElectricity(String clientReference, String disco, String meterNo, String meterType, BigDecimal amount);
}