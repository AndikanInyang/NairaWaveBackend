package com.Vencedora.NairaWave.payment.provider;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.util.UUID;

@Component
@ConditionalOnProperty(name = "vtu.provider", havingValue = "mock", matchIfMissing = true)
public class MockVtuProvider implements VtuProvider {

    @Override
    public MeterVerification verifyMeter(String disco, String meterNo, String meterType) {
        return new MeterVerification("Adaeze Okonkwo", meterNo, disco, "12, Admiralty Way, Lekki Phase 1, Lagos");
    }

    @Override
    public VtuPurchaseResult buyAirtime(String ref, String net, String phone, BigDecimal amt) {
        return new VtuPurchaseResult("MOCK-VTU-" + UUID.randomUUID(), "SUCCESS", null, "Airtime purchase successful");
    }

    @Override
    public VtuPurchaseResult buyData(String ref, String net, String phone, String plan, BigDecimal amt) {
        return new VtuPurchaseResult("MOCK-VTU-" + UUID.randomUUID(), "SUCCESS", null, "Data purchase successful");
    }

    @Override
    public VtuPurchaseResult payElectricity(String ref, String disco, String meterNo, String type, BigDecimal amt) {
        SecureRandom sr = new SecureRandom();
        String token = String.format("%04d-%04d-%04d-%04d-%04d", sr.nextInt(10000), sr.nextInt(10000), sr.nextInt(10000), sr.nextInt(10000), sr.nextInt(10000));
        return new VtuPurchaseResult("MOCK-VTU-" + UUID.randomUUID(), "SUCCESS", token, "Electricity payment successful");
    }
}