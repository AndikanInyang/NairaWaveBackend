package com.Vencedora.NairaWave.repository;

import com.Vencedora.NairaWave.entity.OtpChallenge;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface OtpChallengeRepository extends JpaRepository<OtpChallenge, Long> {
    Optional<OtpChallenge> findByIdAndUserId(Long id, Long userId);
}