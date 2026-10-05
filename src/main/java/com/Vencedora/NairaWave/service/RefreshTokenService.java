package com.Vencedora.NairaWave.service;

import com.Vencedora.NairaWave.dto.response.AuthResponse;
import com.Vencedora.NairaWave.dto.response.UserDTO;
import com.Vencedora.NairaWave.entity.RefreshToken;
import com.Vencedora.NairaWave.entity.User;
import com.Vencedora.NairaWave.exception.UnauthorizedException;
import com.Vencedora.NairaWave.repository.RefreshTokenRepository;
import com.Vencedora.NairaWave.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;

    @Value("${app.jwt.refresh-expiration:604800000}")
    private long refreshExpirationMs;

    /**
      Creates a refresh JWT, stores only its SHA-256 hash, returns the raw token to the client.
     **/
    @Transactional
    public String issue(User user, UserDetails userDetails) {
        String rawRefreshToken = jwtService.generateRefreshToken(userDetails);

        RefreshToken row = RefreshToken.builder()
                .user(user)
                .tokenHash(sha256(rawRefreshToken))
                .expiresAt(LocalDateTime.now().plusSeconds(refreshExpirationMs / 1000))
                .revoked(false)
                .build();

        refreshTokenRepository.save(row);
        return rawRefreshToken;
    }

    /**
      Validates the incoming refresh token, revokes it, issues a new access + refresh pair (rotation).
      Reuse of an already-revoked token revokes all sessions for that user.
     **/
    @Transactional
    public AuthResponse rotate(String rawRefreshToken) {
        if (rawRefreshToken == null || rawRefreshToken.isBlank()) {
            throw new UnauthorizedException("Refresh token is required");
        }
        if (!jwtService.isRefreshToken(rawRefreshToken)) {
            throw new UnauthorizedException("Invalid refresh token");
        }

        String hash = sha256(rawRefreshToken);
        RefreshToken current = refreshTokenRepository.findByTokenHash(hash)
                .orElseThrow(() -> new UnauthorizedException("Refresh token not recognized"));

        if (current.isRevoked()) {
            revokeAll(current.getUser().getId());
            throw new UnauthorizedException("Refresh token reuse detected");
        }

        if (current.getExpiresAt().isBefore(LocalDateTime.now())) {
            current.setRevoked(true);
            refreshTokenRepository.save(current);
            throw new UnauthorizedException("Refresh token expired");
        }

        User user = current.getUser();
        UserDetails userDetails = userDetailsService.loadUserByUsername(user.getEmail());

        current.setRevoked(true);

        String newAccessToken = jwtService.generateToken(userDetails);
        String newRefreshToken = issue(user, userDetails);
        current.setReplacedByHash(sha256(newRefreshToken));
        refreshTokenRepository.save(current);

        return AuthResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(newRefreshToken)
                .user(mapToUserDTO(user))
                .build();
    }

    @Transactional
    public void revoke(String rawRefreshToken) {
        if (rawRefreshToken == null || rawRefreshToken.isBlank()) {
            return;
        }
        refreshTokenRepository.findByTokenHash(sha256(rawRefreshToken))
                .ifPresent(token -> {
                    token.setRevoked(true);
                    refreshTokenRepository.save(token);
                });
    }

    @Transactional
    public void revokeAll(Long userId) {
        List<RefreshToken> active = refreshTokenRepository.findByUserIdAndRevokedFalse(userId);
        for (RefreshToken token : active) {
            token.setRevoked(true);
        }
        refreshTokenRepository.saveAll(active);
    }

    public static String sha256(String raw) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest(raw.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(hashed.length * 2);
            for (byte b : hashed) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }

    private UserDTO mapToUserDTO(User user) {
        return UserDTO.builder()
                .id(user.getId())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .phoneNumber(user.getPhoneNumber())
                .createdAt(user.getCreatedAt())
                .build();
    }
}