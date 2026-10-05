package com.Vencedora.NairaWave.service;

import com.Vencedora.NairaWave.dto.request.BankTransferRequest;
import com.Vencedora.NairaWave.dto.response.BankResolveResponse;
import com.Vencedora.NairaWave.dto.response.BankTransferResponse;
import com.Vencedora.NairaWave.entity.Account;
import com.Vencedora.NairaWave.entity.BankTransferRecipient;
import com.Vencedora.NairaWave.entity.PaymentOrder;
import com.Vencedora.NairaWave.entity.Transaction;
import com.Vencedora.NairaWave.entity.User;
import com.Vencedora.NairaWave.entity.enums.PaymentStatus;
import com.Vencedora.NairaWave.entity.enums.TransactionStatus;
import com.Vencedora.NairaWave.entity.enums.TransactionType;
import com.Vencedora.NairaWave.exception.*;
import com.Vencedora.NairaWave.payment.provider.BankTransferProvider;
import com.Vencedora.NairaWave.payment.provider.BankTransferCommand;
import com.Vencedora.NairaWave.payment.provider.NameEnquiryResult;
import com.Vencedora.NairaWave.payment.provider.ProviderTransferResult;
import com.Vencedora.NairaWave.repository.*;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class BankTransferService {

    private static final Logger log = LoggerFactory.getLogger(BankTransferService.class);

    private final UserRepository userRepository;
    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final PaymentOrderRepository paymentOrderRepository;
    private final BankTransferRecipientRepository recipientRepository;
    private final BankTransferProvider bankTransferProvider;

    private static final BigDecimal FEE_THRESHOLD = new BigDecimal("5000");
    private static final BigDecimal TRANSFER_FEE = new BigDecimal("10.75");

    public BankResolveResponse resolveAccount(String bankCode, String accountNumber) {
        NameEnquiryResult result = bankTransferProvider.resolveAccount(bankCode, accountNumber);
        return BankResolveResponse.builder()
                .accountName(result.accountName())
                .bankCode(result.bankCode())
                .accountNumber(result.accountNumber())
                .bankName(result.bankName())
                .build();
    }

    @Transactional
    public BankTransferResponse initiateTransfer(BankTransferRequest request, String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        String clientRef = (request.getClientReference() == null || request.getClientReference().isBlank())
                ? UUID.randomUUID().toString()
                : request.getClientReference();

        if (paymentOrderRepository.existsByClientReference(clientRef)) {
            throw new BadRequestException("Duplicate transfer request");
        }

        Account source = accountRepository.findByAccountNumberForUpdate(request.getSourceAccountNumber())
                .orElseThrow(() -> new ResourceNotFoundException("Source account not found"));

        if (!source.getUser().getId().equals(user.getId())) {
            throw new UnauthorizedException("You do not own the source account");
        }
        if (!source.isActive()) {
            throw new BadRequestException("Account is not active");
        }

        BigDecimal fee = request.getAmount().compareTo(FEE_THRESHOLD) > 0 ? TRANSFER_FEE : BigDecimal.ZERO;
        BigDecimal totalDebit = request.getAmount().add(fee);

        if (source.getBalance().compareTo(totalDebit) < 0) {
            throw new InsufficientFundsException("Insufficient funds (including ₦" + fee + " fee)");
        }

        source.setBalance(source.getBalance().subtract(totalDebit));
        accountRepository.save(source);

        String narration = request.getNarration() != null && !request.getNarration().isBlank()
                ? request.getNarration()
                : "Transfer to " + request.getAccountName();

        Transaction tx = Transaction.builder()
                .referenceNumber("BNK-" + UUID.randomUUID().toString().substring(0, 18).toUpperCase())
                .sourceAccount(source)
                .amount(request.getAmount())
                .type(TransactionType.TRANSFER)
                .category("transfer")
                .status(TransactionStatus.PENDING)
                .description(narration + " · " + request.getBankCode())
                .build();
        tx = transactionRepository.save(tx);

        PaymentOrder order = PaymentOrder.builder()
                .user(user)
                .account(source)
                .transaction(tx)
                .category("transfer")
                .provider(request.getBankCode())
                .bankCode(request.getBankCode())
                .recipientId(request.getDestinationAccountNumber())
                .destinationAccountNumber(request.getDestinationAccountNumber())
                .destinationAccountName(request.getAccountName())
                .amount(request.getAmount())
                .fee(fee)
                .status(PaymentStatus.PENDING)
                .clientReference(clientRef)
                .build();
        order = paymentOrderRepository.save(order);

        try {
            String recipientCode = getOrCreateRecipient(
                    user.getId(), request.getBankCode(),
                    request.getDestinationAccountNumber(), request.getAccountName()
            );

            ProviderTransferResult result = bankTransferProvider.initiateTransfer(
                    new BankTransferCommand(
                            clientRef, recipientCode, request.getBankCode(),
                            request.getDestinationAccountNumber(),
                            request.getAccountName(), request.getAmount(), narration
                    )
            );

            order.setProviderReference(result.providerReference());

            if (result.isSuccess()) {
                order.setStatus(PaymentStatus.COMPLETED);
                tx.setStatus(TransactionStatus.COMPLETED);
            } else if (result.isFailed()) {
                refund(source, totalDebit);
                order.setStatus(PaymentStatus.FAILED);
                tx.setStatus(TransactionStatus.FAILED);
            }
            // PENDING stays PENDING — webhook or reconciliation will update

            paymentOrderRepository.save(order);
            transactionRepository.save(tx);

            return BankTransferResponse.builder()
                    .referenceNumber(tx.getReferenceNumber())
                    .status(tx.getStatus().name())
                    .amount(request.getAmount())
                    .fee(fee)
                    .destinationAccountName(request.getAccountName())
                    .destinationAccountNumber(request.getDestinationAccountNumber())
                    .bankName(request.getBankCode())
                    .message(result.message())
                    .build();

        } catch (Exception e) {
            log.error("Provider call failed, refunding: {}", e.getMessage());
            refund(source, totalDebit);
            order.setStatus(PaymentStatus.FAILED);
            tx.setStatus(TransactionStatus.FAILED);
            paymentOrderRepository.save(order);
            transactionRepository.save(tx);

            throw new BadRequestException("Transfer failed: " + e.getMessage());
        }
    }

    @Transactional
    public void handleWebhookSuccess(String clientReference, String providerReference) {
        paymentOrderRepository.findByClientReference(clientReference).ifPresent(order -> {
            if (order.getStatus() == PaymentStatus.COMPLETED) return;

            order.setStatus(PaymentStatus.COMPLETED);
            order.setProviderReference(providerReference);
            paymentOrderRepository.save(order);

            if (order.getTransaction() != null) {
                order.getTransaction().setStatus(TransactionStatus.COMPLETED);
                transactionRepository.save(order.getTransaction());
            }
        });
    }

    @Transactional
    public void handleWebhookFailure(String clientReference, String providerReference) {
        paymentOrderRepository.findByClientReference(clientReference).ifPresent(order -> {
            if (order.getStatus() != PaymentStatus.PENDING) return;

            BigDecimal refundAmount = order.getAmount().add(
                    order.getFee() != null ? order.getFee() : BigDecimal.ZERO
            );
            Account source = accountRepository.findByAccountNumberForUpdate(order.getAccount().getAccountNumber())
                    .orElse(null);
            if (source != null) {
                refund(source, refundAmount);
            }

            order.setStatus(PaymentStatus.FAILED);
            order.setProviderReference(providerReference);
            paymentOrderRepository.save(order);

            if (order.getTransaction() != null) {
                order.getTransaction().setStatus(TransactionStatus.FAILED);
                transactionRepository.save(order.getTransaction());
            }
        });
    }

    private String getOrCreateRecipient(Long userId, String bankCode, String accountNumber, String accountName) {
        return recipientRepository
                .findByUserIdAndBankCodeAndAccountNumber(userId, bankCode, accountNumber)
                .map(r -> {
                    if (r.getProviderRecipientCode() != null) return r.getProviderRecipientCode();
                    String code = bankTransferProvider.createRecipient(bankCode, accountNumber, accountName);
                    r.setProviderRecipientCode(code);
                    recipientRepository.save(r);
                    return code;
                })
                .orElseGet(() -> {
                    String code = bankTransferProvider.createRecipient(bankCode, accountNumber, accountName);
                    BankTransferRecipient r = BankTransferRecipient.builder()
                            .user(userRepository.findById(userId).orElseThrow())
                            .bankCode(bankCode)
                            .accountNumber(accountNumber)
                            .accountName(accountName)
                            .providerRecipientCode(code)
                            .build();
                    recipientRepository.save(r);
                    return code;
                });
    }

    private void refund(Account account, BigDecimal amount) {
        account.setBalance(account.getBalance().add(amount));
        accountRepository.save(account);
    }
}