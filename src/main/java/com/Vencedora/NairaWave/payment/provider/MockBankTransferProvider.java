package com.Vencedora.NairaWave.payment.provider;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

@Component
@ConditionalOnProperty(name = "bank.provider", havingValue = "mock", matchIfMissing = true)
public class MockBankTransferProvider implements BankTransferProvider {

    private static final Map<String, String> BANK_NAMES = Map.of(
    );

    @Override
    public NameEnquiryResult resolveAccount(String bankCode, String accountNumber) {
        if (accountNumber == null || accountNumber.length() != 10) {
            throw new IllegalArgumentException("Account number must be 10 digits");
        }
        String bankName = BANK_NAMES.getOrDefault(bankCode, "Unknown Bank");
        String mockName = "CHIKE OKORO";
        return new NameEnquiryResult(mockName, bankCode, accountNumber, bankName);
    }

    @Override
    public String createRecipient(String bankCode, String accountNumber, String accountName) {
        return "MOCK-RCP-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    @Override
    public ProviderTransferResult initiateTransfer(BankTransferCommand command) {
        String ref = "MOCK-NIP-" + UUID.randomUUID().toString().substring(0, 12).toUpperCase();
        return new ProviderTransferResult(ref, "SUCCESS", "Mock transfer completed");
    }

    @Override
    public ProviderTransferResult queryTransfer(String providerReference) {
        return new ProviderTransferResult(providerReference, "SUCCESS", "Mock query success");
    }
}