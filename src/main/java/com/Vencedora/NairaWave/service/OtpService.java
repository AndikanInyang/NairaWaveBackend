package com.Vencedora.NairaWave.service;

import com.Vencedora.NairaWave.entity.OtpChallenge;
import com.Vencedora.NairaWave.entity.User;
import com.Vencedora.NairaWave.exception.BadRequestException;
import com.Vencedora.NairaWave.exception.ResourceNotFoundException;
import com.Vencedora.NairaWave.repository.OtpChallengeRepository;
import com.Vencedora.NairaWave.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class OtpService {

    private final OtpChallengeRepository otpChallengeRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder; // Used to hash the OTP code securely
    private final JavaMailSender mailSender;

    private final SecureRandom random = new SecureRandom();

    @Transactional
    public Long generateOtp(String userEmail, String purpose) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        //  Generate 6-digit OTP
        String rawCode = String.format("%06d", random.nextInt(1000000));

        //  Hash and Save
        OtpChallenge challenge = OtpChallenge.builder()
                .user(user)
                .purpose(purpose)
                .codeHash(passwordEncoder.encode(rawCode))
                .expiresAt(LocalDateTime.now().plusMinutes(5)) // 5 Minutes expiry
                .build();

        challenge = otpChallengeRepository.save(challenge);

        //  Dispatch Code via Email
        sendOtpEmail(user.getEmail(), rawCode, purpose);

        return challenge.getId();
    }

    @Transactional
    public void verifyOtp(Long challengeId, String rawCode, String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        OtpChallenge challenge = otpChallengeRepository.findByIdAndUserId(challengeId, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Verification request not found"));

        if (challenge.isConsumed()) {
            throw new BadRequestException("OTP has already been used");
        }
        if (challenge.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new BadRequestException("OTP has expired");
        }
        if (challenge.getAttempts() >= challenge.getMaxAttempts()) {
            throw new BadRequestException("Too many wrong attempts. Request a new OTP.");
        }

        // Increment attempts
        challenge.setAttempts(challenge.getAttempts() + 1);

        if (!passwordEncoder.matches(rawCode, challenge.getCodeHash())) {
            otpChallengeRepository.save(challenge);
            throw new BadRequestException("Invalid verification code");
        }

        challenge.setConsumed(true);
        otpChallengeRepository.save(challenge);
    }

    private void sendOtpEmail(String email, String rawCode, String purpose) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(email);
            message.setSubject("NairaWave Security Code: " + rawCode);
            message.setText("Dear Customer,\n\nUse this verification code to authorized your " + purpose.toLowerCase() + " request:\n\n"
                    + "👉 CODE: " + rawCode + "\n\n"
                    + "This code is confidential. For your own security, do not share this OTP with anyone.\n\n"
                    + "Best regards,\nNairaWave Security Operations.");
            mailSender.send(message);
        } catch (Exception e) {
            // Logs fail silently so local development works without mail servers configured
            System.err.println("SMTP Mail dispatch skipped: " + e.getMessage());
            System.out.println(">>> DEV MODE OTP DISPATCH [EMAIL: " + email + "] -> CODE: " + rawCode);
        }
    }
}