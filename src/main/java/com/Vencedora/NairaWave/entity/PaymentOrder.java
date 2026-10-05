package com.Vencedora.NairaWave.entity;

import com.Vencedora.NairaWave.entity.enums.PaymentStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "payment_orders")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "transaction_id")
    private Transaction transaction;

    @Column(nullable = false, length = 30)
    private String category;

    @Column(nullable = false, length = 80)
    private String provider;

    @Column(name = "bank_code", length = 10)
    private String bankCode;

    @Column(name = "recipient_id", nullable = false, length = 80)
    private String recipientId;

    @Column(name = "destination_account_number", length = 10)
    private String destinationAccountNumber;

    @Column(name = "destination_account_name", length = 150)
    private String destinationAccountName;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Builder.Default
    @Column(precision = 19, scale = 2)
    private BigDecimal fee = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private PaymentStatus status;

    @Column(name = "client_reference", nullable = false, unique = true, length = 100)
    private String clientReference;

    @Column(name = "provider_reference", length = 150)
    private String providerReference;

    @Column(length = 80)
    private String token;

    @Column(columnDefinition = "TEXT")
    private String metadata;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}