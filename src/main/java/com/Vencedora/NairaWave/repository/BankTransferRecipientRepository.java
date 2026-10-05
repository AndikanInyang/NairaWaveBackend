package com.Vencedora.NairaWave.repository;

import com.Vencedora.NairaWave.entity.BankTransferRecipient;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface BankTransferRecipientRepository extends JpaRepository<BankTransferRecipient, Long> {
    Optional<BankTransferRecipient> findByUserIdAndBankCodeAndAccountNumber(
            Long userId, String bankCode, String accountNumber
    );
}