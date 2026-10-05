package com.Vencedora.NairaWave.repository;

import com.Vencedora.NairaWave.entity.PaymentOrder;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PaymentOrderRepository extends JpaRepository<PaymentOrder, Long> {
    Optional<PaymentOrder> findByClientReference(String clientReference);
    boolean existsByClientReference(String clientReference);
}