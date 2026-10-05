package com.Vencedora.NairaWave.service;

import com.Vencedora.NairaWave.entity.PaymentOrder;
import com.Vencedora.NairaWave.entity.enums.PaymentStatus;
import com.Vencedora.NairaWave.payment.provider.BankTransferProvider;
import com.Vencedora.NairaWave.payment.provider.ProviderTransferResult;
import com.Vencedora.NairaWave.repository.AccountRepository;
import com.Vencedora.NairaWave.repository.PaymentOrderRepository;
import com.Vencedora.NairaWave.repository.TransactionRepository;
import com.Vencedora.NairaWave.entity.enums.TransactionStatus;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BankTransferReconciliationService {

    private static final Logger log = LoggerFactory.getLogger(BankTransferReconciliationService.class);

    private final PaymentOrderRepository paymentOrderRepository;
    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final BankTransferProvider bankTransferProvider;

    @Scheduled(fixedDelay = 120000)
    @Transactional
    public void reconcilePendingTransfers() {
        List<PaymentOrder> pending = paymentOrderRepository.findAll().stream()
                .filter(o -> o.getStatus() == PaymentStatus.PENDING)
                .filter(o -> "transfer".equals(o.getCategory()))
                .filter(o -> o.getProviderReference() != null)
                .filter(o -> o.getCreatedAt().isAfter(LocalDateTime.now().minusHours(24)))
                .toList();

        for (PaymentOrder order : pending) {
            try {
                ProviderTransferResult result = bankTransferProvider.queryTransfer(order.getProviderReference());

                if (result.isSuccess()) {
                    order.setStatus(PaymentStatus.COMPLETED);
                    if (order.getTransaction() != null) {
                        order.getTransaction().setStatus(TransactionStatus.COMPLETED);
                        transactionRepository.save(order.getTransaction());
                    }
                    paymentOrderRepository.save(order);
                    log.info("Reconciled: {} → COMPLETED", order.getClientReference());
                } else if (result.isFailed()) {
                    BigDecimal refund = order.getAmount().add(
                            order.getFee() != null ? order.getFee() : BigDecimal.ZERO
                    );
                    accountRepository.findByAccountNumberForUpdate(order.getAccount().getAccountNumber())
                            .ifPresent(acct -> {
                                acct.setBalance(acct.getBalance().add(refund));
                                accountRepository.save(acct);
                            });
                    order.setStatus(PaymentStatus.FAILED);
                    if (order.getTransaction() != null) {
                        order.getTransaction().setStatus(TransactionStatus.FAILED);
                        transactionRepository.save(order.getTransaction());
                    }
                    paymentOrderRepository.save(order);
                    log.info("Reconciled: {} → FAILED, refunded", order.getClientReference());
                }
            } catch (Exception e) {
                log.error("Reconciliation error for {}: {}", order.getClientReference(), e.getMessage());
            }
        }
    }
}