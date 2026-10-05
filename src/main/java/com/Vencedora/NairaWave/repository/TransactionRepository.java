package com.Vencedora.NairaWave.repository;

import com.Vencedora.NairaWave.entity.Transaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    Optional<Transaction> findByReferenceNumber(String referenceNumber);

    // Explicit LEFT JOIN preserves rows where destinationAccount is NULL ( like airtime, bills, data, etc.)
    @Query("""
        SELECT t FROM Transaction t 
        LEFT JOIN t.sourceAccount sa 
        LEFT JOIN t.destinationAccount da 
        WHERE sa.accountNumber = :accountNumber 
           OR da.accountNumber = :accountNumber 
        ORDER BY t.createdAt DESC
    """)
    Page<Transaction> findByAccountNumber(@Param("accountNumber") String accountNumber, Pageable pageable);
}