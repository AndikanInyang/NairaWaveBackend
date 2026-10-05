package com.Vencedora.NairaWave.payment.provider;

import java.math.BigDecimal;

public interface BankTransferProvider {

    NameEnquiryResult resolveAccount(String bankCode, String accountNumber);

    String createRecipient(String bankCode, String accountNumber, String accountName);

    ProviderTransferResult initiateTransfer(BankTransferCommand command);

    ProviderTransferResult queryTransfer(String providerReference);
}