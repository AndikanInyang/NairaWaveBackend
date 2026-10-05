package com.Vencedora.NairaWave.service;

import com.Vencedora.NairaWave.dto.request.LoginRequest;
import com.Vencedora.NairaWave.dto.request.RegisterRequest;
import com.Vencedora.NairaWave.dto.response.AuthResponse;
import com.Vencedora.NairaWave.dto.response.UserDTO;
import com.Vencedora.NairaWave.entity.Account;
import com.Vencedora.NairaWave.entity.User;
import com.Vencedora.NairaWave.entity.enums.AccountType;
import com.Vencedora.NairaWave.exception.BadRequestException;
import com.Vencedora.NairaWave.repository.AccountRepository;
import com.Vencedora.NairaWave.repository.UserRepository;
import com.Vencedora.NairaWave.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.security.SecureRandom;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final AccountRepository accountRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final UserDetailsService userDetailsService;
    private final RefreshTokenService refreshTokenService;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BadRequestException("Email is already registered");
        }
        if (userRepository.existsByPhoneNumber(request.getPhoneNumber())) {
            throw new BadRequestException("Phone number is already registered");
        }

        //  Create User
        User user = User.builder()
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .email(request.getEmail())
                .phoneNumber(request.getPhoneNumber())
                .password(passwordEncoder.encode(request.getPassword()))
                .enabled(true)
                .build();

        user = userRepository.save(user);

        //  Generate 10-digit NUBAN and create Account with starter balance (₦250,000 for test/dev)
        String accountNumber = generateUniqueNuban();
        Account account = Account.builder()
                .user(user)
                .accountNumber(accountNumber)
                .accountType(AccountType.SAVINGS)
                .currency("NGN")
                .balance(new BigDecimal("250000.00"))
                .active(true)
                .build();

        accountRepository.save(account);

        //  Generate JWT
        UserDetails userDetails = userDetailsService.loadUserByUsername(user.getEmail());
        String token = jwtService.generateToken(userDetails);

        return AuthResponse.builder()
                .accessToken(token)
                .refreshToken(token)
                .user(mapToUserDTO(user))
                .build();
    }

    public AuthResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new BadRequestException("User not found"));

        UserDetails userDetails = userDetailsService.loadUserByUsername(user.getEmail());
        String accessToken = jwtService.generateToken(userDetails);
        String refreshToken = refreshTokenService.issue(user, userDetails);

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .user(mapToUserDTO(user))
                .build();
    }

    public UserDTO getCurrentUser(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BadRequestException("User not found"));
        return mapToUserDTO(user);
    }

    private String generateUniqueNuban() {
        SecureRandom random = new SecureRandom();
        String nuban;
        do {
            long number = 1000000000L + (long)(random.nextDouble() * 9000000000L);
            nuban = String.valueOf(number);
        } while (accountRepository.existsByAccountNumber(nuban));
        return nuban;
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