package com.Vencedora.NairaWave.service;

import com.Vencedora.NairaWave.dto.request.PaymentRequest;
import com.Vencedora.NairaWave.dto.request.TransferRequest;
import com.Vencedora.NairaWave.dto.response.TransactionResponse;
import com.Vencedora.NairaWave.entity.Account;
import com.Vencedora.NairaWave.entity.OtpChallenge;
import com.Vencedora.NairaWave.entity.PaymentOrder;
import com.Vencedora.NairaWave.entity.Transaction;
import com.Vencedora.NairaWave.entity.User;
import com.Vencedora.NairaWave.entity.enums.PaymentStatus;
import com.Vencedora.NairaWave.entity.enums.TransactionStatus;
import com.Vencedora.NairaWave.entity.enums.TransactionType;
import com.Vencedora.NairaWave.exception.BadRequestException;
import com.Vencedora.NairaWave.exception.InsufficientFundsException;
import com.Vencedora.NairaWave.exception.ResourceNotFoundException;
import com.Vencedora.NairaWave.exception.UnauthorizedException;
import com.Vencedora.NairaWave.payment.provider.VtuProvider;
import com.Vencedora.NairaWave.payment.provider.VtuPurchaseResult;
import com.Vencedora.NairaWave.repository.AccountRepository;
import com.Vencedora.NairaWave.repository.OtpChallengeRepository;
import com.Vencedora.NairaWave.repository.PaymentOrderRepository;
import com.Vencedora.NairaWave.repository.TransactionRepository;
import com.Vencedora.NairaWave.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TransactionService {

    private static final Logger log = LoggerFactory.getLogger(TransactionService.class);

    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;
    private final PaymentOrderRepository paymentOrderRepository;
    private final OtpChallengeRepository otpChallengeRepository;
    private final VtuProvider vtuProvider;

    private static final BigDecimal HIGH_VALUE_THRESHOLD = new BigDecimal("10000.00");

    /**
      Internal Wallet-to-Wallet transfer between two NairaWave accounts.
      Enforces OTP validation for transfers >= ₦10,000.
     **/
    @Transactional
    public TransactionResponse transfer(TransferRequest request, String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (request.getSourceAccountNumber().equals(request.getDestinationAccountNumber())) {
            throw new BadRequestException("Cannot transfer to the same account");
        }

        // 1. High-Value OTP Check (>= ₦10,000)
        enforceSecurityValidation(user, request.getAmount(), request.getOtpChallengeId(), "TRANSFER");

        // 2. Lock accounts for atomic balance update
        Account source = accountRepository.findByAccountNumberForUpdate(request.getSourceAccountNumber())
                .orElseThrow(() -> new ResourceNotFoundException("Source account not found"));

        Account destination = accountRepository.findByAccountNumberForUpdate(request.getDestinationAccountNumber())
                .orElseThrow(() -> new ResourceNotFoundException("Destination account not found"));

        // 3. Ownership and status validation
        if (!source.getUser().getId().equals(user.getId())) {
            throw new UnauthorizedException("You do not own the source account");
        }

        if (!source.isActive()) {
            throw new BadRequestException("Source account is inactive");
        }

        if (!destination.isActive()) {
            throw new BadRequestException("Destination account is inactive");
        }

        // 4. Verify balance
        if (source.getBalance().compareTo(request.getAmount()) < 0) {
            throw new InsufficientFundsException("Insufficient funds in account");
        }

        // 5. Update balances atomically
        source.setBalance(source.getBalance().subtract(request.getAmount()));
        destination.setBalance(destination.getBalance().add(request.getAmount()));

        accountRepository.save(source);
        accountRepository.save(destination);

        // 6. Record transaction
        Transaction tx = Transaction.builder()
                .referenceNumber("TX-" + UUID.randomUUID().toString().substring(0, 18).toUpperCase())
                .sourceAccount(source)
                .destinationAccount(destination)
                .amount(request.getAmount())
                .type(TransactionType.TRANSFER)
                .category(request.getCategory() != null ? request.getCategory() : "transfer")
                .status(TransactionStatus.COMPLETED)
                .description(request.getDescription())
                .build();

        tx = transactionRepository.save(tx);
        return mapToResponse(tx);
    }

    /**
     * Executes airtime, data, and electricity bill payments.
     * Enforces OTP validation for payments >= ₦10,000.
     */
    @Transactional
    public TransactionResponse makePayment(PaymentRequest request, String userEmail) {
        String category = request.getCategory() != null ? request.getCategory().toLowerCase() : "";
        PaymentRules.validate(category, request.getAmount());

        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        // 1. Idempotency Check
        String clientRef = (request.getClientReference() == null || request.getClientReference().isBlank())
                ? UUID.randomUUID().toString()
                : request.getClientReference();

        Optional<PaymentOrder> existing = paymentOrderRepository.findByClientReference(clientRef);
        if (existing.isPresent()) {
            PaymentOrder order = existing.get();
            return mapToResponse(order.getTransaction(), order.getToken());
        }

        // 2. High-Value OTP Check (>= ₦10,000)
        enforceSecurityValidation(user, request.getAmount(), request.getOtpChallengeId(), "PAYMENT");

        // 3. Pessimistic Lock on Account row
        Account source = accountRepository.findByAccountNumberForUpdate(request.getAccountNumber())
                .orElseThrow(() -> new ResourceNotFoundException("Account not found"));

        if (!source.getUser().getId().equals(user.getId())) {
            throw new UnauthorizedException("You do not own this account");
        }
        if (!source.isActive()) {
            throw new BadRequestException("Account is inactive");
        }
        if (source.getBalance().compareTo(request.getAmount()) < 0) {
            throw new InsufficientFundsException("Insufficient wallet balance for this payment");
        }

        // 4. Debit the account
        source.setBalance(source.getBalance().subtract(request.getAmount()));
        accountRepository.save(source);

        // 5. Construct description
        String description = request.getDescription();
        if (description == null || description.isBlank()) {
            description = String.format("%s · %s · %s",
                    category.toUpperCase(),
                    request.getProvider() != null ? request.getProvider() : "",
                    request.getRecipientId() != null ? request.getRecipientId() : "");
        }

        // 6. Record transaction as PENDING
        Transaction tx = Transaction.builder()
                .referenceNumber("PAY-" + UUID.randomUUID().toString().substring(0, 18).toUpperCase())
                .sourceAccount(source)
                .amount(request.getAmount())
                .type(TransactionType.PAYMENT)
                .category(category)
                .status(TransactionStatus.PENDING)
                .description(description)
                .providerReference(request.getProvider())
                .build();
        tx = transactionRepository.save(tx);

        PaymentOrder order = PaymentOrder.builder()
                .user(user)
                .account(source)
                .transaction(tx)
                .category(category)
                .provider(request.getProvider() != null ? request.getProvider() : "VTU")
                .recipientId(request.getRecipientId() != null ? request.getRecipientId() : "")
                .amount(request.getAmount())
                .status(PaymentStatus.PENDING)
                .clientReference(clientRef)
                .build();
        order = paymentOrderRepository.save(order);

        // 7. Call External VTU Provider (VTPass or Mock)
        String generatedToken = null;
        try {
            VtuPurchaseResult result;

            switch (category) {
                case "airtime":
                    result = vtuProvider.buyAirtime(
                            clientRef,
                            request.getProvider(),
                            request.getRecipientId(),
                            request.getAmount()
                    );
                    break;
                case "data":
                    result = vtuProvider.buyData(
                            clientRef,
                            request.getProvider(),
                            request.getRecipientId(),
                            request.getProvider(),
                            request.getAmount()
                    );
                    break;
                case "electricity":
                    String meterType = request.getMeterType() != null ? request.getMeterType() : "prepaid";
                    result = vtuProvider.payElectricity(
                            clientRef,
                            request.getProvider(),
                            request.getRecipientId(),
                            meterType,
                            request.getAmount()
                    );
                    break;
                default:
                    throw new BadRequestException("Unsupported category for VTU payment: " + category);
            }

            // 8. Handle Provider Failure -> Rollback & Refund
            if (!result.isSuccess()) {
                log.warn("VTU Provider returned failure: {}", result.message());
                refundAccount(source, request.getAmount());

                tx.setStatus(TransactionStatus.FAILED);
                order.setStatus(PaymentStatus.FAILED);
                transactionRepository.save(tx);
                paymentOrderRepository.save(order);

                throw new BadRequestException("Payment provider error: " + result.message());
            }

            // 9. Success -> Finalize Order & Transaction
            generatedToken = result.token();
            tx.setStatus(TransactionStatus.COMPLETED);
            tx.setProviderReference(result.providerReference());

            order.setStatus(PaymentStatus.COMPLETED);
            order.setProviderReference(result.providerReference());
            order.setToken(generatedToken);

            transactionRepository.save(tx);
            paymentOrderRepository.save(order);

        } catch (BadRequestException e) {
            throw e;
        } catch (Exception e) {
            log.error("Unhandled error communicating with VTU provider: {}", e.getMessage());
            refundAccount(source, request.getAmount());

            tx.setStatus(TransactionStatus.FAILED);
            order.setStatus(PaymentStatus.FAILED);
            transactionRepository.save(tx);
            paymentOrderRepository.save(order);

            throw new BadRequestException("Failed to complete transaction: " + e.getMessage());
        }

        return mapToResponse(tx, generatedToken);
    }

    /**
     * Paginated transaction history for a specific account
     */
    public Page<TransactionResponse> getHistory(String accountNumber, int page, int size) {
        return transactionRepository.findByAccountNumber(accountNumber, PageRequest.of(page, size))
                .map(this::mapToResponse);
    }

    /**
     * Validates that high-value transactions (>= ₦10,000) have been verified with a consumed OTP
     */
    private void enforceSecurityValidation(User user, BigDecimal amount, Long otpChallengeId, String purpose) {
        if (amount.compareTo(HIGH_VALUE_THRESHOLD) >= 0) {
            if (otpChallengeId == null) {
                throw new BadRequestException("High-value transaction (≥ ₦10,000) requires OTP authorization");
            }

            OtpChallenge challenge = otpChallengeRepository.findByIdAndUserId(otpChallengeId, user.getId())
                    .orElseThrow(() -> new BadRequestException("Invalid or missing security verification challenge"));

            if (!challenge.isConsumed()) {
                throw new BadRequestException("OTP must be verified before submitting this transaction");
            }

            if (!challenge.getPurpose().equalsIgnoreCase(purpose) && !challenge.getPurpose().equalsIgnoreCase("TRANSFER") && !challenge.getPurpose().equalsIgnoreCase("PAYMENT")) {
                throw new BadRequestException("OTP challenge was not issued for this operation");
            }

            if (challenge.getExpiresAt().isBefore(LocalDateTime.now())) {
                throw new BadRequestException("OTP verification session has expired. Please request a new code.");
            }
        }
    }

    private void refundAccount(Account account, BigDecimal amount) {
        account.setBalance(account.getBalance().add(amount));
        accountRepository.save(account);
    }

    private TransactionResponse mapToResponse(Transaction tx, String token) {
        TransactionResponse r = mapToResponse(tx);
        r.setToken(token);
        return r;
    }

    private TransactionResponse mapToResponse(Transaction tx) {
        return TransactionResponse.builder()
                .id(tx.getId())
                .referenceNumber(tx.getReferenceNumber())
                .type(tx.getType())
                .category(tx.getCategory())
                .amount(tx.getAmount())
                .description(tx.getDescription())
                .status(tx.getStatus())
                .sourceAccountNumber(tx.getSourceAccount() != null ? tx.getSourceAccount().getAccountNumber() : null)
                .destinationAccountNumber(tx.getDestinationAccount() != null ? tx.getDestinationAccount().getAccountNumber() : null)
                .providerReference(tx.getProviderReference())
                .createdAt(tx.getCreatedAt())
                .build();
    }
}